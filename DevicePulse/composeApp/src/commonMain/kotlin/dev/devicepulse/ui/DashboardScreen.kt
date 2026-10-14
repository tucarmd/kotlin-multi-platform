package dev.devicepulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.devicepulse.model.Reading
import dev.devicepulse.model.SensorKind
import dev.devicepulse.model.valueOrNull
import dev.devicepulse.resources.Res
import dev.devicepulse.resources.app_name
import dev.devicepulse.resources.history
import dev.devicepulse.resources.measure_latency
import dev.devicepulse.resources.measuring
import dev.devicepulse.resources.not_measured
import dev.devicepulse.resources.offline
import dev.devicepulse.resources.online
import dev.devicepulse.resources.save_snapshot
import dev.devicepulse.resources.saved
import dev.devicepulse.resources.state_error
import dev.devicepulse.rules.batteryLevel
import dev.devicepulse.rules.fixQuality
import dev.devicepulse.rules.latencyGrade
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onOpenDetail: (SensorKind) -> Unit,
    onOpenHistory: () -> Unit,
    onMeasureLatency: () -> Unit,
    onSave: () -> Unit,
    onGrantLocation: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("🕘 " + stringResource(Res.string.history)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(
                onClick = {
                    onSave()
                    scope.launch { snackbar.showSnackbar(getString(Res.string.saved)) }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(Res.string.save_snapshot)) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { BatteryCard(state, onClick = { onOpenDetail(SensorKind.Battery) }) }
            item { LocationCard(state, onClick = { onOpenDetail(SensorKind.Location) }, onGrantLocation) }
            item { NetworkCard(state, onClick = { onOpenDetail(SensorKind.Network) }, onMeasureLatency) }
        }
    }
}

@Composable
private fun BatteryCard(state: DashboardState, onClick: () -> Unit) {
    val info = state.battery.valueOrNull()
    SensorCard(
        emoji = SensorKind.Battery.emoji,
        title = SensorKind.Battery.title(),
        onClick = onClick,
        grade = info?.let { batteryLevel(it).grade() },
    ) {
        ReadingContent(state.battery) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${it.levelPercent}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(12.dp))
                Text(it.charging.label(), modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

@Composable
private fun LocationCard(state: DashboardState, onClick: () -> Unit, onGrant: () -> Unit) {
    val fix = state.location.valueOrNull()
    SensorCard(
        emoji = SensorKind.Location.emoji,
        title = SensorKind.Location.title(),
        onClick = onClick,
        grade = fix?.let { fixQuality(it).grade() },
    ) {
        ReadingContent(state.location, onGrantPermission = onGrant) {
            Text("${it.latitude.fixed(5)}, ${it.longitude.fixed(5)}", style = MaterialTheme.typography.titleLarge)
            Text("± ${it.accuracyMeters.fixed(0)} m", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NetworkCard(state: DashboardState, onClick: () -> Unit, onMeasure: () -> Unit) {
    val latency = state.latencyMs
    SensorCard(
        emoji = SensorKind.Network.emoji,
        title = SensorKind.Network.title(),
        onClick = onClick,
        grade = latency?.valueOrNull()?.let { latencyGrade(it).grade() },
    ) {
        ReadingContent(state.network) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(it.type.label(), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(12.dp))
                Text(
                    stringResource(if (it.isOnline) Res.string.online else Res.string.offline),
                    color = (if (it.isOnline) Tone.Good else Tone.Bad).color(),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(latencyText(latency), modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onMeasure, enabled = latency != Reading.Loading) {
                Text(stringResource(Res.string.measure_latency))
            }
        }
    }
}

@Composable
fun latencyText(latency: Reading<Long>?): String = when (latency) {
    null -> stringResource(Res.string.not_measured)
    Reading.Loading -> stringResource(Res.string.measuring)
    is Reading.Available -> "${latency.value} ms"
    is Reading.Error -> stringResource(Res.string.state_error, latency.message)
    else -> stringResource(Res.string.not_measured)
}
