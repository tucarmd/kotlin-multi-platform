package com.learn.ndmkmp.platform

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.fake.FakeDeviceMonitor

/**
 * The iOS side of [AppServices] — the only place that picks real vs fake on iOS.
 * R5: Swift calls this ONCE at launch (`IosPlatform.swift`) with its native objects (Apple SDK code written in Swift);
 * each Kotlin adapter keeps the state + rules and implements the `core/` interface.
 *
 * ```
 *  Swift  PermissionNative ──► IosPermissionController ──► PermissionController   (R5a)
 *  Swift  MediaNative      ──► IosMediaPicker          ──► MediaPicker            (R5b)
 * ```
 */
fun createIosServices(permissions: IosPermissionNative, media: IosMediaNative): AppServices = AppServices(
    permissions = IosPermissionController(permissions),          // R5a ✅
    ble = FakeBleClient(),                                       // R5d: Swift (CoreBluetooth)
    media = IosMediaPicker(media),                               // R5b ✅
    api = KtorApiService(),                                      // P6 ✅ shared Ktor (Darwin engine)
    monitor = FakeDeviceMonitor(supportsForegroundService = false),  // R5c: Swift providers + BGTaskScheduler
)
