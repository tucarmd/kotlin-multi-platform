package com.learn.ndmkmp.platform

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService

/**
 * The iOS side of [AppServices] — the only place that picks real vs fake on iOS.
 * R5: Swift calls this ONCE at launch (`IosPlatform.swift`) with its native objects (Apple SDK code written in Swift);
 * each Kotlin adapter keeps the state + rules and implements the `core/` interface.
 *
 * ```
 *  Swift  PermissionNative ──► IosPermissionController ──► PermissionController   (R5a)
 *  Swift  BleNative        ──► IosBleClient            ──► BleClient              (R5d)
 *  Swift  MediaNative      ──► IosMediaPicker          ──► MediaPicker            (R5b)
 *  Swift  DeviceNative     ──► IosDeviceStatusSource + IosPlatformBackground ──► LoopDeviceMonitor (R5c)
 * ```
 */
fun createIosServices(
    permissions: IosPermissionNative,
    ble: IosBleNative,
    media: IosMediaNative,
    device: IosDeviceNative,
): AppServices {
    val permissionController = IosPermissionController(permissions)
    val bleClient = IosBleClient(ble)
    return AppServices(
        permissions = permissionController,                      // R5a ✅
        ble = bleClient,                                         // R5d ✅
        media = IosMediaPicker(media),                           // R5b ✅
        api = KtorApiService(),                                  // P6 ✅ shared Ktor (Darwin engine)
        monitor = iosDeviceMonitor(device, permissionController, bleClient),   // R5c ✅
    )
}
