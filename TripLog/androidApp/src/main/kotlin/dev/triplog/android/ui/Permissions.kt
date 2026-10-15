package dev.triplog.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** [App 4 · B5] What went wrong with the last permission request. Drawn as a banner on the Record screen. */
enum class PermissionIssue { LocationDenied, PreciseDenied, NotificationsDenied }

/**
 * [App 4 · B5] Asks for everything a recording needs, in one system dialog sequence, when the user taps Start.
 *
 * Why it lives in the app and not in shared: asking is UI work (an Activity result). The shared
 * AndroidLocationStream only checks the result and reports PermissionDenied if it is missing.
 * iOS asks in LocationPermission.swift.
 */
class TrackingPermissions internal constructor(private val context: Context) {

    /** The banner to show, or null. Compose state: the Record screen redraws after the dialog closes. */
    var issue by mutableStateOf<PermissionIssue?>(null)
        private set

    internal var launch: (Array<String>) -> Unit = {}
    private var pending: (() -> Unit)? = null

    /** Calls [onReady] at once when precise location is already granted, else asks first. */
    fun requestThen(onReady: () -> Unit) {
        val missing = buildList {
            if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            // PLATFORM: notifications are a runtime permission only from Android 13 (API 33).
            if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isEmpty()) {
            issue = null
            onReady()
        } else {
            pending = onReady
            launch(missing.toTypedArray())
        }
    }

    /** Called with the dialog result. Android may skip the dialog entirely after "Don't allow" twice. */
    internal fun onResult() {
        val fine = has(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = has(Manifest.permission.ACCESS_COARSE_LOCATION)
        val notifications = Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS)
        // TRAP: since Android 12 the user can grant only "approximate". Fixes are then ~2 km wide and the 30 m filter
        // drops every one of them, so a trip would record nothing. Treat it as a separate, explained case.
        issue = when {
            fine && !notifications -> PermissionIssue.NotificationsDenied
            fine -> null
            coarse -> PermissionIssue.PreciseDenied
            else -> PermissionIssue.LocationDenied
        }
        // Recording without notifications still works (the service runs, its notification is just hidden).
        if (fine) pending?.invoke()
        pending = null
    }

    private fun has(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Opens this app's page in system Settings: the only way back after "Don't allow" twice. */
    fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Remembers a [TrackingPermissions] tied to this composable's Activity result launcher. */
@Composable
fun rememberTrackingPermissions(): TrackingPermissions {
    val context = LocalContext.current
    val permissions = remember { TrackingPermissions(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissions.onResult()
    }
    permissions.launch = { launcher.launch(it) }
    return permissions
}
