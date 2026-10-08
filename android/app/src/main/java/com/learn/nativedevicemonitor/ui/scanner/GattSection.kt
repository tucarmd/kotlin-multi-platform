package com.learn.nativedevicemonitor.ui.scanner

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.nativedevicemonitor.nativecore.bluetooth.BluetoothDecoder
import com.learn.nativedevicemonitor.nativecore.bluetooth.BluetoothLookup
import com.learn.nativedevicemonitor.nativecore.bluetooth.GattCharacteristicInfo
import com.learn.nativedevicemonitor.nativecore.bluetooth.GattServiceInfo
import com.learn.nativedevicemonitor.nativecore.bluetooth.GattValue
import com.learn.nativedevicemonitor.nativecore.bluetooth.RECEIVED_LOG_MAX
import com.learn.nativedevicemonitor.nativecore.bluetooth.WritePlan
import com.learn.nativedevicemonitor.nativecore.bluetooth.maxPayloadOf
import com.learn.nativedevicemonitor.nativecore.bluetooth.notifyTargetOf
import com.learn.nativedevicemonitor.nativecore.bluetooth.planWrite
import com.learn.nativedevicemonitor.nativecore.bluetooth.readTargetOf
import com.learn.nativedevicemonitor.nativecore.bluetooth.writeTargetOf
import com.learn.nativedevicemonitor.ui.theme.AppColor
import com.learn.nativedevicemonitor.ui.theme.AppRadius
import com.learn.nativedevicemonitor.ui.theme.AppSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Day 3 + Day 4 — the GATT block under Status, and the *Received Data* box. Takes state + lambdas, NOT the service:
 * readable means drawable, previewable / testable without Bluetooth.
 *
 * ```
 *  GATT                                   3 services · 9 characteristics
 *  Read target: 2A19 Battery Level        ← readTargetOf()
 *  Notify target: FFE1 UART data          ← notifyTargetOf()
 *  Write target: FFE1 UART data           ← writeTargetOf() / the row whose [Write] was just tapped      (task 004)
 *  MTU 247 · max 244 bytes per packet     ← service.mtu                                 (task 004)
 *  ┌────────────────────────────────────────────────┐
 *  │ 180F Battery                                   │
 *  │   2A19 Battery Level  ★ target                 │
 *  │   READ NOTIFY        [Read] [Notify] [Write]   │  ← buttons only show when the property allows
 *  └────────────────────────────────────────────────┘
 *  Write  To FFE1 UART data · with response          ← WriteBlock (task 004)
 *  [ ATZ\r                                      ]
 *  [Text][Hex]   [ATZ\r] [ATI\r] [010C\r]
 *  → 41 54 5A 0D (4 bytes)                  [Send]   ← planWrite(): preview or a red reason
 *  Last read: 2A19 Battery Level
 *    raw:   5A
 *    value: 90 %
 *  (red error: "Read 2A29 failed, status=2")
 *
 *  Received Data                          12 / 50  [Clear]
 *  ┌ 12:03:04.123 NOTIFY FFE1  41 54 0D → "AT\r" ┐   ← one line per notify packet
 * ```
 * Mirrors `ios/NativeDeviceMonitor/UI/Scanner/GattSection.swift`.
 */
@Composable
fun GattSection(
    isConnected: Boolean,
    isDiscovering: Boolean,
    services: List<GattServiceInfo>,
    lastRead: GattValue?,
    gattError: String?,
    mtu: Int,
    onRead: (GattCharacteristicInfo) -> Unit,
    onNotify: (GattCharacteristicInfo, Boolean) -> Unit,
    onWrite: (GattCharacteristicInfo, ByteArray, Boolean) -> Unit,
) {
    val readTarget = readTargetOf(services)
    val notifyTarget = notifyTargetOf(services)
    // Task 004 — write target: the row whose [Write] was just tapped; none tapped / another device lacks that key → auto-pick.
    // Only the KEY (a string) is kept for rememberSaveable; looked up again in the new services on every draw.
    var writeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val writeTarget = services.flatMap { it.characteristics }.firstOrNull { it.key == writeKey && it.properties.canWrite }
        ?: writeTargetOf(services)

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ScannerStrings.GATT_TITLE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.textPrimary,
                modifier = Modifier.weight(1f).padding(top = AppSpacing.xs),
            )
            if (services.isNotEmpty()) {
                Text(
                    ScannerStrings.servicesCountFormat(services.size, services.sumOf { it.characteristics.size }),
                    fontSize = 12.sp,
                    color = AppColor.textSecondary,
                )
            }
        }

        when {
            isDiscovering -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Hint(ScannerStrings.GATT_DISCOVERING, modifier = Modifier.padding(start = AppSpacing.sm))
            }
            !isConnected && services.isEmpty() -> Hint(ScannerStrings.GATT_NOT_CONNECTED)
            services.isEmpty() -> Hint(ScannerStrings.GATT_EMPTY)
            else -> {
                Hint(ScannerStrings.readTargetFormat(readTarget?.let(::characteristicLabel)))
                Hint(ScannerStrings.notifyTargetFormat(notifyTarget?.let(::characteristicLabel)))
                Hint(ScannerStrings.writeTargetFormat(writeTarget?.let(::characteristicLabel)))
                Hint(ScannerStrings.mtuFormat(mtu, maxPayloadOf(mtu)))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppColor.divider, RoundedCornerShape(AppRadius.field))
                        .padding(vertical = AppSpacing.xs),
                ) {
                    services.forEachIndexed { index, service ->
                        if (index > 0) HorizontalDivider(color = AppColor.divider)
                        ServiceBlock(
                            service = service,
                            targetKeys = setOfNotNull(readTarget?.key, notifyTarget?.key, writeTarget?.key),
                            writeKey = writeTarget?.key,
                            onRead = onRead,
                            onNotify = onNotify,
                            onSelectWrite = { writeKey = it.key },
                        )
                    }
                }
                writeTarget?.let { WriteBlock(target = it, mtu = mtu, onSend = onWrite) }
            }
        }

        if (lastRead != null) {
            Text(
                ScannerStrings.lastReadTitleFormat(
                    ScannerStrings.uuidWithName(lastRead.uuid, BluetoothLookup.characteristicName(lastRead.uuid)),
                ),
                fontSize = 13.sp,
                color = AppColor.textPrimary,
                modifier = Modifier.padding(top = AppSpacing.xs),
            )
            Mono(ScannerStrings.rawFormatLine(lastRead.hex))
            Mono(ScannerStrings.decodedFormatLine(lastRead.decoded))
        }
        // Read / subscribe errors don't drop the connection → red here, not over the Status
        gattError?.let { Text(it, fontSize = 12.sp, color = AppColor.danger) }
    }
}

@Composable
private fun ServiceBlock(
    service: GattServiceInfo,
    targetKeys: Set<String>,
    writeKey: String?,
    onRead: (GattCharacteristicInfo) -> Unit,
    onNotify: (GattCharacteristicInfo, Boolean) -> Unit,
    onSelectWrite: (GattCharacteristicInfo) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
        Text(
            ScannerStrings.uuidWithName(service.uuid, BluetoothLookup.serviceName(service.uuid)),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.primary,
        )
        service.characteristics.forEach { c ->
            Column(modifier = Modifier.padding(start = AppSpacing.sm, top = AppSpacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(characteristicLabel(c), fontSize = 12.sp, color = AppColor.textPrimary, modifier = Modifier.weight(1f, fill = false))
                    if (c.key in targetKeys) {
                        Text(ScannerStrings.TARGET_TAG, fontSize = 10.sp, color = AppColor.warning, modifier = Modifier.padding(start = AppSpacing.xs))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.properties.labels.joinToString(" "),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColor.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (c.properties.read) {
                        SmallButton(ScannerStrings.READ_BUTTON, active = false) { onRead(c) }
                    }
                    if (c.properties.canSubscribe) {
                        SmallButton(
                            if (c.isNotifying) ScannerStrings.STOP_NOTIFY_BUTTON else ScannerStrings.NOTIFY_BUTTON,
                            active = c.isNotifying,
                        ) { onNotify(c, !c.isNotifying) }
                    }
                    // Doesn't write right away: only SELECTS the target — the content is typed in WriteBlock below. Blue background = current write target.
                    if (c.properties.canWrite) {
                        SmallButton(ScannerStrings.WRITE_BUTTON, active = c.key == writeKey) { onSelectWrite(c) }
                    }
                }
            }
        }
    }
}

/**
 * Task 004 — command input + Send for [target]. The rules (writable? valid hex? longer than MTU?) all live in
 * [planWrite] — here we only print the preview / red reason and disable Send.
 *
 * The input state lives here (rememberSaveable), not pushed into the service: half-typed text is the screen's business,
 * the service only needs the final bytes when Send is tapped.
 */
@Composable
private fun WriteBlock(
    target: GattCharacteristicInfo,
    mtu: Int,
    onSend: (GattCharacteristicInfo, ByteArray, Boolean) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var asHex by rememberSaveable { mutableStateOf(false) }
    val plan = planWrite(input, asHex, target.properties, maxPayloadOf(mtu))

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs), modifier = Modifier.padding(top = AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ScannerStrings.WRITE_TITLE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.textPrimary)
            Hint(
                ScannerStrings.writeToFormat(characteristicLabel(target), target.properties.write),
                modifier = Modifier.padding(start = AppSpacing.sm),
            )
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (asHex) ScannerStrings.WRITE_PLACEHOLDER_HEX else ScannerStrings.WRITE_PLACEHOLDER_TEXT,
                    fontFamily = FontFamily.Monospace,
                    color = AppColor.textSecondary,
                )
            },
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            // AT commands are UPPERCASE; the keyboard auto-capitalizes + autocorrect off (ATZ corrected to "Atz" means the dongle won't understand)
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            shape = RoundedCornerShape(AppRadius.field),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = AppColor.border,
                focusedBorderColor = AppColor.primary,
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallButton(ScannerStrings.MODE_TEXT, active = !asHex) { asHex = false }
            SmallButton(ScannerStrings.MODE_HEX, active = asHex) { asHex = true }
            // Horizontal scroll: on narrow screens / large system fonts 5 buttons don't fit in one row
            Row(modifier = Modifier.weight(1f).padding(start = AppSpacing.sm).horizontalScroll(rememberScrollState())) {
                ScannerStrings.WRITE_PRESETS.forEach { preset ->
                    SmallButton(preset, active = false) { asHex = false; input = preset }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (plan) {
                is WritePlan.Ok -> Text(
                    ScannerStrings.writePreviewFormat(BluetoothDecoder.toHex(plan.bytes), plan.bytes.size),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AppColor.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                // An empty field isn't an error yet → grey text; everything else (bad hex, too long) → red
                is WritePlan.Invalid -> Text(
                    plan.reason,
                    fontSize = 12.sp,
                    color = if (input.isEmpty()) AppColor.textSecondary else AppColor.danger,
                    modifier = Modifier.weight(1f),
                )
            }
            SmallButton(ScannerStrings.SEND_BUTTON, active = plan is WritePlan.Ok, enabled = plan is WritePlan.Ok) {
                if (plan is WritePlan.Ok) onSend(target, plan.bytes, plan.withResponse)
            }
        }
        if (!asHex) Hint(ScannerStrings.WRITE_ESCAPE_HINT)
    }
}

/**
 * The *Received Data* box: dark terminal-style background, old lines on top, new ones at the bottom (like a log).
 * No LazyColumn: at most 50 lines inside an already scrolling Column — a plain Column is fast enough and needs no fixed height.
 */
@Composable
fun ReceivedDataSection(received: List<GattValue>, onClear: () -> Unit) {
    val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ScannerStrings.RECEIVED_TITLE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.textPrimary,
                modifier = Modifier.weight(1f).padding(top = AppSpacing.xs),
            )
            if (received.isNotEmpty()) {
                Text(ScannerStrings.receivedCountFormat(received.size, RECEIVED_LOG_MAX), fontSize = 12.sp, color = AppColor.textSecondary)
                SmallButton(ScannerStrings.CLEAR_BUTTON, active = false, onClick = onClear)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColor.terminalBackground, RoundedCornerShape(AppRadius.field))
                .padding(AppSpacing.md),
        ) {
            if (received.isEmpty()) {
                Text(ScannerStrings.RECEIVED_PLACEHOLDER, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.terminalText)
            }
            received.forEach { v ->
                Text(
                    ScannerStrings.receivedLineFormat(clock.format(Date(v.atMs)), v.source.name, v.uuid, v.hex, v.decoded),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,   // hex bytes: only readable when columns line up
                    color = AppColor.terminalText,
                )
            }
        }
    }
}

private fun characteristicLabel(c: GattCharacteristicInfo) =
    ScannerStrings.uuidWithName(c.uuid, BluetoothLookup.characteristicName(c.uuid))

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, color = AppColor.textSecondary, modifier = modifier)
}

@Composable
private fun Mono(text: String) {
    Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary, modifier = Modifier.padding(start = AppSpacing.sm))
}

/**
 * Small Read / Notify / Stop / Clear / Write / Send button. [active] = notifying / selected → blue background.
 * [enabled] false = dimmed + not tappable (Send while the input is invalid).
 */
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
