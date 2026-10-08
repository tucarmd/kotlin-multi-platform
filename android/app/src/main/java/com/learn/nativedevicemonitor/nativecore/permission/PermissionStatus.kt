package com.learn.nativedevicemonitor.nativecore.permission

/**
 * Day 1 — the 4 states of ONE runtime permission.
 *
 * Why 4 instead of a `Boolean granted`:
 * Android only lets you ask exactly 1 question directly — "granted yet?" (`checkSelfPermission`).
 * But the UI must tell apart 3 kinds of "not granted" because each one leads to a DIFFERENT action:
 *
 * ```
 * NOT_REQUESTED        -> never asked        -> tapping Request SHOWS the system dialog
 * DENIED               -> denied once        -> show the reason then Request again, dialog still available
 * PERMANENTLY_DENIED   -> denied twice       -> Request shows NOTHING anymore -> only Settings left
 * ```
 *
 * Collapsing these 3 into `false` is the classic bug: the app shows "Request permissions", the user taps,
 * no dialog appears (because it's permanently denied), the user thinks the app is broken.
 *
 * `enum class` = a CLOSED set of values, a wrong name is a build error (unlike a hand-typed `"denied"` string).
 */
enum class PermissionStatus {
    /** Never requested (fresh install, or right after `pm reset-permissions`). */
    NOT_REQUESTED,

    /** Granted — the API can be called. */
    GRANTED,

    /** Denied but the system STILL lets us ask again (Android calls it "should show rationale"). */
    DENIED,

    /** Denied to the point the system no longer shows a dialog. The only way: open App Settings. */
    PERMANENTLY_DENIED,
}

/**
 * Result for a whole permission GROUP (e.g. BLE scan on API ≤ 30 = 1 location permission + 2 install-time ones).
 *
 * Keeps both the overall [status] and the detailed [perPermission]: the UI shows 1 status line to stay compact, but when
 * they diverge (Camera granted but Location not) you can still see which permission is missing — no digging through logs.
 */
data class PermissionGroupState(
    val status: PermissionStatus,
    val perPermission: Map<String, PermissionStatus>,
) {
    val isGranted: Boolean get() = status == PermissionStatus.GRANTED

    /** Only the Settings route is left → the UI renames the button from "Request" to "Open Settings". */
    val needsSettings: Boolean get() = status == PermissionStatus.PERMANENTLY_DENIED

    /** Short names of permissions not yet granted — shown in the UI to say exactly what's missing. */
    fun missingShortNames(): List<String> = perPermission
        .filterValues { it != PermissionStatus.GRANTED }
        .keys
        .map { it.substringAfterLast('.') }     // android.permission.CAMERA -> CAMERA
}
