package dev.devicepulse.ui

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
import dev.devicepulse.model.Reading
import dev.devicepulse.model.SensorKind
import dev.devicepulse.resources.Res
import dev.devicepulse.resources.back
import dev.devicepulse.resources.field_accuracy
import dev.devicepulse.resources.field_latency
import dev.devicepulse.resources.field_latitude
import dev.devicepulse.resources.field_level
import dev.devicepulse.resources.field_longitude
import dev.devicepulse.resources.field_rating
import dev.devicepulse.resources.field_speed
import dev.devicepulse.resources.field_status
import dev.devicepulse.resources.field_type
import dev.devicepulse.resources.field_updated
import dev.devicepulse.resources.measure_latency
import dev.devicepulse.resources.offline
import dev.devicepulse.resources.online
import dev.devicepulse.resources.rule_battery
import dev.devicepulse.resources.rule_location
import dev.devicepulse.resources.rule_network
import dev.devicepulse.rules.batteryLevel
import dev.devicepulse.rules.fixQuality
import dev.devicepulse.rules.latencyGrade
import org.jetbrains.compose.resources.stringResource

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
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
                    FieldRow(stringResource(Res.string.field_level), "${it.levelPercent}%")
                    FieldRow(stringResource(Res.string.field_status), it.charging.label())
                    FieldRow(stringResource(Res.string.field_rating), stringResource(batteryLevel(it).grade().label))
                }
                SensorKind.Location -> ReadingContent(state.location, onGrantPermission = onGrantLocation) {
                    FieldRow(stringResource(Res.string.field_latitude), it.latitude.fixed(6))
                    FieldRow(stringResource(Res.string.field_longitude), it.longitude.fixed(6))
                    FieldRow(stringResource(Res.string.field_accuracy), "${it.accuracyMeters.fixed(1)} m")
                    FieldRow(stringResource(Res.string.field_speed), it.speedMps?.let { s -> "${(s * 3.6).fixed(1)} km/h" } ?: "—")
                    FieldRow(stringResource(Res.string.field_updated), it.timestamp.toClockText())
                    FieldRow(stringResource(Res.string.field_rating), stringResource(fixQuality(it).grade().label))
                }
                SensorKind.Network -> {
                    ReadingContent(state.network) {
                        FieldRow(stringResource(Res.string.field_type), it.type.label())
                        FieldRow(
                            stringResource(Res.string.field_status),
                            stringResource(if (it.isOnline) Res.string.online else Res.string.offline),
                        )
                    }
                    FieldRow(stringResource(Res.string.field_latency), latencyText(state.latencyMs))
                    (state.latencyMs as? Reading.Available)?.let {
                        FieldRow(stringResource(Res.string.field_rating), stringResource(latencyGrade(it.value).grade().label))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onMeasureLatency, enabled = state.latencyMs != Reading.Loading) {
                        Text(stringResource(Res.string.measure_latency))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(
                    when (sensor) {
                        SensorKind.Battery -> Res.string.rule_battery
                        SensorKind.Location -> Res.string.rule_location
                        SensorKind.Network -> Res.string.rule_network
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
