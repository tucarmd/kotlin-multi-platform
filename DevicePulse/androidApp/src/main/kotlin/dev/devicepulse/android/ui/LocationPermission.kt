package dev.devicepulse.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Asking for permission is UI work, so it lives in the app, not the shared core. iOS does the same in Swift. */
class LocationPermission internal constructor(
    private val isGranted: () -> Boolean,
    private val launch: () -> Unit,
) {
    /** Observable: reading it from a composable recomposes when the user answers the prompt. */
    val granted: Boolean get() = isGranted()

    // Android decides itself whether to show the dialog (it stays silent after "Don't ask again").
    fun request() = launch()
}

@Composable
fun rememberLocationPermission(): LocationPermission {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(LOCATION_PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        isGranted = result.values.any { it }
    }
    return remember(launcher) { LocationPermission({ isGranted }, { launcher.launch(LOCATION_PERMISSIONS) }) }
}
