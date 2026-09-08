package com.example.galeria.features.gallery.domain

class ScanMediaUseCase(
    private val mediaRepository: MediaRepository,
) {

    operator fun invoke(): List<Album> {
        val classified = mediaRepository.scan()
            .map { item ->
                item.copy(albumId = classify(item.relativePath, item.bucketName, item.ownerPackage))
            }
            .sortedByDescending { it.capturedAtEpochMs }
        val autoIds = listOf(
            AlbumId.CAMERA,
            AlbumId.SCREENSHOTS,
            AlbumId.WHATSAPP,
            AlbumId.DOWNLOADS,
        )
        val auto = autoIds.mapNotNull { id ->
            val items = classified.filter { it.albumId == id }
            if (items.isEmpty()) null else Album(id = id, title = id.title(), items = items)
        }
        val userAlbums = classified
            .filter { it.albumId == AlbumId.OTHER }
            .groupBy { otherAlbumTitle(it.bucketName, it.relativePath) }
            .map { (name, items) -> Album(id = AlbumId.OTHER, title = name, items = items) }
            .sortedBy { it.title.lowercase() }
        return auto + userAlbums
    }
}

fun createScanMediaUseCase(): ScanMediaUseCase = ScanMediaUseCase(createMediaRepository())

internal fun classify(
    relativePath: String?,
    bucketName: String?,
    ownerPackage: String? = null,
): AlbumId {
    val haystack = listOfNotNull(relativePath, bucketName, ownerPackage).joinToString(" ").lowercase()
    return when {
        containsAny(haystack, "screenshot", "screen capture", "screencapture", "captura") ->
            AlbumId.SCREENSHOTS
        containsAny(haystack, "whatsapp") -> AlbumId.WHATSAPP
        containsAny(haystack, "download", "descarga") -> AlbumId.DOWNLOADS
        containsAny(haystack, "dcim", "camera", "cámara", "camara") -> AlbumId.CAMERA
        else -> AlbumId.OTHER
    }
}

internal fun otherAlbumTitle(bucketName: String?, relativePath: String?): String {
    val name = bucketName?.takeIf { it.isNotBlank() } ?: "Otros"
    return if (looksLikeGooglePhotos(name) || looksLikeGooglePhotos(relativePath)) "Otros" else name
}

private fun looksLikeGooglePhotos(value: String?): Boolean {
    val haystack = value.orEmpty().lowercase()
    if (haystack.isBlank()) return false
    if ("com.google.android.apps.photos" in haystack) return true
    return containsAny(
        haystack,
        "google photos",
        "googlephotos",
        "gphotos",
        "google/photos",
        "photos/google",
        "google fotos",
        "instant upload",
        "google+",
        "google plus",
    )
}

private fun containsAny(haystack: String, vararg needles: String): Boolean =
    needles.any { it in haystack }
