package com.learn.ndmkmp.presentation

import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import com.learn.ndmkmp.core.bluetooth.GattCharacteristicInfo
import com.learn.ndmkmp.core.bluetooth.GattProperties
import com.learn.ndmkmp.core.bluetooth.GattServiceInfo
import com.learn.ndmkmp.fake.FakeBleClient
import com.learn.ndmkmp.presentation.scanner.ScannerStrings
import com.learn.ndmkmp.presentation.scanner.ScannerViewModel
import com.learn.ndmkmp.presentation.scanner.StatusTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** R1 — what ScannerScreen used to compute while drawing, now checked without a UI. */
class ScannerViewModelTest : MainDispatcherTest() {

    private val readiness = MutableStateFlow(BleReadiness.READY)
    private val devices = listOf(
        BluetoothDeviceInfo("A1", "CMD-01", -50),
        BluetoothDeviceInfo("B2", null, -70),
        BluetoothDeviceInfo("C3", "HR Sensor", -90),
    )

    private fun TestScope.vm(ble: FakeBleClient) = ScannerViewModel(ble, readiness, now = { 0L })

    @Test
    fun notReady_explainsWhyAndDisablesScan() = runTest(dispatcher) {
        readiness.value = BleReadiness.RADIO_OFF
        val vm = vm(FakeBleClient(this))
        advanceUntilIdle()
        assertEquals(ScannerStrings.NOT_READY_RADIO_OFF, vm.state.value.notReadyText)
        assertEquals(false, vm.state.value.scanEnabled)
        assertTrue(vm.state.value.showOpenPermissions)
        vm.clear()
    }

    @Test
    fun queryAndHideUnknown_filterTheList() = runTest(dispatcher) {
        val ble = FakeBleClient(this).apply { this.devices.value = this@ScannerViewModelTest.devices }
        val vm = vm(ble)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.rows.size)
        assertEquals(ScannerStrings.UNKNOWN_DEVICE, vm.state.value.rows[1].name)

        vm.setHideUnknown(true)
        advanceUntilIdle()
        assertEquals(listOf("A1", "C3"), vm.state.value.rows.map { it.address })
        assertEquals("2 of 3", vm.state.value.matchCountText)

        vm.onQueryChange("zzz")
        advanceUntilIdle()
        assertEquals(ScannerStrings.noMatchFormat("zzz"), vm.state.value.emptyListText)
        vm.clear()
    }

    @Test
    fun select_thenTapAgain_expandsDetails_andEnablesConnect() = runTest(dispatcher) {
        val ble = FakeBleClient(this).apply { this.devices.value = this@ScannerViewModelTest.devices }
        val vm = vm(ble)
        advanceUntilIdle()
        assertEquals(false, vm.state.value.buttons.connectEnabled)

        vm.onDeviceClick("A1")
        advanceUntilIdle()
        assertTrue(vm.state.value.rows.first().isSelected)
        assertTrue(vm.state.value.buttons.connectEnabled)
        assertTrue(vm.state.value.rows.first().detailLines.isEmpty())   // no advertisement in this test device
        vm.clear()
    }

    @Test
    fun connectedWithWritableService_buildsWriteBlockAndPlan() = runTest(dispatcher) {
        val ble = FakeBleClient(this)
        ble.connection.value = BluetoothConnectionState.Connected("CMD-01")
        ble.services.value = listOf(
            GattServiceInfo("FFE0", listOf(GattCharacteristicInfo("FFE0", "FFE1", GattProperties(write = true, notify = true)))),
        )
        val vm = vm(ble)
        advanceUntilIdle()
        assertEquals(StatusTone.CONNECTED, vm.state.value.statusTone)
        assertNull(vm.state.value.gattMessage)
        assertEquals(4, vm.state.value.targetLines.size)
        val write = vm.state.value.write!!
        assertEquals(false, write.canSend)
        assertEquals(false, write.previewIsError)          // empty box = grey, not red

        vm.applyWritePreset("ATZ\\r")
        advanceUntilIdle()
        assertEquals("→ 41 54 5A 0D (4 bytes)", vm.state.value.write!!.previewText)
        assertTrue(vm.state.value.write!!.canSend)

        vm.setWriteHex(true)
        vm.onWriteInputChange("4G")
        advanceUntilIdle()
        assertTrue(vm.state.value.write!!.previewIsError)
        vm.clear()
    }
}
