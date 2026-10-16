package com.learn.ndmkmp.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.permission.PermissionGroup
import com.learn.ndmkmp.core.permission.PermissionGroupState
import com.learn.ndmkmp.core.permission.PermissionStatus
import com.learn.ndmkmp.fake.FakePermissionController
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing

/**
 * Day 1 — runtime permission test screen: one card per feature, each card shows exactly 1 of the 4 states
 * and exactly 1 available action.
 *
 * ```
 *  PermissionScreen
 *   └─ Column(verticalScroll)
 *       ├─ AppHintText(instructions for the 3 test paths)
 *       ├─ AppCard "BLE Scan"      -> StatusChip + real permission names + button
 *       ├─ AppCard "BLE Connect"   -> ...
 *       ├─ AppCard "Camera"        -> ... (Day 1 exercise: a 2nd permission that isn't BLE)
 *       ├─ AppCard "Bluetooth radio" -> chip ON/OFF + "Ready to scan" + [Turn on]   (BLE Step 1)
 *       └─ AppCard "Debug"         -> Request all · Reset asked flags
 *
 *  which button shows:
 *    GRANTED            -> no button (nothing to do)
 *    NOT_REQUESTED      -> [Request permission]   (the system will show a dialog)
 *    DENIED             -> [Request permission] + a reason line (dialog still available)
 *    PERMANENTLY_DENIED -> [Open app settings]   (the dialog is dead)
 * ```
 *
 * This screen does NOT request permissions itself: that is the platform's job behind `PermissionController`
 * (Android needs `registerForActivityResult` ready before the Activity is STARTED). So here we only draw state + fire lambdas.
 */
@Composable
fun PermissionScreen(
    groups: List<PermissionGroup>,
    states: Map<String, PermissionGroupState>,
    lastResult: String,
    onRequest: (PermissionGroup) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestAll: () -> Unit,
    onResetAskedFlags: () -> Unit,
    // Day 1 (BLE Step 1): Bluetooth radio + the "can we scan yet" verdict — computed in `App()` from the PermissionController,
    // the screen only draws it (like the permission states above).
    radioState: BluetoothRadioState = BluetoothRadioState.OFF,
    bleReadiness: BleReadiness = BleReadiness.MISSING_PERMISSION,
    onTurnOnBluetooth: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.contentBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            AppHintText(PermissionStrings.HINT, bullet = false)

            if (lastResult.isNotEmpty()) {
                Text(
                    text = PermissionStrings.lastResultFormat(lastResult),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,      // raw result: read by eye, not prose
                    color = AppColor.textPrimary,
                )
            }

            groups.forEach { group ->
                // Group without state yet (first draw) → treat as not asked, no `!!` crash
                val state = states[group.id]
                PermissionGroupCard(
                    group = group,
                    state = state,
                    onRequest = { onRequest(group) },
                    onOpenSettings = onOpenSettings,
                )
            }

            BluetoothRadioCard(radioState, bleReadiness, onTurnOnBluetooth)

            AppCard(title = PermissionStrings.DEBUG_TITLE) {
                AppHintText(PermissionStrings.DEBUG_HINT)
                AppPrimaryButton(
                    text = PermissionStrings.REQUEST_ALL_BUTTON,
                    onClick = onRequestAll,
                )
                AppSecondaryButton(
                    text = PermissionStrings.RESET_FLAGS_BUTTON,
                    onClick = onResetAskedFlags,
                )
            }
        }
    }
}

/** 1 card = 1 feature. A separate function because the 3 cards are identical stroke for stroke — copying 4 times invites drift. */
@Composable
private fun PermissionGroupCard(
    group: PermissionGroup,
    state: PermissionGroupState?,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val status = state?.status ?: PermissionStatus.NOT_REQUESTED

    AppCard(title = groupLabel(group.id)) {
        AppHintText(groupWhy(group.id))

        StatusChip(status)

        // The REAL permission names on this device — this is where you see which API-level branch is running.
        // An API 33 device shows BLUETOOTH_SCAN; an API 30 device shows ACCESS_FINE_LOCATION. Same card.
        Text(
            text = if (group.runtimePermissions.isEmpty()) {
                PermissionStrings.NONE_NEEDED
            } else {
                group.runtimePermissions.joinToString("\n") { it.substringAfterLast('.') }
            },
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = AppColor.textSecondary,
        )
        if (group.installTimeNote.isNotEmpty()) {
            Text(
                text = PermissionStrings.installTimeFormat(group.installTimeNote),
                fontSize = 12.sp,
                color = AppColor.textSecondary,
            )
        }

        when (status) {
            // Granted → no button: a "Request" button now would show nothing
            // (the system sees it's granted → returns granted immediately) → the user thinks the button is broken.
            PermissionStatus.GRANTED -> Unit

            // Denied once: Android says "show a rationale" → explain first, then allow tapping again.
            PermissionStatus.DENIED -> {
                AppHintText(PermissionStrings.RATIONALE)
                AppPrimaryButton(PermissionStrings.REQUEST_BUTTON, onRequest)
            }

            // The dialog is dead: calling requestPermissions here is silent → switch to the Settings route.
            PermissionStatus.PERMANENTLY_DENIED -> {
                AppHintText(PermissionStrings.PERMANENTLY_DENIED_HINT)
                AppPrimaryButton(PermissionStrings.OPEN_SETTINGS_BUTTON, onOpenSettings)
            }

            PermissionStatus.NOT_REQUESTED ->
                if (group.runtimePermissions.isNotEmpty()) {
                    AppPrimaryButton(PermissionStrings.REQUEST_BUTTON, onRequest)
                }
        }
    }
}

/**
 * Day 1 (BLE Step 1) — last card: not a permission but the Bluetooth RADIO.
 *
 * Why on the Permission screen: it's the last "gate" before the Scanner (Day 2) scans — one screen shows
 * both conditions (permissions + radio on). The Scanner screen reuses exactly `BluetoothService.readiness()`.
 */
@Composable
private fun BluetoothRadioCard(
    radio: BluetoothRadioState,
    readiness: BleReadiness,
    onTurnOn: () -> Unit,
) {
    AppCard(title = PermissionStrings.RADIO_TITLE) {
        AppHintText(PermissionStrings.RADIO_HINT)
        when (radio) {
            BluetoothRadioState.ON -> LabelChip(PermissionStrings.RADIO_ON, AppColor.success)
            BluetoothRadioState.OFF -> LabelChip(PermissionStrings.RADIO_OFF, AppColor.warning)
            BluetoothRadioState.UNSUPPORTED -> LabelChip(PermissionStrings.RADIO_UNSUPPORTED, AppColor.danger)
        }
        Text(
            text = when (readiness) {
                BleReadiness.READY -> PermissionStrings.READY_YES
                BleReadiness.MISSING_PERMISSION -> PermissionStrings.READY_NO_PERMISSION
                BleReadiness.RADIO_OFF -> PermissionStrings.READY_RADIO_OFF
                BleReadiness.UNSUPPORTED -> PermissionStrings.READY_UNSUPPORTED
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (readiness == BleReadiness.READY) AppColor.success else AppColor.textPrimary,
        )
        // Only show the button when tapping it DOES something: radio off and permission granted (without BLUETOOTH_CONNECT
        // the enable-Bluetooth Intent throws SecurityException on API 31+).
        if (readiness == BleReadiness.RADIO_OFF) {
            AppPrimaryButton(PermissionStrings.TURN_ON_BUTTON, onTurnOn)
        }
    }
}

/**
 * Color label of the state. Colors come from the app tokens (`AppColor`), not hex codes typed here —
 * change the palette in one place and this screen follows.
 */
@Composable
private fun StatusChip(status: PermissionStatus) {
    val (label, color) = when (status) {
        PermissionStatus.GRANTED -> PermissionStrings.STATUS_GRANTED to AppColor.success
        PermissionStatus.DENIED -> PermissionStrings.STATUS_DENIED to AppColor.warning
        PermissionStatus.PERMANENTLY_DENIED -> PermissionStrings.STATUS_PERMANENTLY_DENIED to AppColor.danger
        PermissionStatus.NOT_REQUESTED -> PermissionStrings.STATUS_NOT_REQUESTED to AppColor.textSecondary
    }
    LabelChip(label, color)
}

/** Day 1 (BLE Step 1): the chip drawing is split out so the Bluetooth radio card shares it, no duplicate. */
@Composable
private fun LabelChip(label: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .background(color, RoundedCornerShape(12.dp))
                .padding(horizontal = AppSpacing.sm, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF5FF, widthDp = 380, heightDp = 900)
@Composable
private fun PermissionScreenPreview() {
    // Same groups as the fake (Android API 31+ shape) — the real per-platform list arrives in P3
    val groups = FakePermissionController.GROUPS
    PermissionScreen(
        groups = groups,
        states = mapOf(
            groups[0].id to PermissionGroupState(PermissionStatus.GRANTED, emptyMap()),
            groups[1].id to PermissionGroupState(PermissionStatus.DENIED, emptyMap()),
            groups[2].id to PermissionGroupState(PermissionStatus.PERMANENTLY_DENIED, emptyMap()),
        ),
        lastResult = "BLUETOOTH_SCAN=true, CAMERA=false",
        onRequest = {}, onOpenSettings = {}, onRequestAll = {}, onResetAskedFlags = {},
    )
}
