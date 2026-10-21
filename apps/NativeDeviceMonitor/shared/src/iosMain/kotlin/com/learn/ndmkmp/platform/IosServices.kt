package com.learn.ndmkmp.platform

import com.learn.ndmkmp.AppServices
import com.learn.ndmkmp.core.network.KtorApiService

/**
 * The iOS side of [AppServices] — the only place that picks real vs fake on iOS.
 * P12: Swift calls this ONCE at launch (`IosPlatform.swift`) with its native objects (Apple SDK code written in Swift);
 * each Kotlin adapter keeps the state + rules and implements the `core/` interface.
 *
 * ```
 *  Swift  PermissionNative ──► IosPermissionController ──► PermissionController   (P12a)
 *  Swift  BleNative        ──► IosBleClient            ──► BleClient              (P12d)
 *  Swift  MediaNative      ──► IosMediaPicker          ──► MediaPicker            (P12b)
 *  Swift  DeviceNative     ──► IosDeviceStatusSource + IosPlatformBackground ──► LoopDeviceMonitor (P12c)
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
        permissions = permissionController,                      // P12a ✅
        ble = bleClient,                                         // P12d ✅
        media = IosMediaPicker(media),                           // P12b ✅
        api = KtorApiService(),                                  // P6 ✅ shared Ktor (Darwin engine)
        monitor = iosDeviceMonitor(device, permissionController, bleClient),   // P12c ✅
    )
}
