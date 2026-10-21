package com.learn.nativedevicemonitor.nativecore.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Day 2 — real BLE scan + connect/disconnect. Day 3 — discover + read + decode. Day 4 — notify + cleanup on disconnect.
 * Task 004 — write characteristic + request MTU.
 * `nativecore/` layer: imports NOTHING from `ui/`.
 *
 * No auto-reconnect yet. See docs/README-HOW-TO-IMPLEMENT.md §5–§6, §8.4 and docs/tasks/003-gatt-read-notify.md, 004-write-mtu.md.
 *
 * ```
 *  startScan(filter)
 *    │  BluetoothLeScanner.startScan(scanCallback)
 *    ▼
 *  onScanResult (Binder thread, NOT main)
 *    │  filter.matches(name)? no → hiddenCount++
 *    │  found[address] = BluetoothDeviceInfo(..., advertisement)  <- Map = dedupe, one row per device
 *    ▼
 *  _devices: MutableStateFlow  ──collectAsState()──►  ScannerScreen
 *    └── after SCAN_TIMEOUT_MS (10 s) stopScan() by itself     <- endless scan = battery drain + Android throttles
 *
 *  connect(address)
 *    │  stopScan() → device.connectGatt(ctx, autoConnect=false, gattCallback, TRANSPORT_LE)
 *    ▼
 *  onConnectionStateChange(status, newState)   (thread Binder)
 *    │  CONNECTED    → Connected(name)
 *    │  DISCONNECTED → Disconnected (we tapped) | Error("status=…") (signal lost / device disconnected)
 *    └─ always gatt.close()  <- no close LEAKS a slot, next connect returns status=133
 *
 *  onRadioOff()  (Activity listens to the radio-off broadcast)
 *    └─ clear scan flag, clear list, close GATT, Error("Bluetooth turned off")
 *
 *  ── Day 3 ──────────────────────────────────────────────────────────────────────────────
 *  CONNECTED → discoverServices() → onServicesDiscovered → services: [GattServiceInfo]
 *  read(chr)      → gattOps (queue, 1 op at a time) → readCharacteristic → onCharacteristicRead
 *                   └─ GattValue(hex, decode(uuid, bytes)) → lastRead + received
 *  ── Day 4 ──────────────────────────────────────────────────────────────────────────────
 *  setNotify(on)  → setCharacteristicNotification + ghi CCCD 2902 (qua gattOps) → onDescriptorWrite → isNotifying
 *  onCharacteristicChanged → GattValue(NOTIFY) → received (max 50 lines)          ← data → event → UI
 *  disconnect()   → turn notify off locally → gatt.disconnect() → DISCONNECTED → clearGattState()
 *  signal lost / radio off ─────────────────────────────► clearGattState() + Error (red)
 *  ── Task 004 ───────────────────────────────────────────────────────────────────────────
 *  discover xong  → gattOps "Request MTU" → requestMtu(247) → onMtuChanged(mtu) → mtu (UI: max = mtu − 3 byte / write)
 *  write(chr, bytes) → gattOps → writeCharacteristic(DEFAULT | NO_RESPONSE) → onCharacteristicWrite
 *                   └─ GattValue(WRITE) → received        peripheral replies (if any) → onCharacteristicChanged
 * ```
 *
 * ## Why a GATT queue is needed (Android)
 * Android runs **only 1 GATT operation at a time**. Calling `readCharacteristic` while a CCCD write is still pending →
 * returns `false` (or `ERROR_GATT_WRITE_REQUEST_BUSY`) and the operation is **lost**, no callback. iOS queues by itself.
 *
 * ## Day 3–4 GATT callbacks hop to main
 * Every new callback does `scope.launch { … }` → runs on main → the queue + handle map are only touched from 1 thread,
 * no lock needed.
 *
 * ## Why `StateFlow` instead of plain variables
 * BLE callbacks arrive on **an internal system thread** (`Binder:xxxx`), not the main thread.
 * `MutableStateFlow` is thread-safe, and `collectAsState()` in the UI hops to main before drawing.
 * *Compared to TS:* `StateFlow` ≈ RxJS `BehaviorSubject`.
 *
 * ## `@SuppressLint("MissingPermission")`
 * Every call below needs `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`. Permission checks happen on Day 1
 * (`PermissionService` + `BluetoothService.readiness`) and the UI disables buttons until granted. Lint can't see
 * through those 2 layers so it's suppressed manually — **in exactly one place, with an explanation**.
 *
 * @param appContext **must be** `applicationContext` — the service outlives the screen, holding an Activity leaks memory.
 */
@SuppressLint("MissingPermission")
class BluetoothClientService(private val appContext: Context) {

    private companion object {
        const val TAG = "BleClient"

        /**
         * 10 s. A timeout is mandatory: Android punishes apps that start a scan more than 5 times / 30 s by **silently
         * returning 0 results** (`SCAN_FAILED_SCANNING_TOO_FREQUENTLY`). Stopping on purpose is also the only way
         * to know "scan done, saw N devices".
         */
        const val SCAN_TIMEOUT_MS = 10_000L

        /** Client Characteristic Configuration Descriptor — the notify/indicate "switch" that lives on the peripheral. */
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** Label of the MTU op in [gattOps] — `onMtuChanged` uses it to know whether our op is the one waiting. */
        const val MTU_OP_LABEL = "Request MTU"
    }

    // --- State pushed to the UI ----------------------------------------------------------------

    private val _devices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val devices: StateFlow<List<BluetoothDeviceInfo>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connection = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    val connection: StateFlow<BluetoothConnectionState> = _connection.asStateFlow()

    /** Address of the device being / just connected — the UI uses it to mark the right row in the list. */
    private val _connectedAddress = MutableStateFlow<String?>(null)
    val connectedAddress: StateFlow<String?> = _connectedAddress.asStateFlow()

    /** Raw status line for the learner to read by eye (scan failed code, status=133…). */
    private val _lastEvent = MutableStateFlow("")
    val lastEvent: StateFlow<String> = _lastEvent.asStateFlow()

    /** Devices that advertised but were dropped by the filter — UI shows "N other devices hidden" so scanning doesn't look broken. */
    private val _hiddenCount = MutableStateFlow(0)
    val hiddenCount: StateFlow<Int> = _hiddenCount.asStateFlow()

    // --- State Day 3 + Day 4 (GATT) ---

    /** Service → characteristic tree of the connected device. Empty until discovery finishes / after disconnect. */
    private val _services = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    val services: StateFlow<List<GattServiceInfo>> = _services.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    /** Most recent Read (Day 3 exercise: raw bytes + decoded). Kept after disconnect so it can still be read. */
    private val _lastRead = MutableStateFlow<GattValue?>(null)
    val lastRead: StateFlow<GattValue?> = _lastRead.asStateFlow()

    /** The *Received Data* box: every read + notify value, oldest on top, at most [RECEIVED_LOG_MAX] lines. */
    private val _received = MutableStateFlow<List<GattValue>>(emptyList())
    val received: StateFlow<List<GattValue>> = _received.asStateFlow()

    /** Read / subscribe / discover errors (don't drop the connection) — UI shows them in red in the GATT block. */
    private val _gattError = MutableStateFlow<String?>(null)
    val gattError: StateFlow<String?> = _gattError.asStateFlow()

    /** Task 004 — negotiated MTU. 23 until `onMtuChanged` reports a new value; disconnect → back to 23. */
    private val _mtu = MutableStateFlow(ATT_DEFAULT_MTU)
    val mtu: StateFlow<Int> = _mtu.asStateFlow()

    // --- Internals -------------------------------------------------------------------------------

    /**
     * Map address → device. `onScanResult` fires **continuously** for the same device (once per advertising
     * packet) — appending straight to a List gives 200 duplicate rows in 10 s. LinkedHashMap keeps first-seen order.
     */
    private val found = LinkedHashMap<String, BluetoothDeviceInfo>()

    @Volatile
    private var scanFilter: BluetoothScanFilter = BluetoothScanFilter.NONE
    /** Devices filtered out (by address) — a Set so we count DEVICES, not packets. */
    private val hidden = HashSet<String>()
    /** address → (packet count, timestampNanos of first packet) to estimate the interval like nRF does. */
    private val packetStats = HashMap<String, Pair<Int, Long>>()

    /** The service's own scope, cancelled in [release]. Not borrowing `lifecycleScope`: the service outlives the screen. */
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var scanTimeoutJob: Job? = null

    /** Holds the GATT handle for `disconnect`/`close`. Without it the connection can never be closed. */
    private var gatt: BluetoothGatt? = null

    /** Tells "we tapped Disconnect" apart from "signal lost" — both arrive on the same callback. */
    @Volatile
    private var userRequestedDisconnect = false

    /** `service/char` → real handle for read / setNotify. The UI only holds [GattCharacteristicInfo] (a snapshot). */
    private val charHandles = HashMap<String, BluetoothGattCharacteristic>()

    /** One GATT op: [start] returns false = Android rejected it immediately (busy / disconnected) → no callback will come. */
    private class GattOp(val label: String, val start: () -> Boolean)

    private val gattOps = ArrayDeque<GattOp>()
    private var opInFlight: GattOp? = null

    /** `service/char` → writing CCCD to enable (true) or disable (false). onDescriptorWrite reads it back to know the outcome. */
    private val pendingCccd = HashMap<String, Boolean>()

    /**
     * Bytes of the in-flight write. The queue allows only 1 op at a time so one variable is enough. Needed because
     * `onCharacteristicWrite` does **not** return the written bytes — and we want to print them in *Received Data*.
     */
    private var pendingWrite: ByteArray? = null

    private val adapter
        get() = ContextCompat.getSystemService(appContext, BluetoothManager::class.java)?.adapter

    // --- Scan --------------------------------------------------------------------------------

    /**
     * **One** `ScanCallback` instance for the service's whole life. `stopScan(callback)` compares by **reference**
     * — create a new callback on every Scan tap and stop won't match, the old scan runs forever.
     */
    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            // device.name needs BLUETOOTH_CONNECT (API 31+) — already guarded in the UI. Fall back to the name in the advertising packet:
            // device.name may be null on the first packet (the system hasn't cached the name yet).
            val name = device.name ?: result.scanRecord?.deviceName
            if (!scanFilter.matches(name)) {
                if (hidden.add(device.address)) _hiddenCount.value = hidden.size
                return
            }
            val info = BluetoothDeviceInfo(
                address = device.address,
                name = name,
                rssi = result.rssi,
                advertisement = advertisementOf(result),
                // timestampNanos uses the elapsedRealtime clock → UI compares with SystemClock.elapsedRealtime() to show "N s ago"
                lastSeenMs = result.timestampNanos / 1_000_000,
            )
            found[info.address] = info          // overwrite → RSSI is always the latest
            // Assign a NEW List: StateFlow compares with equals, mutating the old list in place won't redraw the UI
            _devices.value = found.values.toList()
        }

        /** Devices advertising rapidly → the system batches several results at once. */
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(0, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "onScanFailed code=$errorCode")
            _isScanning.value = false
            // Keep the raw number: 1 = already started · 2 = registration failed · 6 = scanning too frequently
            _lastEvent.value = "Scan failed: code=$errorCode"
        }
    }

    /** Turn the advertising packet into a snapshot like the nRF Scanner device card. Runs on the Binder thread — pure computation only. */
    private fun advertisementOf(result: ScanResult): BluetoothAdvertisement {
        val address = result.device.address
        val now = result.timestampNanos
        val (count, first) = packetStats[address] ?: (0 to now)
        val newCount = count + 1
        packetStats[address] = newCount to first
        val record: ScanRecord? = result.scanRecord
        return BluetoothAdvertisement(
            // minSdk 26 = O → isConnectable / txPower always exist, no version check needed (lint ObsoleteSdkInt).
            isConnectable = result.isConnectable,
            txPower = if (result.txPower != ScanResult.TX_POWER_NOT_PRESENT) {
                result.txPower
            } else {
                record?.txPowerLevel?.takeIf { it != Int.MIN_VALUE }
            },
            bondState = when (result.device.bondState) {
                BluetoothDevice.BOND_BONDED -> "Bonded"
                BluetoothDevice.BOND_BONDING -> "Bonding"
                else -> "Not bonded"
            },
            flags = record?.advertiseFlags?.takeIf { it >= 0 },
            serviceUuids = record?.serviceUuids.orEmpty().map { BluetoothDecoder.shortUuid(it.uuid) },
            manufacturerData = record?.manufacturerSpecificData?.let { sparse ->
                (0 until sparse.size()).map { i ->
                    val id = sparse.keyAt(i)
                    val company = BluetoothLookup.companyName(id)?.let { " $it" }.orEmpty()
                    "0x%04X%s: %s".format(id, company, BluetoothDecoder.toHex(sparse.valueAt(i)))
                }
            }.orEmpty(),
            serviceData = record?.serviceData.orEmpty().map { (uuid, bytes) ->
                "${BluetoothDecoder.shortUuid(uuid.uuid)}: ${BluetoothDecoder.toHex(bytes)}"
            },
            // raw bytes are zero-padded to 62 bytes → trim trailing zeros for readability
            rawHex = BluetoothDecoder.toHex(record?.bytes?.dropLastWhile { it.toInt() == 0 }?.toByteArray() ?: ByteArray(0)),
            packetCount = newCount,
            intervalMs = if (newCount > 1) (now - first) / 1_000_000 / (newCount - 1) else null,
            deviceType = when (result.device.type) {
                BluetoothDevice.DEVICE_TYPE_LE -> "LE only"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual mode"
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                else -> "Unknown"
            },
            // Random = address rotates periodically (phones, AirTag…) → today's MAC differs from tomorrow's, don't store it as a long-term key
            addressType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                when (result.device.addressType) {
                    BluetoothDevice.ADDRESS_TYPE_PUBLIC -> "Public"
                    BluetoothDevice.ADDRESS_TYPE_RANDOM -> "Random"
                    else -> "Unknown"
                }
            } else {
                null
            },
            appearance = record?.bytes?.let(BluetoothDecoder::appearanceOf),
            serviceDataUuids = record?.serviceData.orEmpty().keys.map { BluetoothDecoder.shortUuid(it.uuid) },
            appleType = record?.getManufacturerSpecificData(BluetoothLookup.APPLE_COMPANY_ID)?.firstOrNull()?.let { it.toInt() and 0xFF },
            isAudioClass = result.device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO,
        )
    }

    /**
     * Start scanning, stops by itself after [SCAN_TIMEOUT_MS]. Calling again while scanning is ignored —
     * Android returns `SCAN_FAILED_ALREADY_STARTED` if started twice.
     *
     * Filter in CODE inside the callback, NOT with the system `ScanFilter`: `ScanFilter` only matches the exact
     * name, there is no "starts with CMD-". The CarMD Ionic app also filters in the callback.
     */
    fun startScan(filter: BluetoothScanFilter = BluetoothScanFilter.NONE) {
        if (_isScanning.value) return
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            _lastEvent.value = "Bluetooth is off or unavailable"
            return
        }
        found.clear()
        _devices.value = emptyList()
        scanFilter = filter
        hidden.clear()
        _hiddenCount.value = 0
        packetStats.clear()
        _isScanning.value = true
        _lastEvent.value = if (filter.isOff) "Scanning…" else "Scanning (only ${filter.label})…"
        Log.d(TAG, "startScan filter=${filter.label.ifEmpty { "none" }}")
        scanner.startScan(scanCallback)

        // Keep the Job so a manual stopScan can cancel it — otherwise it still fires 10 s later and overwrites later state
        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            stopScan()
        }
    }

    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        if (!_isScanning.value) return
        _isScanning.value = false
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _lastEvent.value = "Scan finished: ${found.size} devices"
        Log.d(TAG, "stopScan found=${found.size}")
    }

    // --- Connect / disconnect ----------------------------------------------------------------

    /**
     * GATT callback — **every function here runs on a Binder thread, NOT main.**
     * Only writes to `MutableStateFlow`, never touches a Composable.
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange ${g.device?.address} status=$status newState=$newState")
            // A late callback from the OLD connection (user tapped CONNECT on another device, see connect()) → ignore,
            // otherwise it overwrites the new device's "Connecting…" with "Disconnected".
            if (g !== gatt) {
                g.close()
                return
            }
            when {
                status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED -> {
                    val name = g.device?.name ?: g.device?.address.orEmpty()
                    _connection.value = BluetoothConnectionState.Connected(name)
                    _lastEvent.value = "Connected to $name"
                    // Day 3: only ask "what services?" once connected. Hop to main to call it — some devices (older Samsung)
                    // ignore discoverServices() called directly on the callback's Binder thread.
                    scope.launch { if (g === gatt) startDiscovery(g) }
                }

                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    _connection.value = if (userRequestedDisconnect) {
                        BluetoothConnectionState.Disconnected
                    } else {
                        // 8 = signal lost/timeout · 19 = device disconnected on purpose · 133 = generic error (most common)
                        BluetoothConnectionState.Error("Unexpected disconnect, status=$status")
                    }
                    _lastEvent.value = "Disconnected (status=$status)"
                    // close() is MANDATORY: each connectGatt takes 1 client slot (~30). Out of slots → every later connect returns 133.
                    closeGatt()
                    // Day 4: both paths (manual / signal lost) clear services + subscriptions + the queue.
                    scope.launch { clearGattState() }
                }
            }
        }

        // --- Day 3 ---

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            scope.launch { if (g === gatt) handleServicesDiscovered(g, status) }
        }

        /** API 33+: bytes come as a parameter — safe, not overwritten by the next notification. */
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            scope.launch { if (g === gatt) handleRead(c, value, status) }
        }

        /** API 26–32: the old variant, bytes live in `c.value` (a shared buffer → copy immediately). Not called on API 33+. */
        @Deprecated("API < 33 only")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            val bytes = c.value?.copyOf() ?: ByteArray(0)
            scope.launch { if (g === gatt) handleRead(c, bytes, status) }
        }

        // --- Day 4 ---

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            scope.launch { if (g === gatt) handleCccdWritten(d.characteristic, status) }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            scope.launch { if (g === gatt) handleNotified(c, value) }
        }

        @Deprecated("API < 33 only")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            val bytes = c.value?.copyOf() ?: ByteArray(0)
            scope.launch { if (g === gatt) handleNotified(c, bytes) }
        }

        // --- Task 004 ---

        /** Peripheral received it (with response) or Android pushed it onto the air (no response) — both end up here. */
        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            scope.launch { if (g === gatt) handleWritten(c, status) }
        }

        /** Reply to `requestMtu`. Some peripherals request MTU themselves → callback arrives even if we didn't ask. */
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            scope.launch { if (g === gatt) handleMtuChanged(mtu, status) }
        }
    }

    /**
     * Connect by MAC. `autoConnect = false`: connect **now**, ~30 s timeout (true = "connect whenever it's
     * seen", slow and hard to debug). `TRANSPORT_LE`: dual-mode devices may pick Classic by mistake and fail with 133.
     *
     * Called while connected to device A (select row B then tap Connect): only 1 connection is kept → close A first.
     * Late callbacks from A are filtered in `gattCallback` (`g !== gatt`).
     */
    fun connect(address: String) {
        val device: BluetoothDevice = adapter?.getRemoteDevice(address) ?: run {
            _connection.value = BluetoothConnectionState.Error("Device not found: $address")
            return
        }
        stopScan()                       // scanning in parallel makes connecting slow and flaky
        gatt?.let {
            it.disconnect()              // tell peripheral A we're leaving (otherwise A thinks it's still connected until timeout)
            Log.d(TAG, "switch device: drop ${it.device?.address}")
        }
        closeGatt()                      // clean up the old connection before opening a new one
        clearGattState()
        _lastRead.value = null           // new device → new log
        _received.value = emptyList()
        userRequestedDisconnect = false
        _connectedAddress.value = address
        _connection.value = BluetoothConnectionState.Connecting
        _lastEvent.value = "Connecting to $address…"
        Log.d(TAG, "connect $address")
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    /**
     * Manual disconnect. Set the flag BEFORE `disconnect()` because the callback may arrive immediately — set it after and it's
     * misreported as "Unexpected disconnect". No `close()` here: wait for the DISCONNECTED callback, closing early means
     * the callback never arrives and the UI is stuck on Connected.
     */
    fun disconnect() {
        val current = gatt ?: return
        // Day 4 — clean up subscriptions BEFORE disconnecting: turn notify off on the Android side so late packets don't land in the log.
        // Don't write CCCD = 0 to the peripheral: the link is about to drop, an unbonded peripheral resets CCCD on disconnect.
        // (A BONDED peripheral remembers CCCD — a real app should write DISABLE via gattOps before disconnecting.)
        val subscribed = _services.value.flatMap { it.characteristics }.filter { it.isNotifying }
        subscribed.forEach { info -> charHandles[info.key]?.let { current.setCharacteristicNotification(it, false) } }
        gattOps.clear()
        opInFlight = null
        userRequestedDisconnect = true
        _lastEvent.value = if (subscribed.isEmpty()) "Disconnecting…" else "Unsubscribed ${subscribed.size} · disconnecting…"
        Log.d(TAG, "disconnect unsubscribed=${subscribed.map { it.uuid }}")
        current.disconnect()
    }

    // --- Day 3: discover + read -------------------------------------------------------------

    private fun startDiscovery(g: BluetoothGatt) {
        _isDiscovering.value = true
        _gattError.value = null
        _lastEvent.value = "Discovering services…"
        // Bonded devices sometimes need ~600 ms after CONNECTED before discovery works. Test devices (nRF / ESP32) don't bond → call right away.
        if (!g.discoverServices()) {
            _isDiscovering.value = false
            _gattError.value = "discoverServices() refused to start"
        }
    }

    private fun handleServicesDiscovered(g: BluetoothGatt, status: Int) {
        _isDiscovering.value = false
        if (status != BluetoothGatt.GATT_SUCCESS) {
            _gattError.value = "Service discovery failed, status=$status"
            return
        }
        charHandles.clear()
        _services.value = g.services.map { s ->
            val serviceUuid = BluetoothDecoder.shortUuid(s.uuid)
            GattServiceInfo(
                uuid = serviceUuid,
                characteristics = s.characteristics.map { c ->
                    GattCharacteristicInfo(serviceUuid, BluetoothDecoder.shortUuid(c.uuid), GattProperties.of(c.properties))
                        .also { charHandles[it.key] = c }
                },
            )
        }
        val count = charHandles.size
        _lastEvent.value = "Discovered ${_services.value.size} services, $count characteristics"
        _services.value.forEach { s ->
            Log.d(TAG, "service ${s.uuid}: " + s.characteristics.joinToString { "${it.uuid}[${it.properties.labels.joinToString("|")}]" })
        }
        // Task 004: ask for bigger packets. Goes through the queue like every GATT op — a Read tapped right now won't collide.
        // The peripheral may return less (e.g. 185) or keep 23 if unsupported → use exactly what onMtuChanged reports.
        enqueue(MTU_OP_LABEL) { g.requestMtu(ATT_REQUESTED_MTU) }
    }

    /** Day 3 — read once. The result arrives in [handleRead]. */
    fun read(characteristic: GattCharacteristicInfo) {
        val g = gatt
        val c = charHandles[characteristic.key]
        if (g == null || c == null) {
            _gattError.value = "Not connected"
            return
        }
        _gattError.value = null
        enqueue("Read ${characteristic.uuid}") { g.readCharacteristic(c) }
    }

    private fun handleRead(c: BluetoothGattCharacteristic, bytes: ByteArray, status: Int) {
        opDone()
        if (status != BluetoothGatt.GATT_SUCCESS) {
            // 2 = READ_NOT_PERMITTED · 5 / 15 = pairing required (insufficient authentication / encryption)
            _gattError.value = "Read ${BluetoothDecoder.shortUuid(c.uuid)} failed, status=$status"
            return
        }
        val value = valueOf(c, bytes, GattValueSource.READ)
        _lastRead.value = value
        _received.value = appendCapped(_received.value, value)
        _lastEvent.value = "Read ${value.uuid}: ${value.decoded}"
    }

    // --- Day 4: notify ------------------------------------------------------------------------

    /**
     * Turn notify on / off. 2 steps, miss either one and you silently get no data:
     * 1. `setCharacteristicNotification` — tells **Android** to route packets to the app's callback (local, not over the air).
     * 2. Write descriptor **CCCD `2902`** = `01 00` (notify) / `02 00` (indicate) — tells the **peripheral** to start pushing.
     */
    fun setNotify(characteristic: GattCharacteristicInfo, enabled: Boolean) {
        val g = gatt
        val c = charHandles[characteristic.key]
        if (g == null || c == null) {
            _gattError.value = "Not connected"
            return
        }
        val cccd = c.getDescriptor(CCCD_UUID) ?: run {
            _gattError.value = "${characteristic.uuid} has no CCCD (2902) — cannot subscribe"
            return
        }
        if (!g.setCharacteristicNotification(c, enabled)) {
            _gattError.value = "setCharacteristicNotification(${characteristic.uuid}) failed"
            return
        }
        _gattError.value = null
        val value = when {
            !enabled -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            characteristic.properties.notify -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        pendingCccd[characteristic.key] = enabled
        enqueue("${if (enabled) "Subscribe" else "Unsubscribe"} ${characteristic.uuid}") { writeDescriptor(g, cccd, value) }
    }

    private fun handleCccdWritten(c: BluetoothGattCharacteristic, status: Int) {
        opDone()
        val key = keyOf(c)
        val enabled = pendingCccd.remove(key) ?: return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt?.setCharacteristicNotification(c, false)       // undo step 1 to match the peripheral
            _gattError.value = "${if (enabled) "Subscribe" else "Unsubscribe"} ${BluetoothDecoder.shortUuid(c.uuid)} failed, status=$status"
            return
        }
        markNotifying(key, enabled)
        _lastEvent.value = "Notify ${if (enabled) "ON" else "OFF"}: ${BluetoothDecoder.shortUuid(c.uuid)}"
    }

    /** Data → event → UI: each notify packet becomes 1 [GattValue], appended to [received]. Compose draws the new line. */
    private fun handleNotified(c: BluetoothGattCharacteristic, bytes: ByteArray) {
        val value = valueOf(c, bytes, GattValueSource.NOTIFY)
        _received.value = appendCapped(_received.value, value)
    }

    fun clearReceived() {
        _received.value = emptyList()
    }

    // --- Task 004: write + MTU ----------------------------------------------------------------

    /**
     * Write [bytes] to the characteristic. The UI already checked the rules via `planWrite` (writable? valid hex? ≤ MTU − 3?)
     * before calling — all that's left here is queueing and sending.
     *
     * [withResponse] true = `WRITE_TYPE_DEFAULT`: peripheral sends an ACK, real error status comes back (e.g. 3 = WRITE_NOT_PERMITTED).
     * false = `WRITE_TYPE_NO_RESPONSE`: no ACK, faster; `onCharacteristicWrite` still arrives but only means "sent".
     */
    fun write(characteristic: GattCharacteristicInfo, bytes: ByteArray, withResponse: Boolean) {
        val g = gatt
        val c = charHandles[characteristic.key]
        if (g == null || c == null) {
            _gattError.value = "Not connected"
            return
        }
        _gattError.value = null
        enqueue("Write ${characteristic.uuid}") {
            pendingWrite = bytes                  // set when the op ACTUALLY runs, not when it's queued
            writeCharacteristic(g, c, bytes, withResponse)
        }
    }

    private fun handleWritten(c: BluetoothGattCharacteristic, status: Int) {
        opDone()
        val bytes = pendingWrite ?: return
        pendingWrite = null
        if (status != BluetoothGatt.GATT_SUCCESS) {
            // 3 = WRITE_NOT_PERMITTED · 13 = INVALID_ATTRIBUTE_LENGTH (too long) · 5 / 15 = pairing required
            _gattError.value = "Write ${BluetoothDecoder.shortUuid(c.uuid)} failed, status=$status"
            return
        }
        val value = valueOf(c, bytes, GattValueSource.WRITE)
        _received.value = appendCapped(_received.value, value)
        _lastEvent.value = "Wrote ${bytes.size} bytes to ${value.uuid}"
    }

    private fun handleMtuChanged(mtu: Int, status: Int) {
        // Only release the queue if our MTU op is really the one waiting — if the peripheral requested MTU itself, the in-flight op is a different one.
        if (opInFlight?.label == MTU_OP_LABEL) opDone()
        if (status != BluetoothGatt.GATT_SUCCESS) {
            _lastEvent.value = "MTU request failed, status=$status — staying at ${_mtu.value}"
            return
        }
        _mtu.value = mtu
        _lastEvent.value = "MTU $mtu → max ${maxPayloadOf(mtu)} bytes per packet"
        Log.d(TAG, "onMtuChanged mtu=$mtu")
    }

    // --- GATT queue -----------------------------------------------------------------------------

    private fun enqueue(label: String, start: () -> Boolean) {
        gattOps.addLast(GattOp(label, start))
        drainOps()
    }

    private fun drainOps() {
        while (opInFlight == null) {
            val op = gattOps.removeFirstOrNull() ?: return
            if (op.start()) {
                opInFlight = op                  // wait for the callback to call opDone() before running the next op
            } else {
                _gattError.value = "${op.label}: Android refused to start it (busy or disconnected)"
            }
        }
    }

    private fun opDone() {
        opInFlight = null
        drainOps()
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptor(g: BluetoothGatt, d: BluetoothGattDescriptor, value: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(d, value) == BluetoothStatusCodes.SUCCESS
        } else {
            d.value = value                       // old API: set on the descriptor then write — the descriptor is a shared object
            g.writeDescriptor(d)
        }

    @Suppress("DEPRECATION")
    private fun writeCharacteristic(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, withResponse: Boolean): Boolean {
        val type = if (withResponse) BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(c, value, type) == BluetoothStatusCodes.SUCCESS
        } else {
            c.writeType = type                    // old API: set on the shared object then write — same trap as writeDescriptor
            c.value = value
            g.writeCharacteristic(c)
        }
    }

    private fun keyOf(c: BluetoothGattCharacteristic) =
        "${BluetoothDecoder.shortUuid(c.service.uuid)}/${BluetoothDecoder.shortUuid(c.uuid)}"

    private fun markNotifying(key: String, enabled: Boolean) {
        _services.value = _services.value.map { s ->
            s.copy(characteristics = s.characteristics.map { if (it.key == key) it.copy(isNotifying = enabled) else it })
        }
    }

    private fun valueOf(c: BluetoothGattCharacteristic, bytes: ByteArray, source: GattValueSource): GattValue {
        val uuid = BluetoothDecoder.shortUuid(c.uuid)
        return GattValue(
            serviceUuid = BluetoothDecoder.shortUuid(c.service.uuid),
            uuid = uuid,
            hex = BluetoothDecoder.toHex(bytes),
            decoded = BluetoothDecoder.decode(uuid, bytes),
            source = source,
            atMs = System.currentTimeMillis(),
        ).also { Log.d(TAG, "${it.source} ${it.uuid} raw=[${it.hex}] decoded=${it.decoded}") }   // Day 3: "print raw bytes + decoded"
    }

    /** Day 4 — clear everything that belongs to the old connection. Keep [lastRead] + [received] so the user can still read them after disconnect. */
    private fun clearGattState() {
        gattOps.clear()
        opInFlight = null
        pendingCccd.clear()
        pendingWrite = null
        _mtu.value = ATT_DEFAULT_MTU              // MTU belongs to one connection — the next connection negotiates again from 23
        charHandles.clear()
        _services.value = emptyList()
        _isDiscovering.value = false
        _gattError.value = null
    }

    /**
     * Called by the Activity when the broadcast says the radio is OFF. Radio off stops the scan **with no callback** and
     * many devices don't fire `onConnectionStateChange` either → without cleanup the UI is stuck on Scanning/Connected.
     */
    fun onRadioOff() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        val hadSomething = _isScanning.value || gatt != null
        _isScanning.value = false             // do NOT call scanner.stopScan: the scanner is already null when the radio is off
        found.clear()
        hidden.clear()
        _hiddenCount.value = 0
        _devices.value = emptyList()
        closeGatt()
        clearGattState()
        if (hadSomething) _connection.value = BluetoothConnectionState.Error("Bluetooth turned off")
        _lastEvent.value = "Bluetooth turned off — list cleared"
        Log.d(TAG, "onRadioOff hadSomething=$hadSomething")
    }

    private fun closeGatt() {
        gatt?.close()
        gatt = null
    }

    /** Clean up everything — the root Activity's `onDestroy`. Cancel the scope so timer Jobs don't run after the screen is gone. */
    fun release() {
        stopScan()
        closeGatt()
        clearGattState()
        scope.coroutineContext[Job]?.cancel()
    }
}
