package dev.obdlink.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.obdlink.android.R
import dev.obdlink.model.ConnectionState
import dev.obdlink.model.ObdState
import dev.obdlink.obd.model.Dongle

/** [App 5 · B5] Everything the Connect tab can do. Grouped so the screen signature stays readable. */
data class ConnectActions(
    val permissionGranted: Boolean,
    val bluetoothOn: Boolean,
    val onAllow: () -> Unit,
    val onTurnOn: () -> Unit,
    val onScan: () -> Unit,
    val onStopScan: () -> Unit,
    val onConnect: (Dongle) -> Unit,
    val onDisconnect: () -> Unit,
    val onReset: () -> Unit,
)

/**
 * [App 5 · B5] Connect tab: permission + Bluetooth status, connection state, scan button, dongle list.
 * Its iOS twin is ConnectView.swift.
 */
@Composable
fun ConnectScreen(state: ObdState, actions: ConnectActions, modifier: Modifier = Modifier) {
    val connection = state.connection
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow(
                    title = stringResource(R.string.permission_title),
                    ok = actions.permissionGranted,
                    okText = stringResource(R.string.permission_granted),
                    badText = stringResource(R.string.permission_missing),
                    fixText = stringResource(R.string.permission_allow),
                    onFix = actions.onAllow,
                )
                StatusRow(
                    title = stringResource(R.string.bluetooth_title),
                    ok = actions.bluetoothOn,
                    okText = stringResource(R.string.bluetooth_on),
                    badText = stringResource(R.string.bluetooth_off),
                    fixText = stringResource(R.string.bluetooth_turn_on),
                    onFix = actions.onTurnOn,
                )
                HorizontalDivider()
                Text(
                    connection.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (connection is ConnectionState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (state.adapterVersion.isNotEmpty() && state.isConnected) {
                    Text(stringResource(R.string.adapter_version, state.adapterVersion), style = MaterialTheme.typography.bodySmall)
                }
                if (connection.isBusy()) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        // One branch per state: which buttons make sense right now.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (connection) {
                ConnectionState.Idle -> Button(onClick = actions.onScan) { Text(stringResource(R.string.scan)) }
                ConnectionState.Scanning -> OutlinedButton(onClick = actions.onStopScan) { Text(stringResource(R.string.stop_scan)) }
                is ConnectionState.Error -> Button(onClick = actions.onReset) { Text(stringResource(R.string.try_again)) }
                is ConnectionState.Connecting, is ConnectionState.Initializing, is ConnectionState.Ready,
                is ConnectionState.Polling, is ConnectionState.Reconnecting,
                -> OutlinedButton(onClick = actions.onDisconnect) { Text(stringResource(R.string.disconnect)) }
            }
        }

        Text(stringResource(R.string.dongles_title), style = MaterialTheme.typography.titleSmall)
        if (state.dongles.isEmpty()) {
            Text(stringResource(R.string.dongles_empty), style = MaterialTheme.typography.bodyMedium)
        }
        val canPick = connection == ConnectionState.Idle || connection == ConnectionState.Scanning
        LazyColumn {
            items(state.dongles, key = { it.id }) { dongle ->
                ListItem(
                    headlineContent = { Text(dongle.displayName()) },
                    // PLATFORM: Android shows a MAC address here, iOS a random per-phone UUID.
                    supportingContent = { Text(dongle.id, style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Text(stringResource(R.string.rssi, dongle.rssi)) },
                    modifier = Modifier.clickable(enabled = canPick) { actions.onConnect(dongle) },
                )
            }
        }
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, badText: String, fixText: String, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Text(
            if (ok) okText else badText,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        if (!ok) TextButton(onClick = onFix) { Text(fixText) }
    }
}

private fun ConnectionState.isBusy() = when (this) {
    ConnectionState.Scanning, is ConnectionState.Connecting, is ConnectionState.Initializing,
    is ConnectionState.Reconnecting,
    -> true
    ConnectionState.Idle, is ConnectionState.Ready, is ConnectionState.Polling, is ConnectionState.Error -> false
}
