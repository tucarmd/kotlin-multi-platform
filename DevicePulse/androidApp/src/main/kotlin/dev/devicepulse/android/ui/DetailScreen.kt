package dev.devicepulse.android.ui

import androidx.compose.ui.res.stringResource
import dev.devicepulse.android.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.devicepulse.core.DashboardState
import dev.devicepulse.model.Reading
import dev.devicepulse.model.SensorKind
import dev.devicepulse.rules.batteryLevel
import dev.devicepulse.rules.fixQuality
import dev.devicepulse.rules.latencyGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sensor: SensorKind,
    state: DashboardState,
    onBack: () -> Unit,
    onMeasureLatency: () -> Unit,
    onGrantLocation: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${sensor.emoji}  ${sensor.title()}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            when (sensor) {
                SensorKind.Battery -> ReadingContent(state.battery) {
                    FieldRow(stringResource(R.string.field_level), "${it.levelPercent}%")
                    FieldRow(stringResource(R.string.field_status), it.charging.label())
                    FieldRow(stringResource(R.string.field_rating), stringResource(batteryLevel(it).grade().label))
                }
                SensorKind.Location -> ReadingContent(state.location, onGrantPermission = onGrantLocation) {
                    FieldRow(stringResource(R.string.field_latitude), it.latitude.fixed(6))
                    FieldRow(stringResource(R.string.field_longitude), it.longitude.fixed(6))
                    FieldRow(stringResource(R.string.field_accuracy), "${it.accuracyMeters.fixed(1)} m")
                    FieldRow(stringResource(R.string.field_speed), it.speedMps?.let { s -> "${(s * 3.6).fixed(1)} km/h" } ?: "—")
                    FieldRow(stringResource(R.string.field_updated), it.timestamp.toClockText())
                    FieldRow(stringResource(R.string.field_rating), stringResource(fixQuality(it).grade().label))
                }
                SensorKind.Network -> {
                    ReadingContent(state.network) {
                        FieldRow(stringResource(R.string.field_type), it.type.label())
                        FieldRow(
                            stringResource(R.string.field_status),
                            stringResource(if (it.isOnline) R.string.online else R.string.offline),
                        )
                    }
                    FieldRow(stringResource(R.string.field_latency), latencyText(state.latencyMs))
                    (state.latencyMs as? Reading.Available)?.let {
                        FieldRow(stringResource(R.string.field_rating), stringResource(latencyGrade(it.value).grade().label))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onMeasureLatency, enabled = state.latencyMs != Reading.Loading) {
                        Text(stringResource(R.string.measure_latency))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(
                    when (sensor) {
                        SensorKind.Battery -> R.string.rule_battery
                        SensorKind.Location -> R.string.rule_location
                        SensorKind.Network -> R.string.rule_network
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
