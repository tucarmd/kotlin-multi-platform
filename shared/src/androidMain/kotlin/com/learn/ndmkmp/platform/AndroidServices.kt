package com.learn.ndmkmp.platform

import androidx.activity.ComponentActivity
import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.fake.FakeDeviceMonitor
import com.learn.ndmkmp.fake.FakeMediaPicker
import com.learn.ndmkmp.fake.FakePermissionController

/**
 * The Android side of [AppServices] — the only place that picks real vs fake on Android.
 * Must be called in `MainActivity.onCreate` BEFORE the Activity is STARTED: the permission / camera launchers
 * (`registerForActivityResult`) have to be registered that early.
 */
fun createAndroidServices(activity: ComponentActivity): AppServices = AppServices(
    permissions = FakePermissionController(),   // P3
    ble = FakeBleClient(),                      // P4
    media = FakeMediaPicker(),                  // P5
    api = KtorApiService(),                     // P6 ✅
    monitor = FakeDeviceMonitor(supportsForegroundService = true),   // P7
)
