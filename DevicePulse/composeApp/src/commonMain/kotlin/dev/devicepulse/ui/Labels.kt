package dev.devicepulse.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.ConnectionType
import dev.devicepulse.model.SensorKind
import dev.devicepulse.resources.Res
import dev.devicepulse.resources.charging_charging
import dev.devicepulse.resources.charging_discharging
import dev.devicepulse.resources.charging_full
import dev.devicepulse.resources.charging_unknown
import dev.devicepulse.resources.conn_cellular
import dev.devicepulse.resources.conn_ethernet
import dev.devicepulse.resources.conn_none
import dev.devicepulse.resources.conn_unknown
import dev.devicepulse.resources.conn_wifi
import dev.devicepulse.resources.grade_critical
import dev.devicepulse.resources.grade_fair
import dev.devicepulse.resources.grade_fast
import dev.devicepulse.resources.grade_good
import dev.devicepulse.resources.grade_low
import dev.devicepulse.resources.grade_ok
import dev.devicepulse.resources.grade_poor
import dev.devicepulse.resources.grade_slow
import dev.devicepulse.resources.sensor_battery
import dev.devicepulse.resources.sensor_location
import dev.devicepulse.resources.sensor_network
import dev.devicepulse.rules.BatteryLevel
import dev.devicepulse.rules.FixQuality
import dev.devicepulse.rules.LatencyGrade
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class Tone { Good, Warn, Bad }

/** A rule result ready to draw: its label and how alarming it is. */
data class Grade(val label: StringResource, val tone: Tone)

fun BatteryLevel.grade() = when (this) {
    BatteryLevel.Critical -> Grade(Res.string.grade_critical, Tone.Bad)
    BatteryLevel.Low -> Grade(Res.string.grade_low, Tone.Warn)
    BatteryLevel.Ok -> Grade(Res.string.grade_ok, Tone.Good)
}

fun FixQuality.grade() = when (this) {
    FixQuality.Good -> Grade(Res.string.grade_good, Tone.Good)
    FixQuality.Fair -> Grade(Res.string.grade_fair, Tone.Warn)
    FixQuality.Poor -> Grade(Res.string.grade_poor, Tone.Bad)
}

fun LatencyGrade.grade() = when (this) {
    LatencyGrade.Fast -> Grade(Res.string.grade_fast, Tone.Good)
    LatencyGrade.Ok -> Grade(Res.string.grade_ok, Tone.Warn)
    LatencyGrade.Slow -> Grade(Res.string.grade_slow, Tone.Bad)
}

@Composable
fun Tone.color(): Color = when (this) {
    Tone.Good -> Color(0xFF2E7D32)
    Tone.Warn -> Color(0xFFB26A00)
    Tone.Bad -> MaterialTheme.colorScheme.error
}

@Composable
fun ChargingState.label(): String = stringResource(
    when (this) {
        ChargingState.Charging -> Res.string.charging_charging
        ChargingState.Discharging -> Res.string.charging_discharging
        ChargingState.Full -> Res.string.charging_full
        ChargingState.Unknown -> Res.string.charging_unknown
    }
)

@Composable
fun ConnectionType.label(): String = stringResource(
    when (this) {
        ConnectionType.Wifi -> Res.string.conn_wifi
        ConnectionType.Cellular -> Res.string.conn_cellular
        ConnectionType.Ethernet -> Res.string.conn_ethernet
        ConnectionType.None -> Res.string.conn_none
        ConnectionType.Unknown -> Res.string.conn_unknown
    }
)

@Composable
fun SensorKind.title(): String = stringResource(
    when (this) {
        SensorKind.Battery -> Res.string.sensor_battery
        SensorKind.Location -> Res.string.sensor_location
        SensorKind.Network -> Res.string.sensor_network
    }
)

val SensorKind.emoji: String
    get() = when (this) {
        SensorKind.Battery -> "🔋"
        SensorKind.Location -> "📍"
        SensorKind.Network -> "📶"
    }
