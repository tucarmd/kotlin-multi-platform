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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.monotonicMs
import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.bluetooth.BleSignal
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import com.learn.ndmkmp.core.bluetooth.BluetoothLookup
import com.learn.ndmkmp.core.bluetooth.BluetoothScanFilter
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppRadius
import com.learn.ndmkmp.ui.theme.AppSpacing
import kotlinx.coroutines.delay

/**
 * Day 2 → Day 4 — Scanner screen per Learning Plan §4 mockup ("main screen — BLE + status + received data").
 * The blue "Native Device Monitor" header is `MainActivity`'s TopAppBar, not here.
 *
 * ```
 *  ScannerScreen
 *   └─ Column (verticalScroll)
 *       ├─ AppHintText                 how to start the peripheral
 *       ├─ [not ready] AppCard         missing permission / radio off → [Open Permissions]
 *       ├─ Device Name   [🔍 Search device…    ]   ← matchesName(): filters the current list as you type
 *       │                                           Enter / tap outside / drag the screen / tap a button = hide keyboard
 *       │                [CarMD tools only ○]      ← filters DURING SCAN (BluetoothScanFilter) — a different thing
 *       │                [Hide unknown devices ○]  ← isDeviceShown(): filters the current list, combined
 *       │                [Hide audio devices ○]       with the Device Name box (headphones / speakers / earbuds)
 *       ├─ [ Scan Bluetooth ]
 *       ├─ Discovered Devices          3 of 7
 *       │   ┌──────────────────────────────────┐
 *       │   │ Device A                 -58 dBm │
 *       │   │ Device B ◄ selected      -63 dBm │  ← tap = select · tap again = show advertising packet
 *       │   │ Device C                 -77 dBm │
 *       │   └──────────────────────────────────┘
 *       ├─ [ Connect ] [ Disconnect ]  ← scannerButtonsOf()
 *       ├─ ● Status: Connected to …    + raw lastEvent (status=133…)
 *       ├─ GattSection                 ← Day 3: service/characteristic · target ★ · Read → raw + value
 *       │                                 task 004: Write (command box + Send) · MTU
 *       └─ ReceivedDataSection         ← Day 4: one line per notify packet (max 50) + Clear
 * ```
 *
 * The screen takes the [service] directly and does its own `collectAsState` — BLE has lots of state, hoisting it all into `App()`
 * bloats it for nobody's benefit. Still strictly one way `ui/` → `core/` (the [BleClient] interface, not a platform class).
 */
@Composable
fun ScannerScreen(
    service: BleClient,
    readiness: BleReadiness,
    onOpenPermissions: () -> Unit,
) {
    // collectAsState: StateFlow → Compose State. New values arrive on the main thread before drawing,
    // so BLE callbacks on a Binder thread cause no cross-thread drawing errors.
    val devices by service.devices.collectAsState()
    val isScanning by service.isScanning.collectAsState()
    val connection by service.connection.collectAsState()
    val connectedAddress by service.connectedAddress.collectAsState()
    val lastEvent by service.lastEvent.collectAsState()
    val hiddenCount by service.hiddenCount.collectAsState()
    // Day 3 + Day 4
    val gattServices by service.services.collectAsState()
    val isDiscovering by service.isDiscovering.collectAsState()
    val lastRead by service.lastRead.collectAsState()
    val received by service.received.collectAsState()
    val gattError by service.gattError.collectAsState()
    val mtu by service.mtu.collectAsState()                    // task 004

    // Text in the Device Name box. rememberSaveable: rotating keeps what's being typed.
    var query by rememberSaveable { mutableStateOf("") }
    // Clearing focus from the Device Name box = hide the keyboard (≈ iOS `@FocusState` set to false).
    // Buttons / Switches / device rows swallow taps so the Column's pointerInput never gets them → call hideKeyboard() in onClick.
    val focusManager = LocalFocusManager.current
    val hideKeyboard = { focusManager.clearFocus() }
    val scrollState = rememberScrollState()
    // Dragging the screen drops the keyboard right away — it doesn't cover the Scan button / device list
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) hideKeyboard()
    }

    // The scan-time filter is the SCREEN's choice (the service only receives it when Scan is tapped).
    // Default OFF: Day 2 tests with nRF Connect / ESP32, whose names aren't CarMD2 / CMD-*.
    var carmdOnly by rememberSaveable { mutableStateOf(false) }
    // 2 switches that filter the LIST already seen (scanning unchanged) — default OFF so nRF / ESP32 rows don't vanish.
    var hideUnknown by rememberSaveable { mutableStateOf(false) }
    var hideAudio by rememberSaveable { mutableStateOf(false) }
    val filter = if (carmdOnly) BluetoothScanFilter.CARMD_TOOLS else BluetoothScanFilter.NONE

    // Selected row — store the address (String) so rememberSaveable can save it; storing the BluetoothDeviceInfo means
    // a stale object once RSSI changes, wrong comparisons. showDetails: tapping the selected row AGAIN = open the advertising packet.
    var selectedAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    // The screen's "clock", ticking every second → rows fade when a device goes quiet, "Last seen" counts by itself.
    var nowMs by remember { mutableLongStateOf(monotonicMs()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMs = monotonicMs()
        }
    }

    val ready = readiness == BleReadiness.READY
    // Lambda filter just like the Day 01 exercise — the rules live in isDeviceShown() (Device Name box + 2 switches), this only calls it.
    val shown = devices.filter {
        isDeviceShown(
            device = it,
            query = query,
            hideUnknown = hideUnknown,
            hideAudio = hideAudio,
            isPinned = it.address == selectedAddress || it.address == connectedAddress,
        )
    }
    val isFiltering = query.isNotBlank() || hideUnknown || hideAudio
    val selected = devices.firstOrNull { it.address == selectedAddress }
    val buttons = scannerButtonsOf(
        hasSelection = selected != null,
        selectedIsConnectable = selected?.advertisement?.isConnectable,
        selectedIsConnectedDevice = selected != null && selected.address == connectedAddress,
        connection = connection,
        ready = ready,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.surface)
            // Tap empty space = hide the keyboard
            .pointerInput(Unit) { detectTapGestures { hideKeyboard() } }
            .verticalScroll(scrollState)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        AppHintText(ScannerStrings.HINT, bullet = false)

            // Only shown when there IS a problem — says exactly what's blocking, with one button to go fix it
        if (!ready) {
            AppCard(title = ScannerStrings.NOT_READY_TITLE) {
                AppHintText(
                    when (readiness) {
                        BleReadiness.MISSING_PERMISSION -> ScannerStrings.NOT_READY_PERMISSION
                        BleReadiness.RADIO_OFF -> ScannerStrings.NOT_READY_RADIO_OFF
                        BleReadiness.UNSUPPORTED -> ScannerStrings.NOT_READY_UNSUPPORTED
                        BleReadiness.READY -> ""
                    },
                )
                if (readiness != BleReadiness.UNSUPPORTED) {
                    AppSecondaryButton(ScannerStrings.OPEN_PERMISSIONS_BUTTON, onOpenPermissions)
                }
            }
        }

        // --- Device Name ---
        SectionLabel(ScannerStrings.DEVICE_NAME_LABEL)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(ScannerStrings.SEARCH_PLACEHOLDER, color = AppColor.textSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppColor.textSecondary) },
            singleLine = true,
            // The Enter key shows a magnifier; filtering already runs per keystroke → tapping only needs to hide the keyboard
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { hideKeyboard() }),
            shape = RoundedCornerShape(AppRadius.field),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = AppColor.border,
                focusedBorderColor = AppColor.primary,
            ),
        )
        // Changing the filter while scanning has no effect until the next Scan → disable the switch while scanning
        ToggleRow(ScannerStrings.FILTER_TOGGLE, carmdOnly, enabled = !isScanning) { hideKeyboard(); carmdOnly = it }
        // Filters the list already seen → changing it while scanning takes effect immediately, no need to disable
        ToggleRow(ScannerStrings.HIDE_UNKNOWN_TOGGLE, hideUnknown) { hideKeyboard(); hideUnknown = it }
        ToggleRow(ScannerStrings.HIDE_AUDIO_TOGGLE, hideAudio) { hideKeyboard(); hideAudio = it }
        if (carmdOnly) {
            Text(ScannerStrings.filterOnFormat(filter.label), fontSize = 12.sp, color = AppColor.textSecondary)
            if (hiddenCount > 0) {
                Text(ScannerStrings.hiddenFormat(hiddenCount), fontSize = 12.sp, color = AppColor.textSecondary)
            }
        }

        AppPrimaryButton(
            text = if (isScanning) ScannerStrings.STOP_SCAN_BUTTON else ScannerStrings.SCAN_BUTTON,
            onClick = {
                hideKeyboard()
                if (isScanning) service.stopScan() else service.startScan(filter)
            },
            enabled = ready,
        )
        if (isScanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = AppSpacing.xs), strokeWidth = 2.dp)
                Text(ScannerStrings.scanningFormat(devices.size), fontSize = 13.sp, modifier = Modifier.padding(start = AppSpacing.xs))
            }
        }

        // --- Discovered Devices ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(ScannerStrings.DEVICES_TITLE, modifier = Modifier.weight(1f))
            if (isFiltering && devices.isNotEmpty()) {
                Text(ScannerStrings.matchCountFormat(shown.size, devices.size), fontSize = 12.sp, color = AppColor.textSecondary)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.field))
                .border(1.dp, AppColor.divider, RoundedCornerShape(AppRadius.field)),
        ) {
            when {
                devices.isEmpty() -> EmptyListText(ScannerStrings.NO_DEVICES)
                shown.isEmpty() -> EmptyListText(
                    if (query.isNotBlank()) ScannerStrings.noMatchFormat(query.trim()) else ScannerStrings.allHiddenFormat(devices.size),
                )
                // LazyColumn = RecyclerView: only builds visible rows (a crowded place can give 50–100 devices in 10 s).
                // heightIn(max): a LazyColumn inside a verticalScroll Column MUST have a finite max height,
                // otherwise Compose throws "infinity maximum height constraints" at runtime.
                else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    // key = address: RSSI changes constantly, without a key the list flickers + the selected row jumps around
                    itemsIndexed(shown, key = { _, d -> d.address }) { index, device ->
                        if (index > 0) HorizontalDivider(color = AppColor.divider)
                        val isThisDevice = device.address == connectedAddress
                        val isSelected = device.address == selectedAddress
                        DeviceRow(
                            device = device,
                            isSelected = isSelected,
                            isConnected = isThisDevice && connection is BluetoothConnectionState.Connected,
                            showDetails = isSelected && showDetails,
                            // Fade only WHILE scanning: after the scan the app stopped listening, the device didn't vanish.
                            // The connected row never fades — a peripheral stops advertising once connected.
                            isStale = isScanning && !isThisDevice && BluetoothLookup.isStale(device.lastSeenMs, nowMs),
                            secondsAgo = (nowMs - device.lastSeenMs) / 1_000,
                            onClick = {
                                hideKeyboard()
                                if (isSelected) {
                                    showDetails = !showDetails
                                } else {
                                    selectedAddress = device.address
                                    showDetails = false
                                }
                            },
                        )
                    }
                }
            }
        }
        if (shown.isNotEmpty()) {
            Text(ScannerStrings.TAP_TO_EXPAND, fontSize = 11.sp, color = AppColor.textSecondary)
        }

        // --- Connect / Disconnect ---
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            PanelButton(
                text = ScannerStrings.CONNECT_BUTTON,
                enabled = buttons.connectEnabled,
                loading = buttons.connecting,
                container = AppColor.contentBackground,
                content = AppColor.primary,
                borderColor = AppColor.secondary,
                modifier = Modifier.weight(1f),
                onClick = { hideKeyboard(); selected?.let { service.connect(it.address) } },
            )
            PanelButton(
                text = ScannerStrings.DISCONNECT_BUTTON,
                enabled = buttons.disconnectEnabled,
                loading = false,
                container = AppColor.disabled,
                content = AppColor.textPrimary,
                borderColor = null,
                modifier = Modifier.weight(1f),
                onClick = { hideKeyboard(); service.disconnect() },
            )
        }

        // --- Status ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(connectionColor(connection), CircleShape))
            Text(
                text = ScannerStrings.statusFormat(connectionLabel(connection)),
                fontSize = 14.sp,
                color = AppColor.textPrimary,
                modifier = Modifier.padding(start = AppSpacing.sm),
            )
        }
        // lastEvent is the only readable thing when connect fails (status=133…) → keep it, in small text
        if (lastEvent.isNotEmpty()) {
            Text(lastEvent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary)
        }

        // --- GATT (Day 3) + Received Data (Day 4) ---
        GattSection(
            isConnected = connection is BluetoothConnectionState.Connected,
            isDiscovering = isDiscovering,
            services = gattServices,
            lastRead = lastRead,
            gattError = gattError,
            mtu = mtu,
            onRead = service::read,
            onNotify = service::setNotify,
            onWrite = { c, bytes, withResponse -> hideKeyboard(); service.write(c, bytes, withResponse) },
        )
        ReceivedDataSection(received = received, onClear = service::clearReceived)
    }
}

/** Small bold label above each block ("Device Name", "Discovered Devices"…) like the mockup. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColor.textPrimary,
        modifier = modifier.padding(top = AppSpacing.xs),
    )
}

@Composable
private fun EmptyListText(text: String) {
    Text(text, fontSize = 13.sp, color = AppColor.textSecondary, modifier = Modifier.padding(AppSpacing.md))
}

/**
 * One row in the list: name on the left, dBm on the right (as in the mockup). Selected row = light-blue background + dark-blue text.
 * With [showDetails] on, the advertising packet details expand below (kept from the nRF version).
 */
@Composable
private fun DeviceRow(
    device: BluetoothDeviceInfo,
    isSelected: Boolean,
    isConnected: Boolean,
    showDetails: Boolean,
    isStale: Boolean,
    secondsAgo: Long,
    onClick: () -> Unit,
) {
    val adv = device.advertisement
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isStale) 0.4f else 1f)
            .background(if (isSelected) AppColor.contentBackground else AppColor.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name ?: ScannerStrings.UNKNOWN_DEVICE,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AppColor.primary else AppColor.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isConnected) {
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
                Text(device.address, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary)
            }
            SignalBars(device.rssi)
            Text(
                ScannerStrings.rssiFormat(device.rssi),
                fontSize = 12.sp,
                color = AppColor.textSecondary,
                modifier = Modifier.padding(start = AppSpacing.xs),
            )
        }

        if (showDetails && adv != null) {
            val lines = buildList {
                add(ScannerStrings.connectableFormat(adv.isConnectable))
                add(ScannerStrings.txPowerFormat(adv.txPower))
                add(ScannerStrings.intervalFormat(adv.intervalMs, adv.packetCount))
                add(ScannerStrings.lastSeenFormat(secondsAgo))
                add(ScannerStrings.deviceTypeFormat(adv.deviceType))
                add(ScannerStrings.addressTypeFormat(adv.addressType))
                add(ScannerStrings.bondFormat(adv.bondState))
                add(ScannerStrings.flagsFormat(adv.flags))
                add(ScannerStrings.servicesFormat(adv.serviceUuids, BluetoothLookup::serviceName))
                adv.manufacturerData.forEach { add(ScannerStrings.manufacturerFormat(it)) }
                adv.serviceData.forEach { add(ScannerStrings.serviceDataFormat(it)) }
                add(ScannerStrings.rawFormat(adv.rawHex))
            }
            Column(modifier = Modifier.padding(top = AppSpacing.sm)) {
                lines.forEach {
                    Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AppColor.textSecondary)
                }
            }
        }
    }
}

/**
 * Half-width button for the Connect / Disconnect pair. Not `AppPrimaryButton` because that one is `fillMaxWidth` +
 * solid blue; the mockup needs 2 buttons side by side, light background. Only this screen uses it so it's private.
 */
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
    // Surface(onClick) = Material3's "bare" button: handles shape + background + border + touch area + ripple in the right order.
    // The earlier hand-assembled clip/background/border/alpha/clickable lost its background + border when enabled on a Samsung A32.
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(AppRadius.field),
        color = container,
        border = borderColor?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            modifier = Modifier.alpha(if (enabled || loading) 1f else 0.45f),   // dim the TEXT when disabled, background stays
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

/**
 * 3 small signal bars next to the dBm number. "Off" bars are still drawn (pale) so the eye can compare 1/3 with 3/3.
 * Colors: strong green · medium orange · weak red — thresholds live in [BleSignal.of], not here.
 */
@Composable
private fun SignalBars(rssi: Int) {
    val signal = BleSignal.of(rssi)
    val color = when (signal) {
        BleSignal.STRONG -> AppColor.success
        BleSignal.MEDIUM -> AppColor.warning
        BleSignal.WEAK -> AppColor.danger
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(5.dp, 8.dp, 11.dp).forEachIndexed { i, h ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(h)
                    .background(if (i < signal.bars) color else AppColor.disabled, RoundedCornerShape(1.dp)),
            )
        }
    }
}

/** One label row + Switch. */
@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),   // loosen the switch rows a bit so they're easier to tap
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = AppColor.textSecondary)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

private fun connectionLabel(state: BluetoothConnectionState): String = when (state) {
    BluetoothConnectionState.Idle -> ScannerStrings.STATE_IDLE
    BluetoothConnectionState.Connecting -> ScannerStrings.STATE_CONNECTING
    is BluetoothConnectionState.Connected -> ScannerStrings.stateConnectedFormat(state.name)
    BluetoothConnectionState.Disconnected -> ScannerStrings.STATE_DISCONNECTED
    is BluetoothConnectionState.Error -> state.message
}

/** Color of the dot before "Status:". */
private fun connectionColor(state: BluetoothConnectionState) = when (state) {
    is BluetoothConnectionState.Connected -> AppColor.success
    is BluetoothConnectionState.Error -> AppColor.danger       // signal lost / radio off: red + status code
    BluetoothConnectionState.Connecting -> AppColor.warning
    else -> AppColor.textSecondary                               // Idle / Disconnected by us → grey
}
