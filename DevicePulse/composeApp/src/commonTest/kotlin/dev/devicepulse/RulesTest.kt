package dev.devicepulse

import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.LocationFix
import dev.devicepulse.rules.BatteryLevel
import dev.devicepulse.rules.FixQuality
import dev.devicepulse.rules.LatencyGrade
import dev.devicepulse.rules.batteryLevel
import dev.devicepulse.rules.fixQuality
import dev.devicepulse.rules.latencyGrade
import dev.devicepulse.ui.fixed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RulesTest {

    private fun battery(percent: Int) = BatteryInfo(percent, ChargingState.Discharging)
    private fun fix(accuracy: Double) = LocationFix(0.0, 0.0, accuracy, null, Instant.fromEpochMilliseconds(0))

    @Test
    fun batteryLevelBoundaries() {
        assertEquals(BatteryLevel.Critical, batteryLevel(battery(0)))
        assertEquals(BatteryLevel.Critical, batteryLevel(battery(10)))
        assertEquals(BatteryLevel.Low, batteryLevel(battery(11)))
        assertEquals(BatteryLevel.Low, batteryLevel(battery(20)))
        assertEquals(BatteryLevel.Ok, batteryLevel(battery(21)))
        assertEquals(BatteryLevel.Ok, batteryLevel(battery(100)))
    }

    @Test
    fun fixQualityBoundaries() {
        assertEquals(FixQuality.Good, fixQuality(fix(5.0)))
        assertEquals(FixQuality.Good, fixQuality(fix(20.0)))
        assertEquals(FixQuality.Fair, fixQuality(fix(20.1)))
        assertEquals(FixQuality.Fair, fixQuality(fix(100.0)))
        assertEquals(FixQuality.Poor, fixQuality(fix(100.1)))
    }

    @Test
    fun latencyGradeBoundaries() {
        assertEquals(LatencyGrade.Fast, latencyGrade(0))
        assertEquals(LatencyGrade.Fast, latencyGrade(99))
        assertEquals(LatencyGrade.Ok, latencyGrade(100))
        assertEquals(LatencyGrade.Ok, latencyGrade(299))
        assertEquals(LatencyGrade.Slow, latencyGrade(300))
    }

    @Test
    fun fixedFormatsDecimals() {
        assertEquals("10.77000", 10.77.fixed(5))
        assertEquals("-0.5", (-0.5).fixed(1))
        assertEquals("0.0", (-0.01).fixed(1))
        assertEquals("106.700001", 106.700001.fixed(6))
        assertEquals("13", 12.6.fixed(0))
    }
}
