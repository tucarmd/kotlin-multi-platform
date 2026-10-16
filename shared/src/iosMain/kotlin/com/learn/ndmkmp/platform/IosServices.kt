package com.learn.ndmkmp.platform

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.fake.FakeDeviceMonitor
import com.learn.ndmkmp.fake.FakeMediaPicker
import com.learn.ndmkmp.fake.FakePermissionController

/** The iOS side of [AppServices] — the only place that picks real vs fake on iOS. Built once per process. */
fun createIosServices(): AppServices = AppServices(
    permissions = FakePermissionController(),   // P3
    ble = FakeBleClient(),                      // P4
    media = FakeMediaPicker(),                  // P5
    api = KtorApiService(),                     // P6 ✅
    monitor = FakeDeviceMonitor(supportsForegroundService = false),  // P7
)
