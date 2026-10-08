package com.learn.nativedevicemonitor.nativecore.permission

import android.Manifest
import android.os.Build

/**
 * Day 1 — ONE permission group = one app FEATURE, not one Android constant.
 *
 * Why group by feature: users understand "the app wants to find the dongle", not `BLUETOOTH_SCAN`. And the same
 * feature needs a different SET of permissions depending on the Android version (see [permissionGroups]) — a UI driven
 * by features looks the same on every device, only the internals change.
 *
 * This phase only needs BLE + Camera (exactly the Day 1 exercise).
 *
 * @param runtimePermissions permissions that MUST be requested at runtime — ALREADY filtered by the running device's API level.
 *        Empty = nothing to request on this device (e.g. ble_connect on API ≤ 30).
 * @param installTimeNote related install-time permissions — only shown in the UI, NOT requested.
 *
 * No label/why here: the text shown in the UI belongs to `ui/permission/PermissionStrings.kt`
 * (nativecore holds no UI strings — dependencies go one way `ui/` → `nativecore/`).
 */
data class PermissionGroup(
    val id: String,
    val runtimePermissions: List<String>,
    val installTimeNote: String = "",
)

/** id shared between the Activity (computing readiness) and the Permission screen (looking up labels). Wrong name = build error. */
object PermissionGroupId {
    const val BLE_SCAN = "ble_scan"
    const val BLE_CONNECT = "ble_connect"
    const val CAMERA = "camera"
}

/**
 * Permission matrix by API level — `minSdk` 26 so **both branches run for real**:
 *
 * ```
 *   BLE scan     | API 26–30: ACCESS_FINE_LOCATION (runtime) + BLUETOOTH/ADMIN (install-time)
 *                | API 31+  : BLUETOOTH_SCAN (runtime)
 *   BLE connect  | API 26–30: nothing to request (install-time BLUETOOTH is enough)
 *                | API 31+  : BLUETOOTH_CONNECT (runtime)
 *   Camera       | all      : CAMERA (runtime)
 * ```
 *
 * `fun` not `val`: the list depends on `Build.VERSION.SDK_INT` → passing sdkInt in makes it testable/previewable.
 *
 * ⚠️ API ≤ 30 devices also need the **Location service turned on** (not just the permission) to find devices.
 */
fun permissionGroups(sdkInt: Int = Build.VERSION.SDK_INT): List<PermissionGroup> = listOf(
    PermissionGroup(
        id = PermissionGroupId.BLE_SCAN,
        runtimePermissions = if (sdkInt >= Build.VERSION_CODES.S) {
            // The Manifest also declares usesPermissionFlags="neverForLocation" so location is no longer required
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // API ≤ 30: without this permission the scan returns 0 devices with NO error at all
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        installTimeNote = if (sdkInt >= Build.VERSION_CODES.S) "" else "BLUETOOTH, BLUETOOTH_ADMIN",
    ),
    PermissionGroup(
        id = PermissionGroupId.BLE_CONNECT,
        runtimePermissions = if (sdkInt >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList()
        },
        installTimeNote = if (sdkInt >= Build.VERSION_CODES.S) "" else "BLUETOOTH",
    ),
    PermissionGroup(
        id = PermissionGroupId.CAMERA,
        // CAMERA is a runtime permission since API 23, minSdk 26 → no branching
        runtimePermissions = listOf(Manifest.permission.CAMERA),
    ),
)
