package com.learn.ndmkmp.platform

import com.learn.ndmkmp.core.bluetooth.ATT_DEFAULT_MTU
import com.learn.ndmkmp.core.bluetooth.ATT_HEADER_BYTES
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.bluetooth.BluetoothAdvertisement
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDecoder
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import com.learn.ndmkmp.core.bluetooth.BluetoothLookup
import com.learn.ndmkmp.core.bluetooth.BluetoothScanFilter
import com.learn.ndmkmp.core.bluetooth.GattCharacteristicInfo
import com.learn.ndmkmp.core.bluetooth.GattServiceInfo
import com.learn.ndmkmp.core.bluetooth.GattValue
import com.learn.ndmkmp.core.bluetooth.GattValueSource
import com.learn.ndmkmp.core.bluetooth.appendCapped
import com.learn.ndmkmp.monotonicMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import kotlin.time.Clock

/**
 * P12d — the thin Swift wrapper around CoreBluetooth (`iosApp/.../Platform/BleNative.swift`). Commands only; everything
 * that happens comes back through [IosBleEvents]. All calls and events on the main thread.
 * Devices are identified by `CBPeripheral.identifier.uuidString` (iOS has no MAC).
 */
interface IosBleNative {
    fun setEvents(events: IosBleEvents)
    /** Create the `CBCentralManager` if Bluetooth is already granted (creating it earlier shows the permission dialog). */
    fun prepare()
    /** `scanForPeripherals(withServices: nil, AllowDuplicates: true)`; only valid when the radio is powered on. */
    fun startScan()
    fun stopScan()
    /** false = no CBPeripheral kept for [id] (seen before this scan / list cleared). */
    fun connect(id: String): Boolean
    /** Disconnect / give up connecting. */
    fun cancelConnection(id: String)
    fun read(serviceUuid: String, uuid: String)
    fun setNotify(serviceUuid: String, uuid: String, enabled: Boolean)
    fun write(serviceUuid: String, uuid: String, data: NSData, withResponse: Boolean)
    /** `maximumWriteValueLength(for: .withoutResponse)` of the connected peripheral = MTU − 3; 0 = not connected. */
    fun maxWriteWithoutResponse(): Int
}

/** What the Swift wrapper reports. Implemented by [IosBleClient]; Swift calls it from the CoreBluetooth delegates. */
interface IosBleEvents {
    fun onPoweredOn(on: Boolean)

    /** One advertising packet. [connectable] / [txPower]: [UNKNOWN] when the packet doesn't say. */
    fun onDiscovered(
        id: String,
        name: String?,
        rssi: Int,
        connectable: Int,
        txPower: Int,
        serviceUuids: List<String>,
        manufacturerData: NSData?,
        serviceDataUuids: List<String>,
        serviceDataValues: List<NSData>,
    )

    fun onConnected(id: String, name: String)
    fun onFailedToConnect(id: String, message: String)
    /** [message] null = we asked for it, otherwise the CoreBluetooth error (signal lost…). */
    fun onDisconnected(id: String, message: String?)
    /** Built service by service while characteristics arrive; [done] = all services answered. */
    fun onServices(id: String, services: List<GattServiceInfo>, done: Boolean)
    fun onGattError(id: String, message: String)
    /** Read result AND notification — CoreBluetooth uses the same callback, [IosBleClient] tells them apart. */
    fun onValue(id: String, serviceUuid: String, uuid: String, data: NSData?, error: String?)
    fun onNotifyState(id: String, serviceUuid: String, uuid: String, isNotifying: Boolean, error: String?)
    /** Only for `.withResponse` writes: error null = the peripheral ACKed. */
    fun onWritten(id: String, serviceUuid: String, uuid: String, error: String?)

    companion object {
        const val UNKNOWN = Int.MIN_VALUE
    }
}

/**
 * P12d — [BleClient] for iOS. Holds the state + rules of the old app's `BluetoothClientService.swift` (dedupe, filter,
 * 10 s scan, 15 s connect timeout, read vs notify, cleanup), in Kotlin; CoreBluetooth calls stay in Swift.
 *
 * ```
 *  startScan ─► native.startScan + 10 s timer ─► onDiscovered ─► filter → dedupe (first-seen order) → devices
 *  connect   ─► native.connect + 15 s timer (iOS never gives up) ─► onConnected → Connected · mtu
 *            ─► onServices (Swift discovers services → characteristics)  ─► services · isDiscovering
 *  read      ─► pendingReads += key ─► onValue: key in pendingReads → READ (lastRead) else NOTIFY ─► received (50)
 *  write     ─► withResponse: keep bytes until onWritten · without: logged right away (no callback exists)
 *  radio off ─► iOS sends NO didDisconnect → onPoweredOn(false) clears everything ourselves
 * ```
 */
class IosBleClient(private val native: IosBleNative) : BleClient, IosBleEvents {

    override val devices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val isScanning = MutableStateFlow(false)
    override val connection = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    override val connectedAddress = MutableStateFlow<String?>(null)
    override val lastEvent = MutableStateFlow("")
    override val hiddenCount = MutableStateFlow(0)
    override val services = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    override val isDiscovering = MutableStateFlow(false)
    override val lastRead = MutableStateFlow<GattValue?>(null)
    override val received = MutableStateFlow<List<GattValue>>(emptyList())
    override val gattError = MutableStateFlow<String?>(null)
    override val mtu = MutableStateFlow(ATT_DEFAULT_MTU)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var poweredOn = false
    private var filter = BluetoothScanFilter.NONE
    /** Scan tapped before the manager reported .poweredOn (created lazily) → start it on [onPoweredOn]. */
    private var pendingScan: BluetoothScanFilter? = null
    /** LinkedHashMap = dedupe + first-seen order (sorting by RSSI makes rows jump under your finger). */
    private val found = LinkedHashMap<String, BluetoothDeviceInfo>()
    private val hidden = HashSet<String>()
    /** id → (packets, first packet time) to estimate the advertising interval like nRF. */
    private val packets = HashMap<String, Pair<Int, Long>>()
    private var scanTimer: Job? = null
    private var connectTimer: Job? = null
    private var userRequestedDisconnect = false
    private val pendingReads = HashSet<String>()
    private val pendingWrites = HashMap<String, ByteArray>()

    init {
        native.setEvents(this)
        native.prepare()
    }

    // --- BleClient: commands ------------------------------------------------------------------------------

    override fun startScan(filter: BluetoothScanFilter) {
        // Trap: scanning before .poweredOn returns nothing and only logs "API MISUSE".
        if (!poweredOn) {
            pendingScan = filter                       // manager may just be starting → scan once it reports on
            lastEvent.value = "Waiting for Bluetooth…"
            native.prepare()
            return
        }
        if (isScanning.value) return
        found.clear()
        hidden.clear()
        packets.clear()
        hiddenCount.value = 0
        // keep the connected device in the list — the Disconnect button needs its row
        devices.value = emptyList()
        this.filter = filter
        isScanning.value = true
        lastEvent.value = if (filter == BluetoothScanFilter.NONE) "Scanning…" else "Scanning (only ${filter.label})…"
        native.startScan()
        scanTimer?.cancel()
        scanTimer = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            stopScan()
        }
    }

    override fun stopScan() {
        scanTimer?.cancel()
        scanTimer = null
        if (!isScanning.value) return
        isScanning.value = false
        native.stopScan()
        lastEvent.value = "Scan finished: ${found.size} devices"
    }

    override fun connect(address: String) {
        stopScan()                                     // scanning while connecting = slow + flaky
        connectedAddress.value?.takeIf { it != address }?.let(native::cancelConnection)   // one connection at a time
        clearGattState()
        lastRead.value = null
        received.value = emptyList()
        userRequestedDisconnect = false
        if (!native.connect(address)) {
            connection.value = BluetoothConnectionState.Error("Device is gone — scan again")
            return
        }
        connectedAddress.value = address
        connection.value = BluetoothConnectionState.Connecting
        lastEvent.value = "Connecting to ${found[address]?.name ?: address}…"
        // Trap: iOS waits forever → give up ourselves. Checks the id: a newer connect must not be cancelled.
        connectTimer?.cancel()
        connectTimer = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (connection.value == BluetoothConnectionState.Connecting && connectedAddress.value == address) {
                native.cancelConnection(address)
                connection.value = BluetoothConnectionState.Error("Connect timed out after ${CONNECT_TIMEOUT_MS / 1000}s")
            }
        }
    }

    override fun disconnect() {
        val id = connectedAddress.value ?: return
        // Unsubscribe first — CoreBluetooth queues the CCCD writes before the disconnect.
        val subscribed = services.value.flatMap { it.characteristics }.filter { it.isNotifying }
        subscribed.forEach { native.setNotify(it.serviceUuid, it.uuid, false) }
        pendingReads.clear()
        userRequestedDisconnect = true                 // BEFORE: the callback may come immediately
        lastEvent.value = if (subscribed.isEmpty()) "Disconnecting…" else "Unsubscribed ${subscribed.size} · disconnecting…"
        native.cancelConnection(id)
    }

    override fun read(characteristic: GattCharacteristicInfo) {
        if (connection.value !is BluetoothConnectionState.Connected) {
            gattError.value = "Not connected"
            return
        }
        gattError.value = null
        pendingReads += characteristic.key
        native.read(characteristic.serviceUuid, characteristic.uuid)
    }

    /** iOS writes the CCCD `2902` itself (Android: enable locally + write the descriptor). */
    override fun setNotify(characteristic: GattCharacteristicInfo, enabled: Boolean) {
        if (connection.value !is BluetoothConnectionState.Connected) {
            gattError.value = "Not connected"
            return
        }
        gattError.value = null
        native.setNotify(characteristic.serviceUuid, characteristic.uuid, enabled)
    }

    override fun write(characteristic: GattCharacteristicInfo, bytes: ByteArray, withResponse: Boolean) {
        if (connection.value !is BluetoothConnectionState.Connected) {
            gattError.value = "Not connected"
            return
        }
        gattError.value = null
        if (withResponse) pendingWrites[characteristic.key] = bytes          // logged when the ACK arrives
        native.write(characteristic.serviceUuid, characteristic.uuid, bytes.toNSData(), withResponse)
        if (!withResponse) logWrite(characteristic.serviceUuid, characteristic.uuid, bytes)   // no callback for these
    }

    override fun clearReceived() {
        received.value = emptyList()
    }

    /** The Swift side reports the radio itself ([onPoweredOn]) — nothing extra to do here. */
    override fun onRadioOff() = Unit

    override fun release() {
        stopScan()
        disconnect()
    }

    // --- IosBleEvents: what Swift reports ------------------------------------------------------------------

    override fun onPoweredOn(on: Boolean) {
        poweredOn = on
        if (on) {
            pendingScan?.let { pendingScan = null; startScan(it) }
            return
        }
        pendingScan = null
        // iOS does NOT call didDisconnect when the radio goes off → clean up ourselves, or the UI stays "Connected".
        val hadSomething = isScanning.value || connectedAddress.value != null
        scanTimer?.cancel()
        connectTimer?.cancel()
        isScanning.value = false
        found.clear()
        hidden.clear()
        hiddenCount.value = 0
        devices.value = emptyList()
        connectedAddress.value = null
        clearGattState()
        if (hadSomething) {
            connection.value = BluetoothConnectionState.Error("Bluetooth turned off")
            lastEvent.value = "Bluetooth turned off — list cleared"
        }
    }

    override fun onDiscovered(
        id: String,
        name: String?,
        rssi: Int,
        connectable: Int,
        txPower: Int,
        serviceUuids: List<String>,
        manufacturerData: NSData?,
        serviceDataUuids: List<String>,
        serviceDataValues: List<NSData>,
    ) {
        if (!isScanning.value) return
        if (!filter.matches(name)) {
            if (hidden.add(id)) hiddenCount.value = hidden.size
            return
        }
        val now = monotonicMs()
        val (count0, first) = packets[id] ?: (0 to now)
        val count = count0 + 1
        packets[id] = count to first
        val mfr = manufacturerData?.toByteArray()
        val company = mfr?.takeIf { it.size >= 2 }?.let { (it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8) }
        val adv = BluetoothAdvertisement(
            isConnectable = connectable.takeIf { it != IosBleEvents.UNKNOWN }?.let { it != 0 },
            txPower = txPower.takeIf { it != IosBleEvents.UNKNOWN },
            bondState = "n/a (iOS)",
            flags = null,                                 // iOS doesn't expose the flags byte
            serviceUuids = serviceUuids,
            manufacturerData = if (mfr == null || company == null) emptyList() else listOf(
                "0x" + company.toString(16).uppercase().padStart(4, '0') +
                    (BluetoothLookup.companyName(company)?.let { " $it" } ?: "") + ": " + BluetoothDecoder.toHex(mfr.copyOfRange(2, mfr.size)),
            ),
            serviceData = serviceDataUuids.zip(serviceDataValues).map { (u, d) -> "$u: ${BluetoothDecoder.toHex(d.toByteArray())}" }.sorted(),
            rawHex = "",                                  // iOS gives the parsed dictionary, not the raw packet
            packetCount = count,
            intervalMs = if (count > 1) (now - first) / (count - 1) else null,
            deviceType = "LE (iOS)",
            serviceDataUuids = serviceDataUuids.sorted(),
            // iOS keeps the 2 company-id bytes at the front → the Apple type byte is index 2
            appleType = if (company == BluetoothLookup.APPLE_COMPANY_ID && mfr.size >= 3) mfr[2].toInt() and 0xFF else null,
        )
        found[id] = BluetoothDeviceInfo(address = id, name = name, rssi = rssi, advertisement = adv, lastSeenMs = now)
        devices.value = found.values.toList()
    }

    override fun onConnected(id: String, name: String) {
        if (id != connectedAddress.value) {            // the old device connected after being replaced → drop it
            native.cancelConnection(id)
            return
        }
        connectTimer?.cancel()
        connection.value = BluetoothConnectionState.Connected(name)
        lastEvent.value = "Connected to $name"
        refreshMtu()
        isDiscovering.value = true                     // Swift starts discovery right after didConnect
        gattError.value = null
    }

    override fun onFailedToConnect(id: String, message: String) {
        if (id != connectedAddress.value) return
        connectTimer?.cancel()
        connectedAddress.value = null
        clearGattState()
        connection.value = BluetoothConnectionState.Error(message)
        lastEvent.value = "didFailToConnect: $message"
    }

    override fun onDisconnected(id: String, message: String?) {
        // Tap Connect on B while connected to A: A's didDisconnect arrives later and must not overwrite B.
        if (id != connectedAddress.value) return
        connectTimer?.cancel()
        connectedAddress.value = null
        clearGattState()
        connection.value = if (userRequestedDisconnect) BluetoothConnectionState.Disconnected
        else BluetoothConnectionState.Error("Unexpected disconnect: ${message ?: "unknown"}")
        lastEvent.value = "didDisconnect: ${message ?: "user requested"}"
        userRequestedDisconnect = false
    }

    override fun onServices(id: String, services: List<GattServiceInfo>, done: Boolean) {
        if (id != connectedAddress.value) return
        this.services.value = services
        if (!done) return
        isDiscovering.value = false
        refreshMtu()                                   // at didConnect iOS may still report 23
        lastEvent.value = "Discovered ${services.size} services, ${services.sumOf { it.characteristics.size }} characteristics · MTU ${mtu.value}"
    }

    override fun onGattError(id: String, message: String) {
        if (id != connectedAddress.value) return
        isDiscovering.value = false
        gattError.value = message
    }

    override fun onValue(id: String, serviceUuid: String, uuid: String, data: NSData?, error: String?) {
        if (id != connectedAddress.value) return
        val wasRead = pendingReads.remove("$serviceUuid/$uuid")
        if (error != null) {
            gattError.value = "${if (wasRead) "Read" else "Notify"} $uuid failed: $error"
            return
        }
        val value = gattValue(serviceUuid, uuid, data?.toByteArray() ?: ByteArray(0), if (wasRead) GattValueSource.READ else GattValueSource.NOTIFY)
        if (wasRead) {
            lastRead.value = value
            lastEvent.value = "Read $uuid: ${value.decoded}"
        }
        received.value = appendCapped(received.value, value)
    }

    override fun onNotifyState(id: String, serviceUuid: String, uuid: String, isNotifying: Boolean, error: String?) {
        if (id != connectedAddress.value) return
        if (error != null) gattError.value = "Subscribe $uuid failed: $error"
        else lastEvent.value = "Notify ${if (isNotifying) "ON" else "OFF"}: $uuid"
        // Always the REAL state from CoreBluetooth, not what we asked for
        val key = "$serviceUuid/$uuid"
        services.value = services.value.map { s ->
            s.copy(characteristics = s.characteristics.map { c -> if (c.key == key) c.copy(isNotifying = isNotifying) else c })
        }
    }

    override fun onWritten(id: String, serviceUuid: String, uuid: String, error: String?) {
        if (id != connectedAddress.value) return
        val bytes = pendingWrites.remove("$serviceUuid/$uuid") ?: return
        if (error != null) {
            gattError.value = "Write $uuid failed: $error"
            return
        }
        logWrite(serviceUuid, uuid, bytes)
    }

    // --- helpers --------------------------------------------------------------------------------------------

    private fun logWrite(serviceUuid: String, uuid: String, bytes: ByteArray) {
        received.value = appendCapped(received.value, gattValue(serviceUuid, uuid, bytes, GattValueSource.WRITE))
        lastEvent.value = "Wrote ${bytes.size} bytes to $uuid"
    }

    private fun gattValue(serviceUuid: String, uuid: String, bytes: ByteArray, source: GattValueSource) = GattValue(
        serviceUuid = serviceUuid,
        uuid = uuid,
        hex = BluetoothDecoder.toHex(bytes),
        decoded = BluetoothDecoder.decode(uuid, bytes),
        source = source,
        atMs = Clock.System.now().toEpochMilliseconds(),
    )

    /** iOS negotiates the MTU itself, no request API: read it back as maxWriteWithoutResponse + 3. */
    private fun refreshMtu() {
        val max = native.maxWriteWithoutResponse()
        if (max > 0) mtu.value = max + ATT_HEADER_BYTES
    }

    /** Everything that belongs to one connection. lastRead + received stay so they can be read after disconnect. */
    private fun clearGattState() {
        services.value = emptyList()
        isDiscovering.value = false
        pendingReads.clear()
        pendingWrites.clear()
        gattError.value = null
        mtu.value = ATT_DEFAULT_MTU
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 10_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
    }
}
