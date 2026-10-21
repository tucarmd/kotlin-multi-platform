package com.learn.nativedevicemonitor.ui.permission

import com.learn.nativedevicemonitor.nativecore.permission.PermissionGroupId

/**
 * Day 1 — Permissions screen strings (Rule 4: UI in English · Rule 5.3: Kotlin `object`, no strings.xml).
 */
object PermissionStrings {
    const val HINT = "One card per feature. Allow once to see Granted, deny once to see Denied, " +
        "deny twice to see Permanently denied — then the button turns into Open app settings."
    const val STATUS_GRANTED = "GRANTED"
    const val STATUS_DENIED = "DENIED"
    const val STATUS_PERMANENTLY_DENIED = "PERMANENTLY DENIED"
    const val STATUS_NOT_REQUESTED = "NOT REQUESTED"
    const val REQUEST_BUTTON = "Request permission"
    const val OPEN_SETTINGS_BUTTON = "Open app settings"
    const val RATIONALE = "Denied once. The system dialog still works — explain why, then ask again."
    const val PERMANENTLY_DENIED_HINT = "The system no longer shows a dialog. Enable it in app settings."
    const val NONE_NEEDED = "Nothing to request on this Android version"
    const val GROUP_BLE_SCAN = "BLE Scan"
    const val GROUP_BLE_SCAN_WHY = "Needed to find BLE devices nearby (Scanner tab)."
    const val GROUP_BLE_CONNECT = "BLE Connect"
    const val GROUP_BLE_CONNECT_WHY = "Needed to read device names and open a GATT connection."
    const val GROUP_CAMERA = "Camera"
    const val GROUP_CAMERA_WHY = "Practice only: a second runtime permission to test granted / denied."
    const val DEBUG_TITLE = "Debug"
    const val DEBUG_HINT = "Request all asks for every runtime permission in one dialog chain. " +
        "Reset clears only the app-side \"already asked\" flags — " +
        "run adb shell pm reset-permissions to reset the system side."
    const val REQUEST_ALL_BUTTON = "Request all"
    const val RESET_FLAGS_BUTTON = "Reset asked flags"

    // "Bluetooth radio" card: permissions complete? + radio on?
    const val RADIO_TITLE = "Bluetooth radio"
    const val RADIO_HINT = "Permissions say the app MAY use Bluetooth. The radio must also be ON " +
        "before the Scanner can scan. Toggle Bluetooth in Quick Settings — this card updates live."
    const val RADIO_ON = "RADIO ON"
    const val RADIO_OFF = "RADIO OFF"
    const val RADIO_UNSUPPORTED = "NO BLUETOOTH"
    const val READY_YES = "Ready to scan: YES"
    const val READY_NO_PERMISSION = "Ready to scan: NO — grant BLE Scan and BLE Connect above first"
    const val READY_RADIO_OFF = "Ready to scan: NO — Bluetooth is off"
    const val READY_UNSUPPORTED = "Ready to scan: NO — this device has no Bluetooth"
    const val TURN_ON_BUTTON = "Turn on Bluetooth"

    fun installTimeFormat(note: String) = "Install-time in manifest: $note"

    fun lastResultFormat(result: String) = "Last result: $result"
}

/**
 * Label lookup by `PermissionGroup.id` — nativecore only holds permission data, the ui layer attaches the text.
 * An unknown id shows the id itself on screen: add a group and forget its label and you see it immediately, not a blank.
 */
private val GROUP_LABELS: Map<String, Pair<String, String>> = mapOf(
    PermissionGroupId.BLE_SCAN to (PermissionStrings.GROUP_BLE_SCAN to PermissionStrings.GROUP_BLE_SCAN_WHY),
    PermissionGroupId.BLE_CONNECT to (PermissionStrings.GROUP_BLE_CONNECT to PermissionStrings.GROUP_BLE_CONNECT_WHY),
    PermissionGroupId.CAMERA to (PermissionStrings.GROUP_CAMERA to PermissionStrings.GROUP_CAMERA_WHY),
)

fun groupLabel(groupId: String): String = GROUP_LABELS[groupId]?.first ?: groupId

fun groupWhy(groupId: String): String = GROUP_LABELS[groupId]?.second.orEmpty()
