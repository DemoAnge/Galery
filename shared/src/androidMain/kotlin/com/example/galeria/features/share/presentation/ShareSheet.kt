package com.example.galeria.features.share.presentation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.features.share.domain.ShareKind
import com.example.galeria.features.share.domain.ShareTarget
import com.example.galeria.features.share.domain.featuredSharePackages
import com.example.galeria.features.share.domain.featuredShareTargets
import com.example.galeria.features.share.domain.isBluetoothShareComponent
import com.example.galeria.features.share.domain.isNearbyShareComponent
import com.example.galeria.features.share.domain.mergeShareTargets
import com.example.galeria.features.share.domain.sharedMimeType

private const val ACTION_INSTAGRAM_STORY = "com.instagram.share.ADD_TO_STORY"
private const val ACTION_FACEBOOK_STORY = "com.facebook.stories.ADD_TO_STORY"

actual class ShareSheet actual constructor() {

    actual fun targets(requests: List<ShareRequest>): List<ShareTarget> {
        check(requests.isNotEmpty()) { "Missing public media" }
        val context = runCatching { AndroidAppContext.require() }.getOrNull()
            ?: return featuredShareTargets(requests.map { it.mimeType }, emptySet())
        val pm = context.packageManager
        val probe = shareSendIntent(requests)
        val resolved = queryActivities(pm, probe)
        val installed = resolved.map { it.activityInfo.packageName }.toSet() +
            featuredSharePackages().filter { isPackageInstalled(pm, it) }
        val nearby = resolveSpecial(
            pm = pm,
            probe = probe,
            resolved = resolved,
            predicate = ::isNearbyShareComponent,
            known = nearbyComponents(),
            fallbackLabel = "Quick Share",
        )
        val bluetooth = resolveSpecial(
            pm = pm,
            probe = probe,
            resolved = resolved,
            predicate = ::isBluetoothShareComponent,
            known = bluetoothComponents(),
            fallbackLabel = "Bluetooth",
        )
        val featured = featuredShareTargets(
            mimeTypes = requests.map { it.mimeType },
            installed = installed,
            nearbyLabel = nearby?.label,
            nearbyPackage = nearby?.packageName,
            nearbyActivity = nearby?.activityName,
            bluetoothLabel = bluetooth?.label,
            bluetoothPackage = bluetooth?.packageName,
            bluetoothActivity = bluetooth?.activityName,
        )
        val skip = featuredSharePackages() +
            setOfNotNull(nearby?.packageName, bluetooth?.packageName, context.packageName)
        val apps = resolved
            .distinctBy { it.activityInfo.packageName }
            .filter { info ->
                val pkg = info.activityInfo.packageName
                val name = info.activityInfo.name.orEmpty()
                pkg !in skip &&
                    !isNearbyShareComponent(pkg, name) &&
                    !isBluetoothShareComponent(pkg, name)
            }
            .map { info ->
                val pkg = info.activityInfo.packageName
                ShareTarget(
                    id = "app:$pkg",
                    label = info.loadLabel(pm).toString().ifBlank { pkg },
                    kind = ShareKind.APP,
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                )
            }
            .sortedBy { it.label.lowercase() }
        return mergeShareTargets(featured, apps)
    }

    actual fun share(requests: List<ShareRequest>) {
        shareTo(requests, ShareTarget.MORE)
    }

    actual fun shareTo(requests: List<ShareRequest>, target: ShareTarget) {
        val activity = AndroidAppContext.activity
            ?: error("Share requires an Activity")
        check(requests.isNotEmpty()) { "Missing public media" }
        val intent = when (target.kind) {
            ShareKind.INSTAGRAM_STORY -> storyIntent(
                request = requests.first(),
                action = ACTION_INSTAGRAM_STORY,
                packageName = target.packageName,
            )
            ShareKind.FACEBOOK_STORY -> storyIntent(
                request = requests.first(),
                action = ACTION_FACEBOOK_STORY,
                packageName = target.packageName,
            )
            ShareKind.MORE -> Intent.createChooser(shareSendIntent(requests), "Compartir")
            else -> directedSend(requests, target.packageName, target.activityName)
        }
        launch(activity, intent, requests, target.packageName)
    }
}

private data class ResolvedShare(
    val label: String,
    val packageName: String,
    val activityName: String,
)

private fun shareSendIntent(requests: List<ShareRequest>): Intent {
    val uris = requests.map { it.publicUri.toUri() }
    return if (uris.size == 1) {
        val uri = uris.first()
        Intent(Intent.ACTION_SEND).apply {
            type = requests.first().mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = sharedMimeType(requests.map { it.mimeType })
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            clipData = ClipData.newRawUri("", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

private fun directedSend(
    requests: List<ShareRequest>,
    packageName: String?,
    activityName: String?,
): Intent {
    return shareSendIntent(requests).apply {
        if (!packageName.isNullOrBlank() && !activityName.isNullOrBlank()) {
            component = ComponentName(packageName, activityName)
        } else if (!packageName.isNullOrBlank()) {
            setPackage(packageName)
        }
    }
}

private fun storyIntent(request: ShareRequest, action: String, packageName: String?): Intent {
    val uri = request.publicUri.toUri()
    return Intent(action).apply {
        setDataAndType(uri, request.mimeType)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (!packageName.isNullOrBlank()) setPackage(packageName)
    }
}

private fun launch(
    activity: android.app.Activity,
    intent: Intent,
    requests: List<ShareRequest>,
    packageName: String?,
) {
    val uris = requests.map { it.publicUri.toUri() }
    grantUris(activity, uris, packageName)
    try {
        activity.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        activity.startActivity(Intent.createChooser(shareSendIntent(requests), "Compartir"))
    }
}

private fun grantUris(activity: android.app.Activity, uris: List<Uri>, packageName: String?) {
    val pkg = packageName ?: return
    uris.forEach { uri ->
        runCatching {
            activity.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

private fun queryActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
}

private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
    return runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        true
    }.getOrDefault(false)
}

private fun resolveSpecial(
    pm: PackageManager,
    probe: Intent,
    resolved: List<ResolveInfo>,
    predicate: (String, String) -> Boolean,
    known: List<ComponentName>,
    fallbackLabel: String,
): ResolvedShare? {
    val fromQuery = resolved.firstOrNull { info ->
        predicate(info.activityInfo.packageName, info.activityInfo.name.orEmpty())
    }
    if (fromQuery != null) {
        val label = fromQuery.loadLabel(pm).toString().ifBlank { fallbackLabel }
        return ResolvedShare(label, fromQuery.activityInfo.packageName, fromQuery.activityInfo.name)
    }
    for (component in known) {
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getActivityInfo(component, 0)
            }
        }.getOrNull() ?: continue
        val directed = Intent(probe).setComponent(component)
        val label = runCatching { info.loadLabel(pm).toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: fallbackLabel
        if (directed.resolveActivity(pm) != null || info.exported) {
            return ResolvedShare(label, info.packageName, info.name)
        }
    }
    return null
}

private fun nearbyComponents(): List<ComponentName> = listOf(
    ComponentName("com.google.android.gms", "com.google.android.gms.nearby.sharing.ShareSheetActivity"),
    ComponentName("com.google.android.gms", "com.google.android.gms.nearby.sharing.main.MainActivity"),
    ComponentName("com.samsung.android.app.sharelive", "com.samsung.android.app.sharelive.ShareLiveActivity"),
    ComponentName(
        "com.samsung.android.app.sharelive",
        "com.samsung.android.app.sharelive.presentation.main.MainActivity",
    ),
)

private fun bluetoothComponents(): List<ComponentName> = listOf(
    ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"),
)
