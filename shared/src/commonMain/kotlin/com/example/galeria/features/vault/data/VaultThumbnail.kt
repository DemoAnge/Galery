package com.example.galeria.features.vault.data

internal const val VAULT_THUMB_MAX_EDGE = 512
internal const val VAULT_THUMB_BACKFILL_MAX_BYTES = 16L * 1024 * 1024

internal fun vaultThumbnailFileId(itemId: String): String = "$itemId.thumb"

internal fun looksLikeStillImage(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return true
    if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte()) return true
    if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()) return true
    if (bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) return true
    val riff = bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
        bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte()
    val webp = bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
        bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
    if (riff && webp) return true
    val ftyp = bytes.size >= 12 &&
        bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() &&
        bytes[6] == 0x79.toByte() && bytes[7] == 0x70.toByte()
    if (ftyp) {
        val brand = bytes.copyOfRange(8, 12).decodeToString()
        if (brand.startsWith("heic") || brand.startsWith("heif") || brand.startsWith("mif1") ||
            brand.startsWith("avif")
        ) {
            return true
        }
    }
    return false
}

internal fun canBackfillVaultThumbnail(sizeBytes: Long): Boolean =
    sizeBytes in 1..VAULT_THUMB_BACKFILL_MAX_BYTES

internal expect fun encodeVaultThumbnailJpeg(
    plaintext: ByteArray,
    isVideo: Boolean,
    displayName: String,
): ByteArray?

internal expect fun encodeVaultThumbnailFromPublicUri(
    publicUri: String,
    isVideo: Boolean,
    displayName: String,
): ByteArray?
