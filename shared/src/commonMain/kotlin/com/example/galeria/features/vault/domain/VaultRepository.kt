package com.example.galeria.features.vault.domain

/**
 * Puerto del vault. La implementación (cifrado, DB, MediaStore/PHPhotoLibrary)
 * vive en data; gallery no importa este tipo — core mediará después.
 */
interface VaultRepository {

    fun isLockConfigured(): Boolean

    fun configuredLockMethod(): LockMethod?

    fun isUnlocked(): Boolean

    fun canHideWithoutUnlock(): Boolean

    fun setupLock(method: LockMethod, secret: CharArray)

    fun verifyAndUnlock(secret: CharArray): Boolean

    fun lock()

    /**
     * Si el vault está vacío y desbloqueado, borra PIN/huella para que
     * la próxima entrada vuelva a configurar el candado.
     */
    fun resetLockIfEmpty(): Boolean

    /**
     * Cifra el medio, lo guarda en el sandbox de la app y lo elimina del
     * almacenamiento público. No basta con ocultarlo.
     */
    fun moveToVaultRemovingFromPublicStorage(source: ExternalMediaSource): VaultItem

    fun moveAllToVaultRemovingFromPublicStorage(sources: List<ExternalMediaSource>): List<VaultItem>

    /**
     * Descifra, reinserta en MediaStore / PHPhotoLibrary y saca el ítem del vault.
     */
    fun exportToPublicStorage(id: VaultItemId)

    fun listItems(): List<VaultItem>

    fun readDecrypted(id: VaultItemId): ByteArray

    fun prepareVideoPlayback(item: VaultItem): String

    fun readOrCreateThumbnail(item: VaultItem): ByteArray?

    fun isBiometricEnabled(): Boolean

    fun copySessionDek(): ByteArray?

    fun storeBiometricWrappedDek(wrapped: ByteArray)

    fun biometricWrappedDek(): ByteArray?

    fun clearBiometricUnlock()

    fun unlockWithDek(dekBytes: ByteArray): Boolean

    fun unlockWithStoredDeviceKey(): Boolean
}
