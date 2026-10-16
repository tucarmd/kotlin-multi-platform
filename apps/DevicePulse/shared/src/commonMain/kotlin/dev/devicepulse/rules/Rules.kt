package dev.devicepulse.rules

import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.LocationFix

enum class BatteryLevel { Critical, Low, Ok }

fun batteryLevel(info: BatteryInfo): BatteryLevel = when {
    info.levelPercent <= 10 -> BatteryLevel.Critical
    info.levelPercent <= 20 -> BatteryLevel.Low
    else -> BatteryLevel.Ok
}

enum class FixQuality { Good, Fair, Poor }

fun fixQuality(fix: LocationFix): FixQuality = when {
    fix.accuracyMeters <= 20.0 -> FixQuality.Good
    fix.accuracyMeters <= 100.0 -> FixQuality.Fair
    else -> FixQuality.Poor
}

enum class LatencyGrade { Fast, Ok, Slow }

fun latencyGrade(millis: Long): LatencyGrade = when {
    millis < 100 -> LatencyGrade.Fast
    millis < 300 -> LatencyGrade.Ok
    else -> LatencyGrade.Slow
}
