package dev.devicepulse.android.ui

import androidx.compose.ui.res.stringResource
import dev.devicepulse.android.R
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.devicepulse.core.DashboardState
import dev.devicepulse.model.Reading
import dev.devicepulse.model.SensorKind
import dev.devicepulse.model.valueOrNull
import dev.devicepulse.rules.batteryLevel
import dev.devicepulse.rules.fixQuality
import dev.devicepulse.rules.latencyGrade
import kotlinx.coroutines.launch

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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("🕘 " + stringResource(R.string.history)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(
                onClick = {
                    onSave()
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.saved)) }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(R.string.save_snapshot)) }
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
                    stringResource(if (it.isOnline) R.string.online else R.string.offline),
                    color = (if (it.isOnline) Tone.Good else Tone.Bad).color(),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(latencyText(latency), modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onMeasure, enabled = latency != Reading.Loading) {
                Text(stringResource(R.string.measure_latency))
            }
        }
    }
}

@Composable
fun latencyText(latency: Reading<Long>?): String = when (latency) {
    null -> stringResource(R.string.not_measured)
    Reading.Loading -> stringResource(R.string.measuring)
    is Reading.Available -> "${latency.value} ms"
    is Reading.Error -> stringResource(R.string.state_error, latency.message)
    else -> stringResource(R.string.not_measured)
}
