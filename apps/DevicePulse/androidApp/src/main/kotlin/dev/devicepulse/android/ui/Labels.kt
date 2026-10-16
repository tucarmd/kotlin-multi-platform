package dev.devicepulse.android.ui

import androidx.compose.ui.res.stringResource
import dev.devicepulse.android.R
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.ConnectionType
import dev.devicepulse.model.SensorKind
import dev.devicepulse.rules.BatteryLevel
import dev.devicepulse.rules.FixQuality
import dev.devicepulse.rules.LatencyGrade

enum class Tone { Good, Warn, Bad }

/** A rule result ready to draw: its label and how alarming it is. */
data class Grade(@StringRes val label: Int, val tone: Tone)

fun BatteryLevel.grade() = when (this) {
    BatteryLevel.Critical -> Grade(R.string.grade_critical, Tone.Bad)
    BatteryLevel.Low -> Grade(R.string.grade_low, Tone.Warn)
    BatteryLevel.Ok -> Grade(R.string.grade_ok, Tone.Good)
}

fun FixQuality.grade() = when (this) {
    FixQuality.Good -> Grade(R.string.grade_good, Tone.Good)
    FixQuality.Fair -> Grade(R.string.grade_fair, Tone.Warn)
    FixQuality.Poor -> Grade(R.string.grade_poor, Tone.Bad)
}

fun LatencyGrade.grade() = when (this) {
    LatencyGrade.Fast -> Grade(R.string.grade_fast, Tone.Good)
    LatencyGrade.Ok -> Grade(R.string.grade_ok, Tone.Warn)
    LatencyGrade.Slow -> Grade(R.string.grade_slow, Tone.Bad)
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
        ChargingState.Charging -> R.string.charging_charging
        ChargingState.Discharging -> R.string.charging_discharging
        ChargingState.Full -> R.string.charging_full
        ChargingState.Unknown -> R.string.charging_unknown
    }
)

@Composable
fun ConnectionType.label(): String = stringResource(
    when (this) {
        ConnectionType.Wifi -> R.string.conn_wifi
        ConnectionType.Cellular -> R.string.conn_cellular
        ConnectionType.Ethernet -> R.string.conn_ethernet
        ConnectionType.None -> R.string.conn_none
        ConnectionType.Unknown -> R.string.conn_unknown
    }
)

@Composable
fun SensorKind.title(): String = stringResource(
    when (this) {
        SensorKind.Battery -> R.string.sensor_battery
        SensorKind.Location -> R.string.sensor_location
        SensorKind.Network -> R.string.sensor_network
    }
)

val SensorKind.emoji: String
    get() = when (this) {
        SensorKind.Battery -> "🔋"
        SensorKind.Location -> "📍"
        SensorKind.Network -> "📶"
    }
