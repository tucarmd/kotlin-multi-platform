package com.learn.ndmkmp.platform

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.fake.FakeDeviceMonitor
import com.learn.ndmkmp.fake.FakeMediaPicker
import com.learn.ndmkmp.fake.FakePermissionController

/**
 * One per process: the BG task handler (P7) and the screen must share the same monitor / logger.
 * Swift reads it as `IosServicesKt.iosServices` (`AppModel.swift`).
 */
val iosServices: AppServices by lazy { createIosServices() }

/** The iOS side of [AppServices] — the only place that picks real vs fake on iOS. R5 swaps the fakes for Swift classes. */
fun createIosServices(): AppServices = AppServices(
    permissions = FakePermissionController(),   // R5: Swift (CBManager + AVCaptureDevice)
    ble = FakeBleClient(),                      // R5: Swift (CoreBluetooth)
    media = FakeMediaPicker(),                  // R5: Swift (UIImagePickerController / PHPicker)
    api = KtorApiService(),                     // P6 ✅ shared Ktor (Darwin engine)
    monitor = FakeDeviceMonitor(supportsForegroundService = false),  // R5: Swift providers + BGTaskScheduler
)
