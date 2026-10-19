package com.learn.ndmkmp.platform

import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.bluetooth.BluetoothScanFilter
import com.learn.ndmkmp.core.bluetooth.GattCharacteristicInfo
import com.learn.ndmkmp.core.bluetooth.GattProperties
import com.learn.ndmkmp.core.bluetooth.GattServiceInfo
import com.learn.ndmkmp.core.bluetooth.GattValueSource
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.core.permission.PermissionStatus
import com.learn.ndmkmp.presentation.MainDispatcherTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** R5 — the iOS adapters' rules, with the Swift side replaced by recording fakes (no CoreBluetooth needed). */
class IosAdaptersTest : MainDispatcherTest() {

    private class FakeBleNative : IosBleNative {
        val calls = mutableListOf<String>()
        override fun setEvents(events: IosBleEvents) = Unit
        override fun prepare() { calls += "prepare" }
        override fun startScan() { calls += "startScan" }
        override fun stopScan() { calls += "stopScan" }
        override fun connect(id: String): Boolean { calls += "connect $id"; return true }
        override fun cancelConnection(id: String) { calls += "cancel $id" }
        override fun read(serviceUuid: String, uuid: String) { calls += "read $uuid" }
        override fun setNotify(serviceUuid: String, uuid: String, enabled: Boolean) { calls += "notify $uuid $enabled" }
        override fun write(serviceUuid: String, uuid: String, data: NSData, withResponse: Boolean) { calls += "write $uuid ${data.length}" }
        override fun maxWriteWithoutResponse(): Int = 182
    }

    private fun IosBleClient.discover(id: String, name: String?) = onDiscovered(
        id, name, -60, 1, IosBleEvents.UNKNOWN, emptyList(), null, emptyList(), emptyList(),
    )

    private val uart = GattCharacteristicInfo("FFE0", "FFE1", GattProperties(write = true, notify = true, read = true))

    @Test
    fun scan_waitsForPowerOn_filtersAndDedupes_stopsAfter10s() = runTest(dispatcher) {
        val native = FakeBleNative()
        val ble = IosBleClient(native)
        ble.startScan(BluetoothScanFilter.CARMD_TOOLS)          // manager not on yet → pending
        assertEquals(false, ble.isScanning.value)
        ble.onPoweredOn(true)
        assertTrue(ble.isScanning.value)
        ble.discover("A", "CMD-01")
        ble.discover("B", "Speaker")                             // filtered
        ble.discover("A", "CMD-01")                              // same device again → one row, 2 packets
        assertEquals(listOf("A"), ble.devices.value.map { it.address })
        assertEquals(2, ble.devices.value.single().advertisement!!.packetCount)
        assertEquals(1, ble.hiddenCount.value)
        advanceTimeBy(10_001)
        assertEquals(false, ble.isScanning.value)
        assertTrue("stopScan" in native.calls)
        ble.release()
    }

    @Test
    fun connect_services_readVsNotify_write_mtu() = runTest(dispatcher) {
        val native = FakeBleNative()
        val ble = IosBleClient(native).apply { onPoweredOn(true); startScan(BluetoothScanFilter.NONE); discover("A", "CMD-01") }
        ble.connect("A")
        assertEquals(BluetoothConnectionState.Connecting, ble.connection.value)
        ble.onConnected("A", "CMD-01")
        assertEquals(BluetoothConnectionState.Connected("CMD-01"), ble.connection.value)
        assertEquals(185, ble.mtu.value)                          // 182 + 3
        ble.onServices("A", listOf(GattServiceInfo("FFE0", listOf(uart))), done = true)
        assertEquals(false, ble.isDiscovering.value)

        ble.read(uart)
        ble.onValue("A", "FFE0", "FFE1", null, null)              // answer to our read
        assertEquals(GattValueSource.READ, ble.lastRead.value!!.source)
        ble.onValue("A", "FFE0", "FFE1", null, null)              // nothing pending → a notification
        assertEquals(GattValueSource.NOTIFY, ble.received.value.last().source)

        ble.write(uart, byteArrayOf(0x41, 0x54), withResponse = true)
        assertEquals(2, ble.received.value.size)                  // not logged before the ACK
        ble.onWritten("A", "FFE0", "FFE1", null)
        assertEquals(GattValueSource.WRITE, ble.received.value.last().source)
        ble.release()
    }

    @Test
    fun connectTimeout_and_radioOff_cleanUp() = runTest(dispatcher) {
        val native = FakeBleNative()
        val ble = IosBleClient(native).apply { onPoweredOn(true); startScan(BluetoothScanFilter.NONE); discover("A", "x") }
        ble.connect("A")
        advanceTimeBy(15_001)                                     // iOS would wait forever
        assertIs<BluetoothConnectionState.Error>(ble.connection.value)
        assertTrue("cancel A" in native.calls)

        ble.connect("A")
        ble.onConnected("A", "x")
        ble.onPoweredOn(false)                                    // no didDisconnect on iOS
        assertEquals(BluetoothConnectionState.Error("Bluetooth turned off"), ble.connection.value)
        assertNull(ble.connectedAddress.value)
        assertTrue(ble.devices.value.isEmpty())
    }

    private class FakePermissionNative(var bt: NativeAuthorization, var cam: NativeAuthorization) : IosPermissionNative {
        var radioStarted = false
        override fun bluetoothAuthorization() = bt
        override fun cameraAuthorization() = cam
        override fun requestBluetooth(done: () -> Unit) { bt = NativeAuthorization.GRANTED; done() }
        override fun requestCamera(done: (Boolean) -> Unit) { cam = NativeAuthorization.DENIED; done(false) }
        override fun openSettings() = Unit
        override fun startRadio(onChange: (BluetoothRadioState) -> Unit) { radioStarted = true; onChange(BluetoothRadioState.ON) }
        override fun showPowerAlert() = Unit
    }

    @Test
    fun permissions_mapAppleStates_oneBluetoothPromptForBothCards() {
        val native = FakePermissionNative(NativeAuthorization.NOT_DETERMINED, NativeAuthorization.NOT_DETERMINED)
        val p = IosPermissionController(native)
        assertEquals(PermissionStatus.NOT_REQUESTED, p.states.value[PermissionGroupId.BLE_SCAN]!!.status)
        assertEquals(false, native.radioStarted)                  // no CBCentralManager before the answer

        p.request(p.groups.first { it.id == PermissionGroupId.BLE_CONNECT })
        assertEquals(PermissionStatus.GRANTED, p.states.value[PermissionGroupId.BLE_SCAN]!!.status)
        assertEquals(PermissionStatus.GRANTED, p.states.value[PermissionGroupId.BLE_CONNECT]!!.status)
        assertEquals(BluetoothRadioState.ON, p.radio.value)

        p.request(p.groups.first { it.id == PermissionGroupId.CAMERA })
        // iOS asks once: denied = only Settings left
        assertEquals(PermissionStatus.PERMANENTLY_DENIED, p.states.value[PermissionGroupId.CAMERA]!!.status)
        assertEquals("camera -> DENIED", p.lastResult.value)
    }
}
