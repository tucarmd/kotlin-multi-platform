package com.learn.ndmkmp.presentation.permission

import com.learn.ndmkmp.core.bluetooth.BleReadiness
import com.learn.ndmkmp.core.bluetooth.BluetoothRadioState
import com.learn.ndmkmp.core.bluetooth.bleReadiness
import com.learn.ndmkmp.core.permission.PermissionController
import com.learn.ndmkmp.core.permission.PermissionGroup
import com.learn.ndmkmp.core.permission.PermissionGroupId
import com.learn.ndmkmp.core.permission.PermissionStatus
import com.learn.ndmkmp.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

/** The one button a permission card shows (Day 1: exactly 1 action per state). */
enum class PermissionAction { NONE, REQUEST, OPEN_SETTINGS }

/** One card = one feature. Everything already decided — the View only draws. */
data class PermissionCardUi(
    val groupId: String,
    val title: String,
    val why: String,
    val status: PermissionStatus,
    /** Real permission names on this device, one per line (or "Nothing to request…"). */
    val permissionNames: String,
    val installTimeNote: String?,
    /** RATIONALE / PERMANENTLY_DENIED_HINT, null = no extra line. */
    val hint: String?,
    val action: PermissionAction,
)

data class PermissionUiState(
    val cards: List<PermissionCardUi> = emptyList(),
    /** "Last result: BLUETOOTH_SCAN=true", null = nothing asked yet. */
    val lastResult: String? = null,
    val radio: BluetoothRadioState = BluetoothRadioState.OFF,
    val readiness: BleReadiness = BleReadiness.MISSING_PERMISSION,
    val readinessText: String = "",
    /** Only when tapping it DOES something: radio off and permissions granted. */
    val showTurnOnBluetooth: Boolean = false,
)

/**
 * Day 1 — Permissions tab. Wraps [PermissionController] (platform) and turns its raw states into cards.
 * [readiness] is also read by the Scanner tab: one "can we scan?" verdict for the whole app.
 */
class PermissionViewModel(private val controller: PermissionController) : SharedViewModel() {

    private val _state = MutableStateFlow(build())
    val state: StateFlow<PermissionUiState> = _state.asStateFlow()

    private val _readiness = MutableStateFlow(_state.value.readiness)
    val readiness: StateFlow<BleReadiness> = _readiness.asStateFlow()

    init {
        // Any of the 3 platform flows changes → rebuild the whole state from their current values.
        merge(controller.states.map { }, controller.lastResult.map { }, controller.radio.map { })
            .onEach {
                _state.value = build()
                _readiness.value = _state.value.readiness
            }
            .launchIn(scope)
    }

    fun request(groupId: String) {
        controller.groups.firstOrNull { it.id == groupId }?.let(controller::request)
    }

    fun requestAll() = controller.requestAll()
    fun openSettings() = controller.openSettings()
    fun resetAskedFlags() = controller.resetAskedFlags()
    fun turnOnBluetooth() = controller.turnOnBluetooth()

    /** App back in the foreground (from Settings): no event fires for permission changes → re-read. */
    fun refresh() = controller.refresh()

    private fun build(): PermissionUiState {
        val states = controller.states.value
        val bleGranted = listOf(PermissionGroupId.BLE_SCAN, PermissionGroupId.BLE_CONNECT).all { states[it]?.isGranted == true }
        val radio = controller.radio.value
        val readiness = bleReadiness(radio, bleGranted)
        return PermissionUiState(
            cards = controller.groups.map { card(it, states[it.id]?.status ?: PermissionStatus.NOT_REQUESTED) },
            lastResult = controller.lastResult.value.takeIf { it.isNotEmpty() }?.let(PermissionStrings::lastResultFormat),
            radio = radio,
            readiness = readiness,
            readinessText = when (readiness) {
                BleReadiness.READY -> PermissionStrings.READY_YES
                BleReadiness.MISSING_PERMISSION -> PermissionStrings.READY_NO_PERMISSION
                BleReadiness.RADIO_OFF -> PermissionStrings.READY_RADIO_OFF
                BleReadiness.UNSUPPORTED -> PermissionStrings.READY_UNSUPPORTED
            },
            showTurnOnBluetooth = readiness == BleReadiness.RADIO_OFF,
        )
    }

    private fun card(group: PermissionGroup, status: PermissionStatus): PermissionCardUi {
        val nothingToAsk = group.runtimePermissions.isEmpty()
        return PermissionCardUi(
            groupId = group.id,
            title = groupLabel(group.id),
            why = groupWhy(group.id),
            status = status,
            // The REAL names on this device: API 33 shows BLUETOOTH_SCAN, API 30 shows ACCESS_FINE_LOCATION. Same card.
            permissionNames = if (nothingToAsk) PermissionStrings.NONE_NEEDED
            else group.runtimePermissions.joinToString("\n") { it.substringAfterLast('.') },
            installTimeNote = group.installTimeNote.takeIf { it.isNotEmpty() }?.let(PermissionStrings::installTimeFormat),
            hint = when (status) {
                PermissionStatus.DENIED -> PermissionStrings.RATIONALE
                PermissionStatus.PERMANENTLY_DENIED -> PermissionStrings.PERMANENTLY_DENIED_HINT
                else -> null
            },
            action = when (status) {
                // Granted → no button: "Request" now shows nothing, the user would think it's broken.
                PermissionStatus.GRANTED -> PermissionAction.NONE
                PermissionStatus.DENIED -> PermissionAction.REQUEST
                // The dialog is dead → only Settings is left.
                PermissionStatus.PERMANENTLY_DENIED -> PermissionAction.OPEN_SETTINGS
                PermissionStatus.NOT_REQUESTED -> if (nothingToAsk) PermissionAction.NONE else PermissionAction.REQUEST
            },
        )
    }
}

/** Label of the status chip. */
fun statusLabel(status: PermissionStatus): String = when (status) {
    PermissionStatus.GRANTED -> PermissionStrings.STATUS_GRANTED
    PermissionStatus.DENIED -> PermissionStrings.STATUS_DENIED
    PermissionStatus.PERMANENTLY_DENIED -> PermissionStrings.STATUS_PERMANENTLY_DENIED
    PermissionStatus.NOT_REQUESTED -> PermissionStrings.STATUS_NOT_REQUESTED
}

/** Label of the radio chip. */
fun radioLabel(radio: BluetoothRadioState): String = when (radio) {
    BluetoothRadioState.ON -> PermissionStrings.RADIO_ON
    BluetoothRadioState.OFF -> PermissionStrings.RADIO_OFF
    BluetoothRadioState.UNSUPPORTED -> PermissionStrings.RADIO_UNSUPPORTED
}
