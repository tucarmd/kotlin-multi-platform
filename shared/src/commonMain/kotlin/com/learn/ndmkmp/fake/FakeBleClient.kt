package com.learn.ndmkmp.fake

import com.learn.ndmkmp.core.bluetooth.ATT_REQUESTED_MTU
import com.learn.ndmkmp.core.bluetooth.ATT_DEFAULT_MTU
import com.learn.ndmkmp.core.bluetooth.BleClient
import com.learn.ndmkmp.core.bluetooth.BluetoothAdvertisement
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDecoder
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import com.learn.ndmkmp.core.bluetooth.BluetoothScanFilter
import com.learn.ndmkmp.core.bluetooth.GattCharacteristicInfo
import com.learn.ndmkmp.core.bluetooth.GattProperties
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * P2 stand-in for the real BLE client (P4): scan finds 3 made-up devices, connect "discovers" a Battery service and
 * a CarMD-style UART (`FFE0/FFE1`), write `ATZ\r` answers `ELM327 v1.5\r>` by notify. Same delays as a real phone so
 * the spinners and disabled states are visible. Lets every Scanner state run on the iOS Simulator (no Bluetooth there).
 */
class FakeBleClient(private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)) : BleClient {

    override val devices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val isScanning = MutableStateFlow(false)
    override val connection = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    override val connectedAddress = MutableStateFlow<String?>(null)
    override val lastEvent = MutableStateFlow("Demo mode — fake devices, no real Bluetooth")
    override val hiddenCount = MutableStateFlow(0)
    override val services = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    override val isDiscovering = MutableStateFlow(false)
    override val lastRead = MutableStateFlow<GattValue?>(null)
    override val received = MutableStateFlow<List<GattValue>>(emptyList())
    override val gattError = MutableStateFlow<String?>(null)
    override val mtu = MutableStateFlow(ATT_DEFAULT_MTU)

    private var scanJob: Job? = null

    override fun startScan(filter: BluetoothScanFilter) {
        if (isScanning.value) return
        devices.value = emptyList()
        isScanning.value = true
        lastEvent.value = "Scanning (fake)…"
        scanJob = scope.launch {
            FAKE_DEVICES.forEach { d ->
                delay(700)
                val seen = d.copy(lastSeenMs = monotonicMs())
                if (filter.matches(seen.name)) devices.update { it + seen } else hiddenCount.update { it + 1 }
            }
            delay(SCAN_MS)
            stopScan()
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        isScanning.value = false
        lastEvent.value = "Scan stopped · ${devices.value.size} device(s)"
    }

    override fun connect(address: String) {
        val device = devices.value.firstOrNull { it.address == address } ?: return
        stopScan()
        connectedAddress.value = address
        connection.value = BluetoothConnectionState.Connecting
        scope.launch {
            delay(900)
            connection.value = BluetoothConnectionState.Connected(device.name ?: address)
            lastEvent.value = "Connected · discovering services…"
            isDiscovering.value = true
            delay(600)
            services.value = FAKE_SERVICES
            mtu.value = ATT_REQUESTED_MTU
            isDiscovering.value = false
            lastEvent.value = "Discovered ${FAKE_SERVICES.size} services · MTU $ATT_REQUESTED_MTU"
        }
    }

    override fun disconnect() {
        connection.value = BluetoothConnectionState.Disconnected
        connectedAddress.value = null
        services.value = emptyList()
        lastRead.value = null
        gattError.value = null
        mtu.value = ATT_DEFAULT_MTU
        lastEvent.value = "Disconnected"
    }

    override fun read(characteristic: GattCharacteristicInfo) {
        val bytes = if (characteristic.uuid == "2A19") byteArrayOf(90) else "OK".encodeToByteArray()
        lastRead.value = valueOf(characteristic, bytes, GattValueSource.READ)
    }

    override fun setNotify(characteristic: GattCharacteristicInfo, enabled: Boolean) {
        services.update { list ->
            list.map { s ->
                s.copy(characteristics = s.characteristics.map { c -> if (c.key == characteristic.key) c.copy(isNotifying = enabled) else c })
            }
        }
        lastEvent.value = if (enabled) "Notify ON ${characteristic.uuid}" else "Notify OFF ${characteristic.uuid}"
    }

    override fun write(characteristic: GattCharacteristicInfo, bytes: ByteArray, withResponse: Boolean) {
        received.update { appendCapped(it, valueOf(characteristic, bytes, GattValueSource.WRITE)) }
        val notifying = services.value.flatMap { it.characteristics }.any { it.key == characteristic.key && it.isNotifying }
        if (!notifying) return
        scope.launch {   // the dongle answers by notify, like a real ELM327
            delay(300)
            received.update { appendCapped(it, valueOf(characteristic, "ELM327 v1.5\r>".encodeToByteArray(), GattValueSource.NOTIFY)) }
        }
    }

    override fun clearReceived() {
        received.value = emptyList()
    }

    override fun onRadioOff() = disconnect()

    override fun release() {
        stopScan()
        disconnect()
    }

    private fun valueOf(c: GattCharacteristicInfo, bytes: ByteArray, source: GattValueSource) = GattValue(
        serviceUuid = c.serviceUuid,
        uuid = c.uuid,
        hex = BluetoothDecoder.toHex(bytes),
        decoded = BluetoothDecoder.decode(c.uuid, bytes),
        source = source,
        atMs = Clock.System.now().toEpochMilliseconds(),
    )

    private companion object {
        const val SCAN_MS = 4_000L

        fun adv(rssiPackets: Int, services: List<String>, audio: Boolean = false) = BluetoothAdvertisement(
            isConnectable = true, txPower = null, bondState = "NONE", flags = 0x06,
            serviceUuids = services, manufacturerData = emptyList(), serviceData = emptyList(),
            rawHex = "02 01 06", packetCount = rssiPackets, intervalMs = 100, deviceType = "LE", isAudioClass = audio,
        )

        val FAKE_DEVICES = listOf(
            BluetoothDeviceInfo("AA:BB:CC:00:00:01", "CMD-01 (demo)", -52, adv(40, listOf("FFE0"))),
            BluetoothDeviceInfo("AA:BB:CC:00:00:02", "HR Sensor (demo)", -71, adv(18, listOf("180D"))),
            BluetoothDeviceInfo("AA:BB:CC:00:00:03", "Buds (demo)", -84, adv(6, emptyList(), audio = true)),
        )

        val FAKE_SERVICES = listOf(
            GattServiceInfo("180F", listOf(GattCharacteristicInfo("180F", "2A19", GattProperties(read = true, notify = true)))),
            GattServiceInfo(
                "FFE0",
                listOf(GattCharacteristicInfo("FFE0", "FFE1", GattProperties(write = true, writeNoResponse = true, notify = true))),
            ),
        )
    }
}
