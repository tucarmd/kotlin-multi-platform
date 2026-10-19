import BackgroundTasks
import Network
import UIKit
import Shared

/// R5c — the Apple-SDK half of the Monitor tab, in Swift. Implements Kotlin `IosDeviceNative`; the loop, the JSONL file
/// and the status live in the shared Kotlin `LoopDeviceMonitor`. Ported from the old app's `DeviceProviders.swift` +
/// `BackgroundMonitor.swift`.
///
/// ```
///  batteryPercent()     UIDevice.batteryLevel (monitoring must be ON, Simulator = -1)
///  wifi / cellular      NWPathMonitor — iOS can't read the network once, only listen → keep the latest path
///  scheduleRefresh()    BGTaskScheduler.submit(BGAppRefreshTaskRequest, earliest 15 min)
///  registerRefreshTask  at launch: iOS wakes the app → monitor.collectAndSave("bgRefresh") → schedule the next one
/// ```
final class DeviceNative: NSObject, IosDeviceNative {

    /// Must MATCH Info.plist `BGTaskSchedulerPermittedIdentifiers` — off by one character and `submit` throws.
    static let refreshTaskId = "com.learn.ndmkmp.refresh"
    private static let refreshEarliest: TimeInterval = 15 * 60

    private let pathMonitor = NWPathMonitor()
    private let lock = NSLock()
    private var wifi = false
    private var cellular = false

    override init() {
        super.init()
        // Start listening now: the first path arrives tens of ms later (created lazily, the first read was always "no Wi-Fi").
        pathMonitor.pathUpdateHandler = { [weak self] path in
            let ok = path.status == .satisfied
            self?.lock.withLock {
                self?.wifi = ok && path.usesInterfaceType(.wifi)
                self?.cellular = ok && path.usesInterfaceType(.cellular)
            }
        }
        pathMonitor.start(queue: DispatchQueue(label: "DeviceNative.path"))
    }

    func batteryPercent() -> Int32 {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let level = UIDevice.current.batteryLevel
        return level < 0 ? -1 : Int32((level * 100).rounded())
    }

    func wifiConnected() -> Bool { lock.withLock { wifi } }

    func cellularAvailable() -> Bool { lock.withLock { cellular } }

    /// Submitting the same id again REPLACES the pending request. Simulator: always fails (BGTaskScheduler doesn't run there).
    func scheduleRefresh() -> String {
        let request = BGAppRefreshTaskRequest(identifier: Self.refreshTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: Self.refreshEarliest)
        do {
            try BGTaskScheduler.shared.submit(request)
            return "SCHEDULED (earliest in 15 min, iOS decides)"
        } catch {
            return "SUBMIT FAILED: \(error.localizedDescription)"
        }
    }

    func cancelRefresh() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.refreshTaskId)
    }

    /// Must run before launch finishes (`iOSApp.init`) — registering later crashes. iOS gives ~30 s; schedule the next
    /// run FIRST so a cut-off run still leaves one pending.
    static func registerRefreshTask(monitor: DeviceMonitor, native: DeviceNative) {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: refreshTaskId, using: nil) { task in
            _ = native.scheduleRefresh()
            let work = Task { @MainActor in
                _ = try? await monitor.collectAndSave(source: "bgRefresh")
                task.setTaskCompleted(success: true)
            }
            task.expirationHandler = { work.cancel() }
        }
    }
}
