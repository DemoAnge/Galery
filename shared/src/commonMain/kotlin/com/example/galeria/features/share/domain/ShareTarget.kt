package com.example.galeria.features.share.domain

enum class ShareKind {
    INSTAGRAM_STORY,
    INSTAGRAM,
    FACEBOOK_STORY,
    FACEBOOK,
    WHATSAPP,
    NEARBY,
    BLUETOOTH,
    MORE,
    APP,
}

data class ShareTarget(
    val id: String,
    val label: String,
    val kind: ShareKind,
    val enabled: Boolean = true,
    val reason: String? = null,
    val packageName: String? = null,
    val activityName: String? = null,
) {
    companion object {
        const val ID_INSTAGRAM_STORY = "instagram-story"
        const val ID_INSTAGRAM = "instagram"
        const val ID_FACEBOOK_STORY = "facebook-story"
        const val ID_FACEBOOK = "facebook"
        const val ID_WHATSAPP = "whatsapp"
        const val ID_NEARBY = "nearby"
        const val ID_BLUETOOTH = "bluetooth"
        const val ID_MORE = "more"

        val MORE = ShareTarget(
            id = ID_MORE,
            label = "Más apps",
            kind = ShareKind.MORE,
        )
    }
}

internal const val PACKAGE_INSTAGRAM = "com.instagram.android"
internal const val PACKAGE_FACEBOOK = "com.facebook.katana"
internal const val PACKAGE_FACEBOOK_LITE = "com.facebook.lite"
internal const val PACKAGE_WHATSAPP = "com.whatsapp"
internal const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
