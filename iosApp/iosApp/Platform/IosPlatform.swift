import Foundation
import Shared

/// P12 — the one place iOS builds `AppServices`: Swift objects that call the Apple SDK, handed to Kotlin adapters.
/// One instance per process: the screen (`AppModel`) and the background task (`DeviceNative.registerRefreshTask`)
/// must share the same monitor and log file.
///
/// ```
///  IosPlatform.services
///   └─ IosServicesKt.createIosServices(
///        permissions: PermissionNative()     → Kotlin IosPermissionController   (P12a)
///        ble: BleNative()                    → Kotlin IosBleClient              (P12d)
///        media: MediaNative()                → Kotlin IosMediaPicker            (P12b)
///        device: DeviceNative()              → Kotlin LoopDeviceMonitor source + BGAppRefresh (P12c)
///      )
/// ```
enum IosPlatform {
    static let device = DeviceNative()

    static let services: AppServices = IosServicesKt.createIosServices(
        permissions: PermissionNative(),
        ble: BleNative(),
        media: MediaNative(),
        device: device
    )

    /// Called from `iOSApp.init` — BGTaskScheduler handlers must be registered before launch finishes.
    static func registerBackgroundTasks() {
        DeviceNative.registerRefreshTask(monitor: services.monitor, native: device)
    }
}
