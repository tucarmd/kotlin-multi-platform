package dev.devicepulse.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermission(): PermissionRequester {
    val permission = remember { IosLocationPermission() }
    DisposableEffect(permission) { onDispose { permission.dispose() } }
    return permission
}

private class IosLocationPermission : PermissionRequester {
    private val manager = CLLocationManager()
    private var status by mutableStateOf(manager.authorizationStatus)

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            status = manager.authorizationStatus
        }
    }

    init {
        manager.delegate = delegate
    }

    override val granted: Boolean
        get() = status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways

    // iOS shows the prompt only once; after "Don't Allow" the answer can only change in Settings.
    override val canPrompt: Boolean
        get() = status == kCLAuthorizationStatusNotDetermined

    override fun request() {
        if (canPrompt) {
            manager.requestWhenInUseAuthorization()
        } else {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
        }
    }

    fun dispose() {
        manager.delegate = null
    }
}
