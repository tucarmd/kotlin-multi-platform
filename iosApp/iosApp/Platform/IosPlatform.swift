import Foundation
import Shared

/// R5 — the one place iOS builds `AppServices`: Swift objects that call the Apple SDK, handed to Kotlin adapters.
/// One instance per process: the screen (`AppModel`) and the background task (`DeviceNative.registerRefreshTask`)
/// must share the same monitor and log file.
///
/// ```
///  IosPlatform.services
///   └─ IosServicesKt.createIosServices(
///        permissions: PermissionNative()     → Kotlin IosPermissionController   (R5a)
///        media: MediaNative()                → Kotlin IosMediaPicker            (R5b)
///        device: DeviceNative()              → Kotlin LoopDeviceMonitor source + BGAppRefresh (R5c)
///      )
/// ```
enum IosPlatform {
    static let device = DeviceNative()

    static let services: AppServices = IosServicesKt.createIosServices(
        permissions: PermissionNative(),
        media: MediaNative(),
        device: device
    )

    /// Called from `iOSApp.init` — BGTaskScheduler handlers must be registered before launch finishes.
    static func registerBackgroundTasks() {
        DeviceNative.registerRefreshTask(monitor: services.monitor, native: device)
    }
}
