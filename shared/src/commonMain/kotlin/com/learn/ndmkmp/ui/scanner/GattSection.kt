package com.learn.ndmkmp.ui.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.presentation.scanner.GattServiceUi
import com.learn.ndmkmp.presentation.scanner.ScannerStrings
import com.learn.ndmkmp.presentation.scanner.ScannerUiState
import com.learn.ndmkmp.presentation.scanner.ScannerViewModel
import com.learn.ndmkmp.presentation.scanner.WriteUi
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppRadius
import com.learn.ndmkmp.ui.theme.AppSpacing

/**
 * Day 3 + Day 4 + task 004 — the GATT block under Status, and the *Received Data* box.
 * Targets, write plan, labels: all computed in [ScannerViewModel]; this file only draws [ScannerUiState].
 *
 * ```
 *  GATT                                   3 services · 9 characteristics
 *  Read target / Notify target / Write target / MTU 247 · max 244 bytes
 *  ┌────────────────────────────────────────────────┐
 *  │ 180F Battery                                   │
 *  │   2A19 Battery Level  ★ target                 │
 *  │   READ NOTIFY        [Read] [Notify] [Write]   │
 *  └────────────────────────────────────────────────┘
 *  Write  To FFE1 UART data · with response
 *  [ ATZ\r ]   [Text][Hex]  [ATZ\r] [ATI\r] [010C\r]
 *  → 41 54 5A 0D (4 bytes)                  [Send]
 *  Last read: 2A19 Battery Level · raw · value
 * ```
 */
@Composable
fun GattSection(state: ScannerUiState, vm: ScannerViewModel, hideKeyboard: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ScannerStrings.GATT_TITLE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.textPrimary,
                modifier = Modifier.weight(1f).padding(top = AppSpacing.xs),
            )
            state.gattCountText?.let { Text(it, fontSize = 12.sp, color = AppColor.textSecondary) }
        }

        state.gattMessage?.let { msg ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isDiscovering) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp).padding(end = AppSpacing.sm), strokeWidth = 2.dp)
                }
                Hint(msg)
            }
        }
        state.targetLines.forEach { Hint(it) }
        if (state.services.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppColor.divider, RoundedCornerShape(AppRadius.field))
                    .padding(vertical = AppSpacing.xs),
            ) {
                state.services.forEachIndexed { index, service ->
                    if (index > 0) HorizontalDivider(color = AppColor.divider)
                    ServiceBlock(service, vm)
                }
            }
        }
        state.write?.let { WriteBlock(it, vm, hideKeyboard) }

        if (state.lastReadLines.isNotEmpty()) {
            Text(state.lastReadLines.first(), fontSize = 13.sp, color = AppColor.textPrimary, modifier = Modifier.padding(top = AppSpacing.xs))
            state.lastReadLines.drop(1).forEach { Mono(it) }
        }
        // Read / subscribe errors don't drop the connection → red here, not over the Status
        state.gattError?.let { Text(it, fontSize = 12.sp, color = AppColor.danger) }
    }
}

@Composable
private fun ServiceBlock(service: GattServiceUi, vm: ScannerViewModel) {
    Column(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
        Text(service.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.primary)
        service.characteristics.forEach { c ->
            Column(modifier = Modifier.padding(start = AppSpacing.sm, top = AppSpacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.label, fontSize = 12.sp, color = AppColor.textPrimary, modifier = Modifier.weight(1f, fill = false))
                    if (c.isTarget) {
                        Text(ScannerStrings.TARGET_TAG, fontSize = 10.sp, color = AppColor.warning, modifier = Modifier.padding(start = AppSpacing.xs))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.propertiesText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColor.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (c.canRead) SmallButton(ScannerStrings.READ_BUTTON, active = false) { vm.read(c.info) }
                    if (c.canNotify) SmallButton(c.notifyButtonText, active = c.isNotifying) { vm.setNotify(c.info, !c.isNotifying) }
                    if (c.canWrite) SmallButton(ScannerStrings.WRITE_BUTTON, active = c.isWriteTarget) { vm.selectWriteTarget(c.info) }
                }
            }
        }
    }
}

/** Task 004 — command input + Send. The rules (writable? valid hex? longer than MTU?) live in `planWrite()`. */
@Composable
private fun WriteBlock(write: WriteUi, vm: ScannerViewModel, hideKeyboard: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs), modifier = Modifier.padding(top = AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ScannerStrings.WRITE_TITLE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.textPrimary)
            Hint(write.toText, modifier = Modifier.padding(start = AppSpacing.sm))
        }
        OutlinedTextField(
            value = write.input,
            onValueChange = vm::onWriteInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(write.placeholder, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            // AT commands are UPPERCASE; autocorrect off (ATZ corrected to "Atz" means the dongle won't understand)
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            shape = RoundedCornerShape(AppRadius.field),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = AppColor.border, focusedBorderColor = AppColor.primary),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallButton(ScannerStrings.MODE_TEXT, active = !write.asHex) { vm.setWriteHex(false) }
            SmallButton(ScannerStrings.MODE_HEX, active = write.asHex) { vm.setWriteHex(true) }
            // Horizontal scroll: on narrow screens / large system fonts 5 buttons don't fit in one row
            Row(modifier = Modifier.weight(1f).padding(start = AppSpacing.sm).horizontalScroll(rememberScrollState())) {
                ScannerStrings.WRITE_PRESETS.forEach { preset -> SmallButton(preset, active = false) { vm.applyWritePreset(preset) } }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                write.previewText,
                fontSize = 12.sp,
                fontFamily = if (write.canSend) FontFamily.Monospace else FontFamily.Default,
                color = if (write.previewIsError) AppColor.danger else AppColor.textSecondary,
                modifier = Modifier.weight(1f),
            )
            SmallButton(ScannerStrings.SEND_BUTTON, active = write.canSend, enabled = write.canSend) { hideKeyboard(); vm.send() }
        }
        if (write.showEscapeHint) Hint(ScannerStrings.WRITE_ESCAPE_HINT)
    }
}

/** The *Received Data* box: dark terminal background, old lines on top, new ones at the bottom (like a log). */
@Composable
fun ReceivedDataSection(state: ScannerUiState, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ScannerStrings.RECEIVED_TITLE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.textPrimary,
                modifier = Modifier.weight(1f).padding(top = AppSpacing.xs),
            )
            state.receivedCountText?.let {
                Text(it, fontSize = 12.sp, color = AppColor.textSecondary)
                SmallButton(ScannerStrings.CLEAR_BUTTON, active = false, onClick = onClear)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColor.terminalBackground, RoundedCornerShape(AppRadius.field))
                .padding(AppSpacing.md),
        ) {
            if (state.receivedLines.isEmpty()) {
                Text(ScannerStrings.RECEIVED_PLACEHOLDER, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.terminalText)
            }
            state.receivedLines.forEach {
                // hex bytes: only readable when columns line up
                Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.terminalText)
            }
        }
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, color = AppColor.textSecondary, modifier = modifier)
}

@Composable
private fun Mono(text: String) {
    Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary, modifier = Modifier.padding(start = AppSpacing.sm))
}

/** Small Read / Notify / Stop / Clear / Write / Send button. [active] = blue background. */
@Composable
private fun SmallButton(text: String, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(start = AppSpacing.xs).height(28.dp).alpha(if (enabled) 1f else 0.4f),
        shape = RoundedCornerShape(AppRadius.field),
        color = if (active) AppColor.primary else AppColor.contentBackground,
        border = BorderStroke(1.dp, AppColor.secondary),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else AppColor.primary)
        }
    }
}
