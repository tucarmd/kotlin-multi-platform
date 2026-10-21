package com.learn.ndmkmp.fake

import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.permission.PermissionController
import com.learn.ndmkmp.core.permission.PermissionGroup
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.core.permission.PermissionGroupState
import com.learn.ndmkmp.core.permission.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * P2 stand-in for the real permission code (P3). Walks every card through all 4 states without a real OS dialog:
 *
 * ```
 *  BLE Scan / Connect : NOT_REQUESTED ─Request─► GRANTED
 *  Camera             : NOT_REQUESTED ─Request─► DENIED ─Request─► PERMANENTLY_DENIED ─Open settings─► GRANTED
 *  Radio              : OFF ─Turn on─► ON
 * ```
 * Reset asked flags puts everything back to the start, so the Scanner's "not ready" card can be checked again.
 */
class FakePermissionController : PermissionController {

    override val groups = GROUPS
    override val states = MutableStateFlow(initialStates())
    override val lastResult = MutableStateFlow("")
    override val radio = MutableStateFlow(BluetoothRadioState.OFF)

    override fun request(group: PermissionGroup) {
        val next = when (group.id) {
            PermissionGroupId.CAMERA -> when (statusOf(group.id)) {
                PermissionStatus.NOT_REQUESTED -> PermissionStatus.DENIED
                else -> PermissionStatus.PERMANENTLY_DENIED
            }
            else -> PermissionStatus.GRANTED
        }
        set(group, next)
        lastResult.value = group.runtimePermissions.joinToString { "$it=${next == PermissionStatus.GRANTED}" }
    }

    override fun requestAll() = groups.filter { statusOf(it.id) != PermissionStatus.GRANTED }.forEach(::request)

    /** The user "went to Settings and switched everything on" — in real life we only learn this in [refresh]. */
    override fun openSettings() {
        groups.forEach { set(it, PermissionStatus.GRANTED) }
        lastResult.value = "Settings (fake) → all granted"
    }

    override fun resetAskedFlags() {
        states.value = initialStates()
        radio.value = BluetoothRadioState.OFF
        lastResult.value = ""
    }

    override fun turnOnBluetooth() {
        radio.value = BluetoothRadioState.ON
    }

    override fun refresh() = Unit

    private fun statusOf(id: String) = states.value[id]?.status ?: PermissionStatus.NOT_REQUESTED

    private fun set(group: PermissionGroup, status: PermissionStatus) = states.update {
        it + (group.id to PermissionGroupState(status, group.runtimePermissions.associateWith { status }))
    }

    private fun initialStates() = GROUPS.associate {
        it.id to PermissionGroupState(PermissionStatus.NOT_REQUESTED, it.runtimePermissions.associateWith { PermissionStatus.NOT_REQUESTED })
    }

    companion object {
        /** Same shape as Android API 31+ — the richest case (2 BLE groups + camera). */
        val GROUPS = listOf(
            PermissionGroup(PermissionGroupId.BLE_SCAN, listOf("BLUETOOTH_SCAN"), installTimeNote = "BLUETOOTH, BLUETOOTH_ADMIN"),
            PermissionGroup(PermissionGroupId.BLE_CONNECT, listOf("BLUETOOTH_CONNECT")),
            PermissionGroup(PermissionGroupId.CAMERA, listOf("CAMERA")),
        )
    }
}
