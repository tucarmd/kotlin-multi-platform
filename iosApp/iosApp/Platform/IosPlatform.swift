import Foundation
import Shared

/// R5 — the one place iOS builds `AppServices`: Swift objects that call the Apple SDK, handed to Kotlin adapters.
/// One instance per process: the screen (`AppModel`) and the background task (R5c) must share the same monitor / log.
///
/// ```
///  IosPlatform.services
///   └─ IosServicesKt.createIosServices(
///        permissions: PermissionNative()     → Kotlin IosPermissionController   (R5a)
///      )
/// ```
enum IosPlatform {
    static let services: AppServices = IosServicesKt.createIosServices(permissions: PermissionNative())
}
