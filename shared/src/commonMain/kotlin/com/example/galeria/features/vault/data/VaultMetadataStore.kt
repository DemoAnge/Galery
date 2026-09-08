package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.DerivedKey
import com.example.galeria.core.encryption.Encryptor
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultItemId
import com.example.galeria.features.vault.domain.VaultMediaKind

/**
 * Catálogo de metadatos cifrado con la DEK. Nunca se persiste en texto plano.
 */
class VaultMetadataStore(
    private val fileStore: VaultFileStore,
    private val encryptor: Encryptor,
) {

    fun load(key: DerivedKey): List<VaultItem> {
        if (!fileStore.exists(VAULT_METADATA_FILE_ID)) return emptyList()
        val payload = fileStore.read(VAULT_METADATA_FILE_ID)
        val plaintext = encryptor.decrypt(payload, key)
        return try {
            decode(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }

    fun save(key: DerivedKey, items: List<VaultItem>) {
        val plaintext = encode(items)
        try {
            val payload = encryptor.encrypt(plaintext, key)
            fileStore.write(VAULT_METADATA_FILE_ID, payload)
        } finally {
            plaintext.fill(0)
        }
    }

    private fun encode(items: List<VaultItem>): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(MAGIC)
        writer.writeInt(items.size)
        items.forEach { item ->
            writer.writeUtf8(item.id.value)
            writer.writeUtf8(item.displayName)
            writer.writeByte(
                when (item.mediaKind) {
                    VaultMediaKind.IMAGE -> KIND_IMAGE
                    VaultMediaKind.VIDEO -> KIND_VIDEO
                },
            )
            writer.writeLong(item.capturedAtEpochMs)
            writer.writeLong(item.sizeBytes)
            writer.writeLong(item.addedAtEpochMs)
            writer.writeUtf8Nullable(item.albumLabel)
        }
        return writer.toByteArray()
    }

    private fun decode(bytes: ByteArray): List<VaultItem> {
        val reader = ByteReader(bytes)
        val magic = reader.readBytes(MAGIC.size)
        check(magic.contentEquals(MAGIC)) { "Vault metadata is corrupted" }
        val count = reader.readInt()
        check(count >= 0) { "Vault metadata is corrupted" }
        return List(count) {
            VaultItem(
                id = VaultItemId(reader.readUtf8()),
                displayName = reader.readUtf8(),
                mediaKind = when (val kind = reader.readByte()) {
                    KIND_IMAGE -> VaultMediaKind.IMAGE
                    KIND_VIDEO -> VaultMediaKind.VIDEO
                    else -> error("Vault metadata is corrupted")
                },
                capturedAtEpochMs = reader.readLong(),
                sizeBytes = reader.readLong(),
                addedAtEpochMs = reader.readLong(),
                albumLabel = reader.readUtf8Nullable(),
            )
        }
    }
}

private val MAGIC = byteArrayOf(0x47, 0x56, 0x4D, 0x31)
private const val KIND_IMAGE: Int = 1
private const val KIND_VIDEO: Int = 2
private const val NULL_STRING: Int = 0xFFFF

private class ByteWriter {
    private val out = ArrayList<Byte>(256)

    fun writeByte(value: Int) {
        out.add(value.toByte())
    }

    fun writeBytes(bytes: ByteArray) {
        bytes.forEach { out.add(it) }
    }

    fun writeInt(value: Int) {
        writeByte(value ushr 24)
        writeByte(value ushr 16)
        writeByte(value ushr 8)
        writeByte(value)
    }

    fun writeLong(value: Long) {
        writeInt((value ushr 32).toInt())
        writeInt(value.toInt())
    }

    fun writeUtf8(value: String) {
        val bytes = value.encodeToByteArray()
        require(bytes.size < NULL_STRING) { "String too long" }
        writeByte(bytes.size ushr 8)
        writeByte(bytes.size)
        writeBytes(bytes)
    }

    fun writeUtf8Nullable(value: String?) {
        if (value == null) {
            writeByte(NULL_STRING ushr 8)
            writeByte(NULL_STRING)
        } else {
            writeUtf8(value)
        }
    }

    fun toByteArray(): ByteArray = ByteArray(out.size) { out[it] }
}

private class ByteReader(private val data: ByteArray) {
    private var offset = 0

    fun readByte(): Int {
        check(offset < data.size) { "Vault metadata is corrupted" }
        return data[offset++].toInt() and 0xFF
    }

    fun readBytes(count: Int): ByteArray {
        check(offset + count <= data.size) { "Vault metadata is corrupted" }
        val slice = data.copyOfRange(offset, offset + count)
        offset += count
        return slice
    }

    fun readInt(): Int {
        return (readByte() shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
    }

    fun readLong(): Long {
        return ((readInt().toLong() and 0xFFFFFFFFL) shl 32) or (readInt().toLong() and 0xFFFFFFFFL)
    }

    fun readUtf8(): String {
        val length = (readByte() shl 8) or readByte()
        check(length != NULL_STRING && length >= 0) { "Vault metadata is corrupted" }
        return readBytes(length).decodeToString()
    }

    fun readUtf8Nullable(): String? {
        val length = (readByte() shl 8) or readByte()
        if (length == NULL_STRING) return null
        check(length >= 0) { "Vault metadata is corrupted" }
        return readBytes(length).decodeToString()
    }
}
