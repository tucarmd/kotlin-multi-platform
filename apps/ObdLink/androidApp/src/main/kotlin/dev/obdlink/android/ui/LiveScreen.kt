package dev.obdlink.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.obdlink.android.R
import dev.obdlink.model.ConnectionState
import dev.obdlink.model.ObdState

/**
 * [App 5 · B5] Live tab: three gauges (RPM, km/h, °C), refresh rate, start / pause.
 * Shown while Reconnecting too, so the user sees the gauges empty out instead of a frozen value.
 * Its iOS twin is LiveView.swift.
 */
@Composable
fun LiveScreen(state: ObdState, onStart: () -> Unit, onPause: () -> Unit, modifier: Modifier = Modifier) {
    val connection = state.connection
    if (!state.isConnected && connection !is ConnectionState.Reconnecting) {
        ConnectFirst(modifier)
        return
    }
    val live = state.live
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(connection.label(), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Gauge(stringResource(R.string.live_rpm), stringResource(R.string.unit_rpm), live.rpm, 0, 8000, Modifier.weight(1f))
            Gauge(stringResource(R.string.live_speed), stringResource(R.string.unit_kmh), live.speedKmh, 0, 240, Modifier.weight(1f))
        }
        Gauge(
            stringResource(R.string.live_coolant),
            stringResource(R.string.unit_celsius),
            live.coolantC,
            -40,
            140,
            Modifier.fillMaxWidth(0.5f),
        )
        Text(stringResource(R.string.live_refresh, "%.1f".format(live.refreshHz)), style = MaterialTheme.typography.bodyMedium)
        when (connection) {
            is ConnectionState.Polling -> OutlinedButton(onClick = onPause) { Text(stringResource(R.string.live_pause)) }
            is ConnectionState.Ready -> Button(onClick = onStart) { Text(stringResource(R.string.live_start)) }
            else -> Unit // Reconnecting: polling resumes by itself
        }
    }
}
