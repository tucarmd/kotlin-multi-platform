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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.permission.PermissionStatus
import com.learn.ndmkmp.presentation.permission.PermissionAction
import com.learn.ndmkmp.presentation.permission.PermissionCardUi
import com.learn.ndmkmp.presentation.permission.PermissionStrings
import com.learn.ndmkmp.presentation.permission.PermissionUiState
import com.learn.ndmkmp.presentation.permission.PermissionViewModel
import com.learn.ndmkmp.presentation.permission.radioLabel
import com.learn.ndmkmp.presentation.permission.statusLabel
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing

/**
 * Day 1 — runtime permission test screen: one card per feature, each card shows exactly 1 of the 4 states
 * and exactly 1 action. Which button shows is decided in [PermissionViewModel] (`PermissionCardUi.action`).
 *
 * ```
 *  PermissionScreen
 *   └─ Column(verticalScroll)
 *       ├─ AppHintText(instructions for the 3 test paths)
 *       ├─ AppCard "BLE Scan" · "BLE Connect" · "Camera"   -> StatusChip + real permission names + button
 *       ├─ AppCard "Bluetooth radio" -> chip ON/OFF + "Ready to scan" + [Turn on]
 *       └─ AppCard "Debug"           -> Request all · Reset asked flags
 * ```
 */
@Composable
fun PermissionScreen(vm: PermissionViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.contentBackground)
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppHintText(PermissionStrings.HINT, bullet = false)

        state.lastResult?.let {
            Text(it, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
        }

        state.cards.forEach { card ->
            PermissionGroupCard(card, onRequest = { vm.request(card.groupId) }, onOpenSettings = vm::openSettings)
        }

        BluetoothRadioCard(state, onTurnOn = vm::turnOnBluetooth)

        AppCard(title = PermissionStrings.DEBUG_TITLE) {
            AppHintText(PermissionStrings.DEBUG_HINT)
            AppPrimaryButton(text = PermissionStrings.REQUEST_ALL_BUTTON, onClick = vm::requestAll)
            AppSecondaryButton(text = PermissionStrings.RESET_FLAGS_BUTTON, onClick = vm::resetAskedFlags)
        }
    }
}

@Composable
private fun PermissionGroupCard(card: PermissionCardUi, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    AppCard(title = card.title) {
        AppHintText(card.why)
        LabelChip(statusLabel(card.status), statusColor(card.status))
        Text(card.permissionNames, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary)
        card.installTimeNote?.let { Text(it, fontSize = 12.sp, color = AppColor.textSecondary) }
        card.hint?.let { AppHintText(it) }
        when (card.action) {
            PermissionAction.NONE -> Unit
            PermissionAction.REQUEST -> AppPrimaryButton(PermissionStrings.REQUEST_BUTTON, onRequest)
            PermissionAction.OPEN_SETTINGS -> AppPrimaryButton(PermissionStrings.OPEN_SETTINGS_BUTTON, onOpenSettings)
        }
    }
}

/** Not a permission but the Bluetooth RADIO — the last gate before the Scanner can scan. */
@Composable
private fun BluetoothRadioCard(state: PermissionUiState, onTurnOn: () -> Unit) {
    AppCard(title = PermissionStrings.RADIO_TITLE) {
        AppHintText(PermissionStrings.RADIO_HINT)
        LabelChip(
            radioLabel(state.radio),
            when (state.radio) {
                BluetoothRadioState.ON -> AppColor.success
                BluetoothRadioState.OFF -> AppColor.warning
                BluetoothRadioState.UNSUPPORTED -> AppColor.danger
            },
        )
        Text(
            text = state.readinessText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (state.readiness == BleReadiness.READY) AppColor.success else AppColor.textPrimary,
        )
        if (state.showTurnOnBluetooth) AppPrimaryButton(PermissionStrings.TURN_ON_BUTTON, onTurnOn)
    }
}

/** Colors come from the app tokens (`AppColor`) — change the palette in one place and this screen follows. */
private fun statusColor(status: PermissionStatus): Color = when (status) {
    PermissionStatus.GRANTED -> AppColor.success
    PermissionStatus.DENIED -> AppColor.warning
    PermissionStatus.PERMANENTLY_DENIED -> AppColor.danger
    PermissionStatus.NOT_REQUESTED -> AppColor.textSecondary
}

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
