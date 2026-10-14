package dev.devicepulse.sensor

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
import androidx.core.content.ContextCompat

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
actual fun rememberLocationPermission(): PermissionRequester {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            LOCATION_PERMISSIONS.any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        isGranted = result.values.any { it }
    }
    return remember(launcher) {
        object : PermissionRequester {
            override val granted: Boolean get() = isGranted

            // Android decides itself whether to show the dialog (it stays silent after "Don't ask again").
            override val canPrompt: Boolean get() = true
            override fun request() = launcher.launch(LOCATION_PERMISSIONS)
        }
    }
}
