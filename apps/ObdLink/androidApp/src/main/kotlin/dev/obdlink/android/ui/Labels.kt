package dev.obdlink.android.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.obdlink.android.R
import dev.obdlink.model.CodesFailure
import dev.obdlink.model.ConnectionState
import dev.obdlink.model.ConsoleKind
import dev.obdlink.model.ObdError
import dev.obdlink.obd.model.Dongle
import dev.obdlink.obd.model.DtcSystem

/**
 * [App 5 · B5] Core enums and states → text. The core never holds UI text, so each UI owns this mapping.
 * Its iOS twin is Labels.swift, with the same keys.
 */
@StringRes
fun ObdError.label(): Int = when (this) {
    ObdError.BluetoothOff -> R.string.error_bluetooth_off
    ObdError.ScanFailed -> R.string.error_scan_failed
    ObdError.ConnectFailed -> R.string.error_connect_failed
    ObdError.SetupFailed -> R.string.error_setup_failed
    ObdError.ReconnectFailed -> R.string.error_reconnect_failed
}

@StringRes
fun CodesFailure.label(): Int = when (this) {
    CodesFailure.NoAnswer -> R.string.codes_failed_no_answer
    CodesFailure.Rejected -> R.string.codes_failed_rejected
}

@StringRes
fun DtcSystem.label(): Int = when (this) {
    DtcSystem.Powertrain -> R.string.system_powertrain
    DtcSystem.Chassis -> R.string.system_chassis
    DtcSystem.Body -> R.string.system_body
    DtcSystem.Network -> R.string.system_network
}

/** Name to show for a dongle: advertised name, or a placeholder when it advertises none. */
@Composable
fun Dongle.displayName(): String = name.ifBlank { stringResource(R.string.dongle_unnamed) }

/** One line describing the connection, e.g. "Reconnecting 2/3…". One branch per state, no `else`. */
@Composable
fun ConnectionState.label(): String = when (this) {
    ConnectionState.Idle -> stringResource(R.string.state_idle)
    ConnectionState.Scanning -> stringResource(R.string.state_scanning)
    is ConnectionState.Connecting -> stringResource(R.string.state_connecting, dongle.displayName())
    is ConnectionState.Initializing -> stringResource(R.string.state_initializing, dongle.displayName())
    is ConnectionState.Ready -> stringResource(R.string.state_ready, dongle.displayName())
    is ConnectionState.Polling -> stringResource(R.string.state_polling, dongle.displayName())
    is ConnectionState.Reconnecting -> stringResource(R.string.state_reconnecting, attempt, maxAttempts)
    is ConnectionState.Error -> stringResource(reason.label())
}

/** Console notes are stored as a kind + the command; the sentence is built here. Sent/Received show raw text. */
@Composable
fun ConsoleKind.format(text: String): String = when (this) {
    ConsoleKind.Sent -> "> $text"
    ConsoleKind.Received -> text
    ConsoleKind.NoAnswer -> stringResource(R.string.console_no_answer, text)
    ConsoleKind.InvalidInput -> stringResource(R.string.console_invalid, text)
    ConsoleKind.ClearBlocked -> stringResource(R.string.console_clear_blocked, text)
    ConsoleKind.NotConnected -> stringResource(R.string.console_not_connected, text)
}
