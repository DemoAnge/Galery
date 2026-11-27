package com.example.galeria.features.share.domain

internal fun allowsStoryShare(mimeTypes: List<String>): Boolean {
    val mime = mimeTypes.singleOrNull() ?: return false
    return mime.startsWith("image/") || mime.startsWith("video/")
}

internal fun shareHeadline(count: Int, imageCount: Int, videoCount: Int): String {
    return when {
        count <= 0 -> "Compartir"
        count == 1 && imageCount == 1 -> "Compartir foto"
        count == 1 && videoCount == 1 -> "Compartir video"
        count == 1 -> "Compartir"
        imageCount == count -> "Compartir $count fotos"
        videoCount == count -> "Compartir $count videos"
        else -> "Compartir $count elementos"
    }
}

internal fun featuredShareTargets(
    mimeTypes: List<String>,
    installed: Set<String>,
    nearbyLabel: String? = null,
    nearbyPackage: String? = null,
    nearbyActivity: String? = null,
    bluetoothLabel: String? = null,
    bluetoothPackage: String? = null,
    bluetoothActivity: String? = null,
): List<ShareTarget> {
    val storyOk = allowsStoryShare(mimeTypes)
    val single = mimeTypes.size == 1
    val storyReason = if (storyOk) null else "Elige una sola foto o video"
    val instagramReason = if (single) null else "Instagram admite una sola foto o video"
    val facebookPkg = when {
        PACKAGE_FACEBOOK in installed -> PACKAGE_FACEBOOK
        PACKAGE_FACEBOOK_LITE in installed -> PACKAGE_FACEBOOK_LITE
        else -> null
    }
    val whatsappPkg = when {
        PACKAGE_WHATSAPP in installed -> PACKAGE_WHATSAPP
        PACKAGE_WHATSAPP_BUSINESS in installed -> PACKAGE_WHATSAPP_BUSINESS
        else -> null
    }
    return buildList {
        if (PACKAGE_INSTAGRAM in installed) {
            add(
                ShareTarget(
                    id = ShareTarget.ID_INSTAGRAM_STORY,
                    label = "Historias IG",
                    kind = ShareKind.INSTAGRAM_STORY,
                    enabled = storyOk,
                    reason = storyReason,
                    packageName = PACKAGE_INSTAGRAM,
                ),
            )
            add(
                ShareTarget(
                    id = ShareTarget.ID_INSTAGRAM,
                    label = "Instagram",
                    kind = ShareKind.INSTAGRAM,
                    enabled = single,
                    reason = instagramReason,
                    packageName = PACKAGE_INSTAGRAM,
                ),
            )
        }
        if (PACKAGE_FACEBOOK in installed) {
            add(
                ShareTarget(
                    id = ShareTarget.ID_FACEBOOK_STORY,
                    label = "Historias FB",
                    kind = ShareKind.FACEBOOK_STORY,
                    enabled = storyOk,
                    reason = storyReason,
                    packageName = PACKAGE_FACEBOOK,
                ),
            )
        }
        if (facebookPkg != null) {
            add(
                ShareTarget(
                    id = ShareTarget.ID_FACEBOOK,
                    label = "Facebook",
                    kind = ShareKind.FACEBOOK,
                    enabled = true,
                    packageName = facebookPkg,
                ),
            )
        }
        if (whatsappPkg != null) {
            add(
                ShareTarget(
                    id = ShareTarget.ID_WHATSAPP,
                    label = "WhatsApp",
                    kind = ShareKind.WHATSAPP,
                    enabled = true,
                    packageName = whatsappPkg,
                ),
            )
        }
        if (nearbyLabel != null) {
            add(
                ShareTarget(
                    id = ShareTarget.ID_NEARBY,
                    label = nearbyLabel,
                    kind = ShareKind.NEARBY,
                    enabled = true,
                    packageName = nearbyPackage,
                    activityName = nearbyActivity,
                ),
            )
        }
        if (bluetoothLabel != null) {
            add(
                ShareTarget(
                    id = ShareTarget.ID_BLUETOOTH,
                    label = bluetoothLabel,
                    kind = ShareKind.BLUETOOTH,
                    enabled = true,
                    packageName = bluetoothPackage,
                    activityName = bluetoothActivity,
                ),
            )
        }
        add(ShareTarget.MORE)
    }
}

internal fun mergeShareTargets(
    featured: List<ShareTarget>,
    apps: List<ShareTarget>,
): List<ShareTarget> {
    val featuredPkgs = featured.mapNotNull { it.packageName }.toSet()
    val featuredIds = featured.map { it.id }.toSet()
    return featured + apps.filter { app ->
        app.id !in featuredIds && app.packageName !in featuredPkgs
    }
}

internal fun isNearbyShareComponent(packageName: String, activityName: String): Boolean {
    val pkg = packageName.lowercase()
    val name = activityName.lowercase()
    return pkg == "com.samsung.android.app.sharelive" ||
        name.contains("nearby.sharing") ||
        (pkg == "com.google.android.gms" && name.contains("nearby"))
}

internal fun isBluetoothShareComponent(packageName: String, activityName: String): Boolean {
    val pkg = packageName.lowercase()
    val name = activityName.lowercase()
    return pkg.contains("bluetooth") ||
        name.contains("bluetoothopp") ||
        name.contains("bluetooth.opp")
}

internal fun featuredSharePackages(): Set<String> = setOf(
    PACKAGE_INSTAGRAM,
    PACKAGE_FACEBOOK,
    PACKAGE_FACEBOOK_LITE,
    PACKAGE_WHATSAPP,
    PACKAGE_WHATSAPP_BUSINESS,
)
