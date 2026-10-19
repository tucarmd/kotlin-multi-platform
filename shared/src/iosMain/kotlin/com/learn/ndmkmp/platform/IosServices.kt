package com.learn.ndmkmp.platform

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService
import com.learn.ndmkmp.fake.FakeBleClient

/**
 * The iOS side of [AppServices] — the only place that picks real vs fake on iOS.
 * R5: Swift calls this ONCE at launch (`IosPlatform.swift`) with its native objects (Apple SDK code written in Swift);
 * each Kotlin adapter keeps the state + rules and implements the `core/` interface.
 *
 * ```
 *  Swift  PermissionNative ──► IosPermissionController ──► PermissionController   (R5a)
 *  Swift  MediaNative      ──► IosMediaPicker          ──► MediaPicker            (R5b)
 *  Swift  DeviceNative     ──► IosDeviceStatusSource + IosPlatformBackground ──► LoopDeviceMonitor (R5c)
 * ```
 */
fun createIosServices(permissions: IosPermissionNative, media: IosMediaNative, device: IosDeviceNative): AppServices {
    val permissionController = IosPermissionController(permissions)
    val ble = FakeBleClient()                                    // R5d: Swift (CoreBluetooth)
    return AppServices(
        permissions = permissionController,                      // R5a ✅
        ble = ble,
        media = IosMediaPicker(media),                           // R5b ✅
        api = KtorApiService(),                                  // P6 ✅ shared Ktor (Darwin engine)
        monitor = iosDeviceMonitor(device, permissionController, ble),   // R5c ✅
    )
}
