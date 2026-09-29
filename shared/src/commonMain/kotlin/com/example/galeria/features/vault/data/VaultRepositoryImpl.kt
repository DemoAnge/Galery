package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.DerivedKey
import com.example.galeria.core.encryption.Encryptor
import com.example.galeria.core.encryption.KeyDeriver
import com.example.galeria.core.securestorage.SecureKeyStore
import com.example.galeria.features.vault.domain.ExternalMediaSource
import com.example.galeria.features.vault.domain.LockMethod
import com.example.galeria.features.vault.domain.shouldResetLockWhenLeaving
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultItemId
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.features.vault.domain.VaultRepository

class VaultRepositoryImpl(
    private val keyDeriver: KeyDeriver,
    private val encryptor: Encryptor,
    private val secureKeyStore: SecureKeyStore,
    private val fileStore: VaultFileStore,
    private val metadataStore: VaultMetadataStore,
    private val publicMediaRemover: PublicMediaRemover,
    private val publicMediaInserter: PublicMediaInserter,
) : VaultRepository {

    private var sessionKey: DerivedKey? = null
    private val thumbCreateLock = Any()

    override fun isLockConfigured(): Boolean = secureKeyStore.contains(ID_WRAPPED_DEK)

    override fun configuredLockMethod(): LockMethod? {
        val bytes = secureKeyStore.read(ID_LOCK_METHOD) ?: return null
        return runCatching { LockMethod.valueOf(bytes.decodeToString()) }.getOrNull()
    }

    override fun isUnlocked(): Boolean = sessionKey != null

    override fun canHideWithoutUnlock(): Boolean =
        isLockConfigured() && (isUnlocked() || secureKeyStore.contains(ID_DEVICE_DEK))

    override fun setupLock(method: LockMethod, secret: CharArray) {
        check(!isLockConfigured()) { "Vault lock is already configured" }
        val salt = keyDeriver.generateSalt()
        val kek = keyDeriver.derive(secret, salt)
        val dek = randomDek()
        try {
            val dekCopy = dek.asBytes()
            val wrapped = try {
                encryptor.encrypt(dekCopy, kek)
            } finally {
                dekCopy.fill(0)
            }
            secureKeyStore.write(ID_SALT, salt)
            secureKeyStore.write(ID_WRAPPED_DEK, wrapped.toFileBytes())
            secureKeyStore.write(ID_LOCK_METHOD, method.name.encodeToByteArray())
            sessionKey?.clear()
            sessionKey = dek
            persistDeviceDek(dek)
        } finally {
            kek.clear()
        }
    }

    override fun verifyAndUnlock(secret: CharArray): Boolean {
        val salt = secureKeyStore.read(ID_SALT) ?: return false
        val wrappedBytes = secureKeyStore.read(ID_WRAPPED_DEK) ?: return false
        val kek = keyDeriver.derive(secret, salt)
        return try {
            val wrapped = wrappedBytes.toAesGcmPayload()
            val dekBytes = encryptor.decrypt(wrapped, kek)
            sessionKey?.clear()
            sessionKey = DerivedKey.of(dekBytes)
            persistDeviceDek(sessionKey)
            dekBytes.fill(0)
            true
        } catch (_: Exception) {
            false
        } finally {
            kek.clear()
        }
    }

    override fun lock() {
        sessionKey?.clear()
        sessionKey = null
    }

    override fun resetLockIfEmpty(): Boolean {
        if (!isUnlocked()) return false
        return try {
            val items = metadataStore.load(requireSession())
            if (!shouldResetLockWhenLeaving(items.size)) return false
            wipeLock()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun moveToVaultRemovingFromPublicStorage(source: ExternalMediaSource): VaultItem =
        moveAllToVaultRemovingFromPublicStorage(listOf(source)).first()

    override fun moveAllToVaultRemovingFromPublicStorage(
        sources: List<ExternalMediaSource>,
    ): List<VaultItem> {
        if (sources.isEmpty()) return emptyList()
        val borrowed = sessionKey == null
        val key = cryptoKey()
        val previous = metadataStore.load(key)
        val created = ArrayList<VaultItem>(sources.size)
        val plaintexts = ArrayList<ByteArray>(sources.size)
        try {
            sources.forEach { source ->
                val itemId = newItemId()
                val isVideo = source.mediaKind == VaultMediaKind.VIDEO
                if (shouldStreamVaultMedia(source.mediaKind, source.sizeBytes)) {
                    val jpeg = runCatching {
                        encodeVaultThumbnailFromPublicUri(source.publicUri, isVideo, source.displayName)
                    }.getOrNull()
                    encryptPublicMediaToVault(itemId.value, source.publicUri, key)
                    if (jpeg != null) {
                        try {
                            writeThumbnailBytes(vaultThumbnailFileId(itemId.value), jpeg, key)
                        } finally {
                            jpeg.fill(0)
                        }
                    }
                } else {
                    val plaintext = publicMediaRemover.readBytes(source.publicUri)
                    plaintexts += plaintext
                    val payload = encryptor.encrypt(plaintext, key)
                    fileStore.write(itemId.value, payload)
                    writeThumbnail(itemId.value, plaintext, isVideo, source.displayName, key)
                }
                created += VaultItem(
                    id = itemId,
                    displayName = source.displayName,
                    mediaKind = source.mediaKind,
                    capturedAtEpochMs = source.capturedAtEpochMs,
                    sizeBytes = source.sizeBytes,
                    addedAtEpochMs = currentEpochMs(),
                    albumLabel = source.albumLabel,
                )
            }
            try {
                metadataStore.save(key, previous + created)
            } catch (e: Exception) {
                created.forEach { deleteItemFiles(it.id.value) }
                throw e
            }
            try {
                publicMediaRemover.deleteAll(sources.map { it.publicUri })
            } catch (e: Exception) {
                created.forEach { deleteItemFiles(it.id.value) }
                runCatching { metadataStore.save(key, previous) }
                throw e
            }
            return created
        } finally {
            plaintexts.forEach { it.fill(0) }
            if (borrowed) key.clear()
        }
    }

    override fun exportToPublicStorage(id: VaultItemId) {
        val key = requireSession()
        val items = metadataStore.load(key)
        val item = items.firstOrNull { it.id == id } ?: error("Vault item not found")
        if (shouldStreamVaultMedia(item.mediaKind, item.sizeBytes)) {
            exportDecryptedVaultItem(item, key)
        } else {
            val payload = fileStore.read(id.value)
            val plaintext = encryptor.decrypt(payload, key)
            try {
                publicMediaInserter.insert(item, plaintext)
            } finally {
                plaintext.fill(0)
            }
        }
        deleteItemFiles(id.value)
        metadataStore.save(key, items.filter { it.id != id })
    }

    override fun listItems(): List<VaultItem> {
        val key = requireSession()
        return metadataStore.load(key)
    }

    override fun readDecrypted(id: VaultItemId): ByteArray {
        val key = requireSession()
        return decryptVaultToPlaintext(id.value, key)
    }

    override fun prepareVideoPlayback(item: VaultItem): String {
        val key = requireSession()
        return decryptVaultToPlayablePath(item.id.value, item.displayName, key)
    }

    override fun readOrCreateThumbnail(item: VaultItem): ByteArray? {
        val key = requireSession()
        val thumbId = vaultThumbnailFileId(item.id.value)
        if (fileStore.exists(thumbId)) {
            return runCatching { encryptor.decrypt(fileStore.read(thumbId), key) }.getOrNull()
        }
        synchronized(thumbCreateLock) {
            if (fileStore.exists(thumbId)) {
                return runCatching { encryptor.decrypt(fileStore.read(thumbId), key) }.getOrNull()
            }
            if (!canBackfillVaultThumbnail(item.sizeBytes) || !fileStore.exists(item.id.value)) {
                return null
            }
            val plaintext = runCatching { encryptor.decrypt(fileStore.read(item.id.value), key) }.getOrNull()
                ?: return null
            return try {
                val jpeg = encodeVaultThumbnailJpeg(
                    plaintext,
                    item.mediaKind == VaultMediaKind.VIDEO,
                    item.displayName,
                ) ?: return null
                writeThumbnailBytes(thumbId, jpeg, key)
                jpeg
            } finally {
                plaintext.fill(0)
            }
        }
    }

    override fun isBiometricEnabled(): Boolean = secureKeyStore.contains(ID_BIO_WRAPPED)

    override fun copySessionDek(): ByteArray? = sessionKey?.asBytes()

    override fun storeBiometricWrappedDek(wrapped: ByteArray) {
        check(isUnlocked()) { "Vault is locked" }
        secureKeyStore.write(ID_BIO_WRAPPED, wrapped)
    }

    override fun biometricWrappedDek(): ByteArray? = secureKeyStore.read(ID_BIO_WRAPPED)

    override fun clearBiometricUnlock() {
        secureKeyStore.delete(ID_BIO_WRAPPED)
    }

    override fun unlockWithDek(dekBytes: ByteArray): Boolean {
        return try {
            val key = DerivedKey.of(dekBytes)
            sessionKey?.clear()
            sessionKey = key
            persistDeviceDek(key)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun unlockWithStoredDeviceKey(): Boolean {
        return try {
            val key = loadDeviceDek() ?: return false
            sessionKey?.clear()
            sessionKey = key
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun writeThumbnail(
        itemId: String,
        plaintext: ByteArray,
        isVideo: Boolean,
        displayName: String,
        key: DerivedKey,
    ) {
        val jpeg = runCatching { encodeVaultThumbnailJpeg(plaintext, isVideo, displayName) }.getOrNull()
            ?: return
        try {
            writeThumbnailBytes(vaultThumbnailFileId(itemId), jpeg, key)
        } finally {
            jpeg.fill(0)
        }
    }

    private fun writeThumbnailBytes(thumbId: String, jpeg: ByteArray, key: DerivedKey) {
        runCatching { fileStore.write(thumbId, encryptor.encrypt(jpeg, key)) }
    }

    private fun deleteItemFiles(itemId: String) {
        fileStore.delete(vaultThumbnailFileId(itemId))
        fileStore.delete(itemId)
    }

    private fun wipeLock() {
        sessionKey?.clear()
        sessionKey = null
        runCatching { secureKeyStore.delete(ID_WRAPPED_DEK) }
        runCatching { secureKeyStore.delete(ID_SALT) }
        runCatching { secureKeyStore.delete(ID_LOCK_METHOD) }
        runCatching { secureKeyStore.delete(ID_BIO_WRAPPED) }
        runCatching { secureKeyStore.delete(ID_DEVICE_DEK) }
        runCatching { fileStore.delete(VAULT_METADATA_FILE_ID) }
    }

    private fun requireSession(): DerivedKey =
        sessionKey ?: error("Vault is locked")

    private fun cryptoKey(): DerivedKey = sessionKey ?: loadDeviceDek() ?: error("Vault is locked")

    private fun persistDeviceDek(dek: DerivedKey?) {
        if (dek == null || secureKeyStore.contains(ID_DEVICE_DEK)) return
        val bytes = dek.asBytes()
        try {
            secureKeyStore.write(ID_DEVICE_DEK, bytes)
        } finally {
            bytes.fill(0)
        }
    }

    private fun loadDeviceDek(): DerivedKey? {
        val bytes = secureKeyStore.read(ID_DEVICE_DEK) ?: return null
        return try {
            DerivedKey.of(bytes)
        } finally {
            bytes.fill(0)
        }
    }

    private fun randomDek(): DerivedKey {
        val raw = ByteArray(DerivedKey.SIZE_BYTES)
        val first = keyDeriver.generateSalt()
        val second = keyDeriver.generateSalt()
        first.copyInto(raw, 0)
        second.copyInto(raw, first.size)
        first.fill(0)
        second.fill(0)
        val key = DerivedKey.of(raw)
        raw.fill(0)
        return key
    }

    private fun newItemId(): VaultItemId {
        val entropy = keyDeriver.generateSalt()
        val hex = entropy.toHex()
        entropy.fill(0)
        return VaultItemId(hex)
    }
}

expect fun createVaultRepository(): VaultRepository

internal expect fun currentEpochMs(): Long

private const val ID_SALT = "vault.salt"
private const val ID_WRAPPED_DEK = "vault.wrapped_dek"
private const val ID_LOCK_METHOD = "vault.lock_method"
private const val ID_BIO_WRAPPED = "vault.bio_wrapped_dek"
private const val ID_DEVICE_DEK = "vault.device_dek"

private fun ByteArray.toHex(): String = joinToString("") { byte ->
    val value = byte.toInt() and 0xFF
    value.toString(16).padStart(2, '0')
}
