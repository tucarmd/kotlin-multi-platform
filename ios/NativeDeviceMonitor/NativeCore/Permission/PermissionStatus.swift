import Foundation

/// Day 1 — the 4 states of ONE permission on iOS. Mirrors `android/.../nativecore/permission/PermissionStatus.kt`,
/// but it is **not a 1-to-1 translation** — read the table below before comparing the two codebases.
///
/// ## Android ↔ iOS: the most important difference of Day 1
///
/// ```
///   Android                            iOS
///   ------------------------------     ------------------------------
///   NOT_REQUESTED                  <->  .notDetermined     can ask, the system WILL show a dialog
///   DENIED  (denied once)          ─┐
///                                   ├->  .denied           ONLY THE SETTINGS ROUTE LEFT
///   PERMANENTLY_DENIED (twice)     ─┘
///   GRANTED                        <->  .granted
///   (none)                         <->  .restricted        LOCKED by Screen Time / MDM
/// ```
///
/// **Why Android splits into 2 while iOS merges into 1:** Android lets an app ask again several times, so it must
/// tell "denied but can still ask" from "denied to the point the system goes silent". iOS shows the dialog
/// **exactly once per permission, for the whole lifetime of the app install** — calling `requestAccess` a second time
/// returns the old result immediately and **shows nothing**. So iOS needs only 1 state
/// `.denied`, and it **always** means "only Settings left".
///
/// Consequence for the UI: a card in the `.denied` state on iOS must show an **Open Settings** button, never
/// "Request permission" like Android — tapping it does nothing and the user thinks the app is broken.
///
/// **`.restricted` is something Android doesn't have:** the device is locked by Screen Time (Content & Privacy Restrictions)
/// or a company MDM. The user CANNOT enable it — Settings doesn't even show the switch.
/// Merging it into `.denied` is wrong: the app would lure the user into Settings looking for a button that doesn't exist.
///
/// `enum` = a CLOSED set, a misspelled name is a compile error — like `enum class` in Kotlin,
/// very different from passing a hand-typed `"denied"` string.
enum PermissionStatus {

    /// Never asked → tapping Request makes the system show the real dialog.
    case notDetermined

    /// Granted — the API can be called.
    case granted

    /// Denied. On iOS this is a **final state**: the dialog never shows again.
    case denied

    /// Locked by Screen Time / MDM. The user can't change it, Settings has no switch either.
    case restricted

    /// Only the Settings route left → the UI renames the button to "Open Settings".
    ///
    /// `.restricted` is NOT here: opening Settings is pointless, the UI must say plainly that it's locked.
    var needsSettings: Bool { self == .denied }

    /// Can we still ask — decides whether the "Request permission" button shows.
    var canRequest: Bool { self == .notDetermined }

    /// From best to worst: granted(0) < notDetermined(1) < denied(2) < restricted(3).
    ///
    /// Explicit numbers instead of relying on declaration order (`allCases.firstIndex`) because reordering `case`s
    /// in the enum is harmless and shouldn't silently break the group-state logic.
    var severity: Int {
        switch self {
        case .granted: return 0
        case .notDetermined: return 1
        case .denied: return 2
        case .restricted: return 3
        }
    }
}

/// Result for a whole permission GROUP (one app feature).
///
/// Keeps both the overall [status] and the detailed [perPermission]: the UI shows 1 line to stay compact, but when they diverge
/// (Camera granted but Bluetooth not) you immediately see which permission is missing, no digging through logs.
///
/// Mirrors `PermissionGroupState` in Kotlin. A `struct` (value type) rather than a `class`:
/// UI state should be copyable, not shared by reference — assigning it into an `@Observable` tells SwiftUI
/// there's a new value to redraw.
struct PermissionGroupState {
    let status: PermissionStatus
    /// group id (e.g. "bluetooth") -> its own state
    let perPermission: [String: PermissionStatus]

    var isGranted: Bool { status == .granted }
    var needsSettings: Bool { status.needsSettings }

    /// Names of permissions not yet granted — shown in the UI to say exactly what's missing.
    func missingShortNames() -> [String] {
        perPermission
            .filter { $0.value != .granted }
            .keys
            .sorted()          // Dictionary has no order → sort so the UI doesn't jump around on every draw
    }

    /// The WORST state becomes the group state.
    ///
    /// Why not "at least 1 granted is enough": the feature needs the full set of permissions to work.
    /// An empty group (no permission to request on iOS) counts as `.granted` — nothing blocks it.
    static func from(_ perPermission: [String: PermissionStatus]) -> PermissionGroupState {
        let worst = perPermission.values.max(by: { $0.severity < $1.severity }) ?? .granted
        return PermissionGroupState(status: worst, perPermission: perPermission)
    }
}
