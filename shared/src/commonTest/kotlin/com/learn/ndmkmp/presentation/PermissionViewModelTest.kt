package com.learn.ndmkmp.presentation

import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.core.permission.PermissionStatus
import com.learn.ndmkmp.fake.FakePermissionController
import com.learn.ndmkmp.presentation.permission.PermissionAction
import com.learn.ndmkmp.presentation.permission.PermissionStrings
import com.learn.ndmkmp.presentation.permission.PermissionViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** R1 — the card / readiness rules that used to sit inside PermissionScreen's `when`. */
class PermissionViewModelTest : MainDispatcherTest() {

    private val controller = FakePermissionController()

    private fun PermissionViewModel.card(id: String) = state.value.cards.first { it.groupId == id }

    @Test
    fun fresh_everyCardAsksAndScanIsBlockedByPermission() = runTest(dispatcher) {
        val vm = PermissionViewModel(controller)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.cards.size)
        vm.state.value.cards.forEach { assertEquals(PermissionAction.REQUEST, it.action) }
        assertNull(vm.state.value.lastResult)
        assertEquals(BleReadiness.MISSING_PERMISSION, vm.readiness.value)
        vm.clear()
    }

    @Test
    fun deniedTwice_switchesToOpenSettings() = runTest(dispatcher) {
        val vm = PermissionViewModel(controller)
        vm.request(PermissionGroupId.CAMERA)
        advanceUntilIdle()
        assertEquals(PermissionStatus.DENIED, vm.card(PermissionGroupId.CAMERA).status)
        assertEquals(PermissionStrings.RATIONALE, vm.card(PermissionGroupId.CAMERA).hint)
        assertEquals(PermissionAction.REQUEST, vm.card(PermissionGroupId.CAMERA).action)

        vm.request(PermissionGroupId.CAMERA)
        advanceUntilIdle()
        assertEquals(PermissionAction.OPEN_SETTINGS, vm.card(PermissionGroupId.CAMERA).action)
        assertEquals(PermissionStrings.PERMANENTLY_DENIED_HINT, vm.card(PermissionGroupId.CAMERA).hint)
        vm.clear()
    }

    @Test
    fun bleGranted_thenRadioOn_isReady() = runTest(dispatcher) {
        val vm = PermissionViewModel(controller)
        vm.request(PermissionGroupId.BLE_SCAN)
        vm.request(PermissionGroupId.BLE_CONNECT)
        advanceUntilIdle()
        assertEquals(BleReadiness.RADIO_OFF, vm.readiness.value)
        assertEquals(true, vm.state.value.showTurnOnBluetooth)
        assertEquals(PermissionAction.NONE, vm.card(PermissionGroupId.BLE_SCAN).action)

        vm.turnOnBluetooth()
        advanceUntilIdle()
        assertEquals(BleReadiness.READY, vm.readiness.value)
        assertEquals(PermissionStrings.READY_YES, vm.state.value.readinessText)
        assertEquals(false, vm.state.value.showTurnOnBluetooth)
        vm.clear()
    }
}
