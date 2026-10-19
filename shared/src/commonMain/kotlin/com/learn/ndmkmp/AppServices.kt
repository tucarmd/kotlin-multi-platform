package com.learn.ndmkmp

import com.learn.ndmkmp.core.background.DeviceMonitor
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.network.ApiService
import com.learn.ndmkmp.core.permission.PermissionController
import com.learn.ndmkmp.fake.FakeApiService
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.fake.FakeDeviceMonitor
import com.learn.ndmkmp.fake.FakeMediaPicker
import com.learn.ndmkmp.fake.FakePermissionController

/**
 * Everything platform-specific the 5 tabs need, as interfaces. The ONLY place that decides fake vs real:
 * Android `platform/AndroidServices.kt`, iOS `platform/IosServices.kt` (later: Swift implementations passed in).
 */
class AppServices(
    val permissions: PermissionController,
    val ble: BleClient,
    val media: MediaPicker,
    val api: ApiService,
    val monitor: DeviceMonitor,
) {
    companion object {
        fun fake() = AppServices(
            permissions = FakePermissionController(),
            ble = FakeBleClient(),
            media = FakeMediaPicker(),
            api = FakeApiService(),
            monitor = FakeDeviceMonitor(supportsForegroundService = platformName().startsWith("Android")),
        )
    }
}
