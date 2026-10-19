package com.learn.ndmkmp.ui.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.core.bluetooth.BleSignal
import com.learn.ndmkmp.presentation.scanner.DeviceRowUi
import com.learn.ndmkmp.presentation.scanner.ScannerStrings
import com.learn.ndmkmp.presentation.scanner.ScannerViewModel
import com.learn.ndmkmp.presentation.scanner.StatusTone
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppRadius
import com.learn.ndmkmp.ui.theme.AppSpacing

/**
 * Day 2 → Day 4 — Scanner screen per Learning Plan §4 mockup. Draws [ScannerViewModel.state]: which rows show,
 * which buttons are enabled, every label — all decided in the ViewModel. Only View things stay here:
 * keyboard, scrolling, colours.
 *
 * ```
 *  ScannerScreen
 *   └─ Column (verticalScroll)
 *       ├─ AppHintText                 how to start the peripheral
 *       ├─ [not ready] AppCard         missing permission / radio off → [Open Permissions]
 *       ├─ Device Name   [🔍 Search device…    ]  + 3 switches (CarMD only · Hide unknown · Hide audio)
 *       ├─ [ Scan Bluetooth ]
 *       ├─ Discovered Devices          3 of 7     (tap = select · tap again = advertising packet)
 *       ├─ [ Connect ] [ Disconnect ]
 *       ├─ ● Status: Connected to …    + raw lastEvent (status=133…)
 *       ├─ GattSection                 Day 3 + task 004
 *       └─ ReceivedDataSection         Day 4
 * ```
 */
@Composable
fun ScannerScreen(vm: ScannerViewModel, onOpenPermissions: () -> Unit) {
    val state by vm.state.collectAsState()

    // Clearing focus from the Device Name box = hide the keyboard (≈ iOS `@FocusState` set to false).
    val focusManager = LocalFocusManager.current
    val hideKeyboard = { focusManager.clearFocus() }
    val scrollState = rememberScrollState()
    // Dragging the screen drops the keyboard right away — it doesn't cover the Scan button / device list
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) hideKeyboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.surface)
            .pointerInput(Unit) { detectTapGestures { hideKeyboard() } }   // tap empty space = hide the keyboard
            .verticalScroll(scrollState)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        AppHintText(ScannerStrings.HINT, bullet = false)

        state.notReadyText?.let { text ->
            AppCard(title = ScannerStrings.NOT_READY_TITLE) {
                AppHintText(text)
                if (state.showOpenPermissions) AppSecondaryButton(ScannerStrings.OPEN_PERMISSIONS_BUTTON, onOpenPermissions)
            }
        }

        // --- Device Name + filters ---
        SectionLabel(ScannerStrings.DEVICE_NAME_LABEL)
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(ScannerStrings.SEARCH_PLACEHOLDER, color = AppColor.textSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppColor.textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { hideKeyboard() }),
            shape = RoundedCornerShape(AppRadius.field),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = AppColor.border, focusedBorderColor = AppColor.primary),
        )
        ToggleRow(ScannerStrings.FILTER_TOGGLE, state.carmdOnly, enabled = state.carmdToggleEnabled) { hideKeyboard(); vm.setCarmdOnly(it) }
        ToggleRow(ScannerStrings.HIDE_UNKNOWN_TOGGLE, state.hideUnknown) { hideKeyboard(); vm.setHideUnknown(it) }
        ToggleRow(ScannerStrings.HIDE_AUDIO_TOGGLE, state.hideAudio) { hideKeyboard(); vm.setHideAudio(it) }
        state.filterLines.forEach { Text(it, fontSize = 12.sp, color = AppColor.textSecondary) }

        AppPrimaryButton(text = state.scanButtonText, onClick = { hideKeyboard(); vm.onScanClick() }, enabled = state.scanEnabled)
        state.scanningText?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = AppSpacing.xs), strokeWidth = 2.dp)
                Text(it, fontSize = 13.sp, modifier = Modifier.padding(start = AppSpacing.xs))
            }
        }

        // --- Discovered Devices ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(ScannerStrings.DEVICES_TITLE, modifier = Modifier.weight(1f))
            state.matchCountText?.let { Text(it, fontSize = 12.sp, color = AppColor.textSecondary) }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.field))
                .border(1.dp, AppColor.divider, RoundedCornerShape(AppRadius.field)),
        ) {
            val empty = state.emptyListText
            if (empty != null) {
                Text(empty, fontSize = 13.sp, color = AppColor.textSecondary, modifier = Modifier.padding(AppSpacing.md))
            } else {
                // LazyColumn inside a verticalScroll Column MUST have a finite max height (else "infinity maximum height").
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    // key = address: RSSI changes constantly, without a key the list flickers + the selected row jumps around
                    itemsIndexed(state.rows, key = { _, row -> row.address }) { index, row ->
                        if (index > 0) HorizontalDivider(color = AppColor.divider)
                        DeviceRow(row, onClick = { hideKeyboard(); vm.onDeviceClick(row.address) })
                    }
                }
            }
        }
        if (state.showTapHint) Text(ScannerStrings.TAP_TO_EXPAND, fontSize = 11.sp, color = AppColor.textSecondary)

        // --- Connect / Disconnect ---
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            PanelButton(
                text = ScannerStrings.CONNECT_BUTTON,
                enabled = state.buttons.connectEnabled,
                loading = state.buttons.connecting,
                container = AppColor.contentBackground,
                content = AppColor.primary,
                borderColor = AppColor.secondary,
                modifier = Modifier.weight(1f),
                onClick = { hideKeyboard(); vm.connect() },
            )
            PanelButton(
                text = ScannerStrings.DISCONNECT_BUTTON,
                enabled = state.buttons.disconnectEnabled,
                loading = false,
                container = AppColor.disabled,
                content = AppColor.textPrimary,
                borderColor = null,
                modifier = Modifier.weight(1f),
                onClick = { hideKeyboard(); vm.disconnect() },
            )
        }

        // --- Status ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(toneColor(state.statusTone), CircleShape))
            Text(state.statusText, fontSize = 14.sp, color = AppColor.textPrimary, modifier = Modifier.padding(start = AppSpacing.sm))
        }
        state.lastEvent?.let { Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary) }

        GattSection(state, vm, hideKeyboard)
        ReceivedDataSection(state, onClear = vm::clearReceived)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.textPrimary, modifier = modifier.padding(top = AppSpacing.xs))
}

/** One row: name + address on the left, signal bars + dBm on the right; advertising lines below when expanded. */
@Composable
private fun DeviceRow(row: DeviceRowUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (row.isStale) 0.4f else 1f)
            .background(if (row.isSelected) AppColor.contentBackground else AppColor.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.name,
                        fontSize = 15.sp,
                        fontWeight = if (row.isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (row.isSelected) AppColor.primary else AppColor.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.isConnected) {
                        Text(
                            text = ScannerStrings.CONNECTED_TAG,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = AppSpacing.xs)
                                .background(AppColor.success, RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(row.address, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary)
            }
            SignalBars(row.signal)
            Text(row.rssiText, fontSize = 12.sp, color = AppColor.textSecondary, modifier = Modifier.padding(start = AppSpacing.xs))
        }
        if (row.detailLines.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = AppSpacing.sm)) {
                row.detailLines.forEach { Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary) }
            }
        }
    }
}

/** Half-width button for the Connect / Disconnect pair (mockup: 2 buttons side by side, light background). */
@Composable
private fun PanelButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    container: Color,
    content: Color,
    borderColor: Color?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    // Surface(onClick): shape + background + border + touch area + ripple in the right order (hand-assembled lost its
    // background on a Samsung A32).
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(AppRadius.field),
        color = container,
        border = borderColor?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            modifier = Modifier.alpha(if (enabled || loading) 1f else 0.45f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = content)
                Box(Modifier.width(AppSpacing.sm))
            }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = content)
        }
    }
}

/** 3 small signal bars. "Off" bars are still drawn (pale) so the eye can compare 1/3 with 3/3. */
@Composable
private fun SignalBars(signal: BleSignal) {
    val color = when (signal) {
        BleSignal.STRONG -> AppColor.success
        BleSignal.MEDIUM -> AppColor.warning
        BleSignal.WEAK -> AppColor.danger
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(5.dp, 8.dp, 11.dp).forEachIndexed { i, h ->
            Box(Modifier.width(3.dp).height(h).background(if (i < signal.bars) color else AppColor.disabled, RoundedCornerShape(1.dp)))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = AppColor.textSecondary)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

private fun toneColor(tone: StatusTone) = when (tone) {
    StatusTone.CONNECTED -> AppColor.success
    StatusTone.ERROR -> AppColor.danger
    StatusTone.CONNECTING -> AppColor.warning
    StatusTone.NEUTRAL -> AppColor.textSecondary
}
