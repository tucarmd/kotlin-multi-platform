package com.learn.ndmkmp.presentation.scanner

import com.learn.ndmkmp.clockText
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.bluetooth.BleSignal
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDecoder
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import com.learn.ndmkmp.core.bluetooth.BluetoothLookup
import com.learn.ndmkmp.core.bluetooth.BluetoothScanFilter
import com.learn.ndmkmp.core.bluetooth.GattCharacteristicInfo
import com.learn.ndmkmp.core.bluetooth.RECEIVED_LOG_MAX
import com.learn.ndmkmp.core.bluetooth.WritePlan
import com.learn.ndmkmp.core.bluetooth.maxPayloadOf
import com.learn.ndmkmp.core.bluetooth.notifyTargetOf
import com.learn.ndmkmp.core.bluetooth.planWrite
import com.learn.ndmkmp.core.bluetooth.readTargetOf
import com.learn.ndmkmp.core.bluetooth.writeTargetOf
import com.learn.ndmkmp.monotonicMs
import com.learn.ndmkmp.presentation.SharedViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Colour of the dot before "Status:" — each UI maps it to its own palette. */
enum class StatusTone { NEUTRAL, CONNECTING, CONNECTED, ERROR }

/** One row of Discovered Devices. */
data class DeviceRowUi(
    val address: String,
    val name: String,
    val isSelected: Boolean,
    val isConnected: Boolean,
    /** Not heard for 5 s while scanning → drawn faded. */
    val isStale: Boolean,
    val signal: BleSignal,
    val rssiText: String,
    /** Advertising packet lines, only when the selected row was tapped again. Empty = collapsed. */
    val detailLines: List<String>,
)

/** One characteristic under a service, with which buttons it gets. */
data class GattCharacteristicUi(
    val info: GattCharacteristicInfo,
    val label: String,
    val propertiesText: String,
    val isTarget: Boolean,
    val canRead: Boolean,
    val canNotify: Boolean,
    val isNotifying: Boolean,
    val canWrite: Boolean,
    /** The current write target → its [Write] button is drawn active. */
    val isWriteTarget: Boolean,
) {
    val notifyButtonText: String get() = if (isNotifying) ScannerStrings.STOP_NOTIFY_BUTTON else ScannerStrings.NOTIFY_BUTTON
}

data class GattServiceUi(val title: String, val characteristics: List<GattCharacteristicUi>)

/** Task 004 — the Write block for the current write target. */
data class WriteUi(
    val toText: String,
    val input: String,
    val asHex: Boolean,
    val placeholder: String,
    /** "→ 41 54 5A 0D (4 bytes)" or the reason Send is disabled. */
    val previewText: String,
    /** Red only for a real problem — an empty box is just grey. */
    val previewIsError: Boolean,
    val canSend: Boolean,
) {
    val showEscapeHint: Boolean get() = !asHex
}

data class ScannerUiState(
    val readiness: BleReadiness = BleReadiness.MISSING_PERMISSION,
    /** Why we cannot scan, null = ready. */
    val notReadyText: String? = null,
    val showOpenPermissions: Boolean = false,

    val query: String = "",
    val carmdOnly: Boolean = false,
    val hideUnknown: Boolean = false,
    val hideAudio: Boolean = false,
    /** "Showing only CarMD2 · CMD-*" + "N other devices hidden" when the scan filter is on. */
    val filterLines: List<String> = emptyList(),

    val isScanning: Boolean = false,
    val scanButtonText: String = ScannerStrings.SCAN_BUTTON,
    /** "Scanning… 3 found", null = not scanning. */
    val scanningText: String? = null,

    /** "3 of 7" while a filter is active. */
    val matchCountText: String? = null,
    /** Message instead of the list (no devices / no match / all hidden), null = show [rows]. */
    val emptyListText: String? = ScannerStrings.NO_DEVICES,
    val rows: List<DeviceRowUi> = emptyList(),

    val buttons: ScannerButtons = ScannerButtons(connectEnabled = false, connecting = false, disconnectEnabled = false),
    val statusText: String = ScannerStrings.statusFormat(ScannerStrings.STATE_IDLE),
    val statusTone: StatusTone = StatusTone.NEUTRAL,
    /** Raw last event (status=133…) — the only readable thing when connect fails. */
    val lastEvent: String? = null,

    val gattCountText: String? = null,
    val isDiscovering: Boolean = false,
    /** Not connected / discovering / no services, null = show [services]. */
    val gattMessage: String? = ScannerStrings.GATT_NOT_CONNECTED,
    /** Read target · Notify target · Write target · MTU. */
    val targetLines: List<String> = emptyList(),
    val services: List<GattServiceUi> = emptyList(),
    val write: WriteUi? = null,
    /** "Last read: …" + raw + value, empty = nothing read yet. */
    val lastReadLines: List<String> = emptyList(),
    val gattError: String? = null,

    val receivedLines: List<String> = emptyList(),
    val receivedCountText: String? = null,
) {
    val ready: Boolean get() = readiness == BleReadiness.READY
    val scanEnabled: Boolean get() = ready
    /** Changing the scan filter mid-scan does nothing until the next Scan → locked while scanning. */
    val carmdToggleEnabled: Boolean get() = !isScanning
    val showTapHint: Boolean get() = rows.isNotEmpty()
}

/** What only the screen knows (typing, switches, selection) — the BLE client never sees it. */
private data class LocalInput(
    val query: String = "",
    val carmdOnly: Boolean = false,
    val hideUnknown: Boolean = false,
    val hideAudio: Boolean = false,
    val selectedAddress: String? = null,
    val showDetails: Boolean = false,
    val writeKey: String? = null,
    val writeInput: String = "",
    val asHex: Boolean = false,
    /** The screen's clock — ticks every second while scanning so rows fade by themselves. */
    val nowMs: Long = 0,
)

/**
 * Day 2 → Day 4 + task 004 — Scanner tab. Inputs: every [BleClient] flow + [readiness] (Permissions tab) + [LocalInput].
 * Any of them changes → [rebuild] the whole [ScannerUiState] from current values. The rules themselves live in
 * `ScannerActions.kt` / `GattModel.kt` (unit tested); this class only wires them together.
 */
class ScannerViewModel(
    private val ble: BleClient,
    private val readiness: StateFlow<BleReadiness>,
    /** Monotonic clock — a parameter so tests can run without Android's `SystemClock`. */
    private val now: () -> Long = ::monotonicMs,
) : SharedViewModel() {

    private val input = MutableStateFlow(LocalInput(nowMs = now()))
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    /** Kept for [send] — the bytes Send would write right now. */
    private var writePlan: WritePlan? = null
    private var target: GattCharacteristicInfo? = null
    private var clock: Job? = null

    init {
        merge(
            ble.devices.map { }, ble.isScanning.map { }, ble.connection.map { }, ble.connectedAddress.map { },
            ble.lastEvent.map { }, ble.hiddenCount.map { }, ble.services.map { }, ble.isDiscovering.map { },
            ble.lastRead.map { }, ble.received.map { }, ble.gattError.map { }, ble.mtu.map { },
            readiness.map { }, input.map { },
        ).onEach { rebuild() }.launchIn(scope)
        ble.isScanning.onEach { scanning -> if (scanning) startClock() else stopClock() }.launchIn(scope)
    }

    // --- actions ---------------------------------------------------------------------------------------------

    fun onQueryChange(text: String) = input.update { it.copy(query = text) }
    fun setCarmdOnly(on: Boolean) = input.update { it.copy(carmdOnly = on) }
    fun setHideUnknown(on: Boolean) = input.update { it.copy(hideUnknown = on) }
    fun setHideAudio(on: Boolean) = input.update { it.copy(hideAudio = on) }

    fun onScanClick() {
        if (ble.isScanning.value) ble.stopScan()
        else ble.startScan(if (input.value.carmdOnly) BluetoothScanFilter.CARMD_TOOLS else BluetoothScanFilter.NONE)
    }

    /** Tap = select · tap the selected row again = show / hide its advertising packet. */
    fun onDeviceClick(address: String) = input.update {
        if (it.selectedAddress == address) it.copy(showDetails = !it.showDetails)
        else it.copy(selectedAddress = address, showDetails = false)
    }

    fun connect() {
        input.value.selectedAddress?.let(ble::connect)
    }

    fun disconnect() = ble.disconnect()
    fun read(c: GattCharacteristicInfo) = ble.read(c)
    fun setNotify(c: GattCharacteristicInfo, enabled: Boolean) = ble.setNotify(c, enabled)
    fun clearReceived() = ble.clearReceived()

    /** [Write] on a row only SELECTS the target — the content is typed in the Write block. */
    fun selectWriteTarget(c: GattCharacteristicInfo) = input.update { it.copy(writeKey = c.key) }
    fun onWriteInputChange(text: String) = input.update { it.copy(writeInput = text) }
    fun setWriteHex(hex: Boolean) = input.update { it.copy(asHex = hex) }

    /** Preset button (ATZ\r…) = fill the box as text, not sent yet. */
    fun applyWritePreset(preset: String) = input.update { it.copy(asHex = false, writeInput = preset) }

    fun send() {
        val plan = writePlan as? WritePlan.Ok ?: return
        target?.let { ble.write(it, plan.bytes, plan.withResponse) }
    }

    // --- state ------------------------------------------------------------------------------------------------

    private fun startClock() {
        if (clock?.isActive == true) return
        clock = scope.launch {
            while (isActive) {
                delay(1_000)
                input.update { it.copy(nowMs = now()) }
            }
        }
    }

    private fun stopClock() {
        clock?.cancel()
        clock = null
    }

    private fun rebuild() {
        val i = input.value
        val readiness = readiness.value
        val devices = ble.devices.value
        val isScanning = ble.isScanning.value
        val connection = ble.connection.value
        val connectedAddress = ble.connectedAddress.value
        val nowMs = if (isScanning) i.nowMs else now()

        val shown = devices.filter {
            isDeviceShown(it, i.query, i.hideUnknown, i.hideAudio, isPinned = it.address == i.selectedAddress || it.address == connectedAddress)
        }
        val isFiltering = i.query.isNotBlank() || i.hideUnknown || i.hideAudio
        val selected = devices.firstOrNull { it.address == i.selectedAddress }

        _state.value = ScannerUiState(
            readiness = readiness,
            notReadyText = when (readiness) {
                BleReadiness.READY -> null
                BleReadiness.MISSING_PERMISSION -> ScannerStrings.NOT_READY_PERMISSION
                BleReadiness.RADIO_OFF -> ScannerStrings.NOT_READY_RADIO_OFF
                BleReadiness.UNSUPPORTED -> ScannerStrings.NOT_READY_UNSUPPORTED
            },
            showOpenPermissions = readiness == BleReadiness.MISSING_PERMISSION || readiness == BleReadiness.RADIO_OFF,
            query = i.query,
            carmdOnly = i.carmdOnly,
            hideUnknown = i.hideUnknown,
            hideAudio = i.hideAudio,
            filterLines = if (!i.carmdOnly) emptyList() else buildList {
                add(ScannerStrings.filterOnFormat(BluetoothScanFilter.CARMD_TOOLS.label))
                ble.hiddenCount.value.takeIf { it > 0 }?.let { add(ScannerStrings.hiddenFormat(it)) }
            },
            isScanning = isScanning,
            scanButtonText = if (isScanning) ScannerStrings.STOP_SCAN_BUTTON else ScannerStrings.SCAN_BUTTON,
            scanningText = if (isScanning) ScannerStrings.scanningFormat(devices.size) else null,
            matchCountText = if (isFiltering && devices.isNotEmpty()) ScannerStrings.matchCountFormat(shown.size, devices.size) else null,
            emptyListText = when {
                devices.isEmpty() -> ScannerStrings.NO_DEVICES
                shown.isEmpty() && i.query.isNotBlank() -> ScannerStrings.noMatchFormat(i.query.trim())
                shown.isEmpty() -> ScannerStrings.allHiddenFormat(devices.size)
                else -> null
            },
            rows = shown.map { row(it, i, isScanning, connection, connectedAddress, nowMs) },
            buttons = scannerButtonsOf(
                hasSelection = selected != null,
                selectedIsConnectable = selected?.advertisement?.isConnectable,
                selectedIsConnectedDevice = selected != null && selected.address == connectedAddress,
                connection = connection,
                ready = readiness == BleReadiness.READY,
            ),
            statusText = ScannerStrings.statusFormat(connectionLabel(connection)),
            statusTone = when (connection) {
                is BluetoothConnectionState.Connected -> StatusTone.CONNECTED
                is BluetoothConnectionState.Error -> StatusTone.ERROR
                BluetoothConnectionState.Connecting -> StatusTone.CONNECTING
                else -> StatusTone.NEUTRAL
            },
            lastEvent = ble.lastEvent.value.takeIf { it.isNotEmpty() },
        ).withGatt(i, connection is BluetoothConnectionState.Connected)
    }

    private fun row(
        d: BluetoothDeviceInfo,
        i: LocalInput,
        isScanning: Boolean,
        connection: BluetoothConnectionState,
        connectedAddress: String?,
        nowMs: Long,
    ): DeviceRowUi {
        val isThisDevice = d.address == connectedAddress
        val isSelected = d.address == i.selectedAddress
        val adv = d.advertisement
        return DeviceRowUi(
            address = d.address,
            name = d.name ?: ScannerStrings.UNKNOWN_DEVICE,
            isSelected = isSelected,
            isConnected = isThisDevice && connection is BluetoothConnectionState.Connected,
            // Fade only WHILE scanning (after it the app stopped listening, the device didn't vanish);
            // the connected row never fades — a peripheral stops advertising once connected.
            isStale = isScanning && !isThisDevice && BluetoothLookup.isStale(d.lastSeenMs, nowMs),
            signal = BleSignal.of(d.rssi),
            rssiText = ScannerStrings.rssiFormat(d.rssi),
            detailLines = if (!isSelected || !i.showDetails || adv == null) emptyList() else buildList {
                add(ScannerStrings.connectableFormat(adv.isConnectable))
                add(ScannerStrings.txPowerFormat(adv.txPower))
                add(ScannerStrings.intervalFormat(adv.intervalMs, adv.packetCount))
                add(ScannerStrings.lastSeenFormat((nowMs - d.lastSeenMs) / 1_000))
                add(ScannerStrings.deviceTypeFormat(adv.deviceType))
                add(ScannerStrings.addressTypeFormat(adv.addressType))
                add(ScannerStrings.bondFormat(adv.bondState))
                add(ScannerStrings.flagsFormat(adv.flags))
                add(ScannerStrings.servicesFormat(adv.serviceUuids, BluetoothLookup::serviceName))
                adv.manufacturerData.forEach { add(ScannerStrings.manufacturerFormat(it)) }
                adv.serviceData.forEach { add(ScannerStrings.serviceDataFormat(it)) }
                add(ScannerStrings.rawFormat(adv.rawHex))
            },
        )
    }

    /** Day 3 + Day 4 + task 004: GATT tree, targets, Write block, last read, Received Data. */
    private fun ScannerUiState.withGatt(i: LocalInput, isConnected: Boolean): ScannerUiState {
        val services = ble.services.value
        val mtu = ble.mtu.value
        val readTarget = readTargetOf(services)
        val notifyTarget = notifyTargetOf(services)
        // Write target: the row whose [Write] was tapped; none / that key gone on another device → auto-pick.
        val writeTarget = services.flatMap { it.characteristics }.firstOrNull { it.key == i.writeKey && it.properties.canWrite }
            ?: writeTargetOf(services)
        val targetKeys = setOfNotNull(readTarget?.key, notifyTarget?.key, writeTarget?.key)
        val isDiscovering = ble.isDiscovering.value

        target = writeTarget
        writePlan = writeTarget?.let { planWrite(i.writeInput, i.asHex, it.properties, maxPayloadOf(mtu)) }
        val lastRead = ble.lastRead.value
        val received = ble.received.value

        return copy(
            gattCountText = services.takeIf { it.isNotEmpty() }
                ?.let { ScannerStrings.servicesCountFormat(it.size, it.sumOf { s -> s.characteristics.size }) },
            isDiscovering = isDiscovering,
            gattMessage = when {
                isDiscovering -> ScannerStrings.GATT_DISCOVERING
                !isConnected && services.isEmpty() -> ScannerStrings.GATT_NOT_CONNECTED
                services.isEmpty() -> ScannerStrings.GATT_EMPTY
                else -> null
            },
            targetLines = if (services.isEmpty() || isDiscovering) emptyList() else listOf(
                ScannerStrings.readTargetFormat(readTarget?.let(::characteristicLabel)),
                ScannerStrings.notifyTargetFormat(notifyTarget?.let(::characteristicLabel)),
                ScannerStrings.writeTargetFormat(writeTarget?.let(::characteristicLabel)),
                ScannerStrings.mtuFormat(mtu, maxPayloadOf(mtu)),
            ),
            services = if (isDiscovering) emptyList() else services.map { s ->
                GattServiceUi(
                    title = ScannerStrings.uuidWithName(s.uuid, BluetoothLookup.serviceName(s.uuid)),
                    characteristics = s.characteristics.map { c ->
                        GattCharacteristicUi(
                            info = c,
                            label = characteristicLabel(c),
                            propertiesText = c.properties.labels.joinToString(" "),
                            isTarget = c.key in targetKeys,
                            canRead = c.properties.read,
                            canNotify = c.properties.canSubscribe,
                            isNotifying = c.isNotifying,
                            canWrite = c.properties.canWrite,
                            isWriteTarget = c.key == writeTarget?.key,
                        )
                    },
                )
            },
            write = if (isDiscovering) null else writeTarget?.let { t ->
                val plan = writePlan
                WriteUi(
                    toText = ScannerStrings.writeToFormat(characteristicLabel(t), t.properties.write),
                    input = i.writeInput,
                    asHex = i.asHex,
                    placeholder = if (i.asHex) ScannerStrings.WRITE_PLACEHOLDER_HEX else ScannerStrings.WRITE_PLACEHOLDER_TEXT,
                    previewText = when (plan) {
                        is WritePlan.Ok -> ScannerStrings.writePreviewFormat(BluetoothDecoder.toHex(plan.bytes), plan.bytes.size)
                        is WritePlan.Invalid -> plan.reason
                        null -> ""
                    },
                    previewIsError = plan is WritePlan.Invalid && i.writeInput.isNotEmpty(),
                    canSend = plan is WritePlan.Ok,
                )
            },
            lastReadLines = lastRead?.let {
                listOf(
                    ScannerStrings.lastReadTitleFormat(ScannerStrings.uuidWithName(it.uuid, BluetoothLookup.characteristicName(it.uuid))),
                    ScannerStrings.rawFormatLine(it.hex),
                    ScannerStrings.decodedFormatLine(it.decoded),
                )
            }.orEmpty(),
            gattError = ble.gattError.value,
            receivedLines = received.map {
                ScannerStrings.receivedLineFormat(clockText(it.atMs), it.source.name, it.uuid, it.hex, it.decoded)
            },
            receivedCountText = received.takeIf { it.isNotEmpty() }?.let { ScannerStrings.receivedCountFormat(it.size, RECEIVED_LOG_MAX) },
        )
    }

    override fun clear() {
        stopClock()
        super.clear()
    }
}

private fun characteristicLabel(c: GattCharacteristicInfo) =
    ScannerStrings.uuidWithName(c.uuid, BluetoothLookup.characteristicName(c.uuid))

private fun connectionLabel(state: BluetoothConnectionState): String = when (state) {
    BluetoothConnectionState.Idle -> ScannerStrings.STATE_IDLE
    BluetoothConnectionState.Connecting -> ScannerStrings.STATE_CONNECTING
    is BluetoothConnectionState.Connected -> ScannerStrings.stateConnectedFormat(state.name)
    BluetoothConnectionState.Disconnected -> ScannerStrings.STATE_DISCONNECTED
    is BluetoothConnectionState.Error -> state.message
}
