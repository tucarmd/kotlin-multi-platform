package dev.triplog

import dev.triplog.model.FixVerdict
import dev.triplog.rules.acceptFix
import dev.triplog.rules.distanceMeters
import dev.triplog.rules.formatDistance
import dev.triplog.rules.formatDuration
import dev.triplog.rules.formatFixed
import dev.triplog.rules.formatSpeedKmh
import dev.triplog.rules.judgeFix
import dev.triplog.rules.speedSeries
import dev.triplog.rules.tripStats
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [App 4 · B7] Pure rules: haversine, noise filter, stats, formatting. No coroutines, no fakes.
 */
class RulesTest {

    @Test
    fun `Ha Noi to Ho Chi Minh City is about 1137 km within 1 percent`() {
        val meters = distanceMeters(21.0285, 105.8542, 10.7769, 106.7009)
        assertTrue(abs(meters - 1_137_000) / 1_137_000 < 0.01, "got $meters")
    }

    @Test
    fun `distance is symmetric and zero for the same point`() {
        val there = distanceMeters(21.0285, 105.8542, 10.7769, 106.7009)
        val back = distanceMeters(10.7769, 106.7009, 21.0285, 105.8542)
        assertEquals(there, back, 1e-6)
        assertEquals(0.0, distanceMeters(21.0, 105.0, 21.0, 105.0))
    }

    @Test
    fun `one thousandth of a degree of latitude is about 111 m`() {
        assertEquals(111.2, distanceMeters(fix(0), fix(1, northMeters = 111.195)), 0.1)
    }

    @Test
    fun `a 500 m jump in 1 s is rejected as too fast`() {
        assertEquals(FixVerdict.TooFast, judgeFix(fix(0), fix(1, northMeters = 500.0)))
    }

    @Test
    fun `the same 500 m in 60 s is a car and is accepted`() {
        assertTrue(acceptFix(fix(0), fix(60, northMeters = 500.0)))
    }

    @Test
    fun `a fix wider than 30 m is rejected even as the first one`() {
        assertEquals(FixVerdict.TooInaccurate, judgeFix(null, fix(0, accuracy = 45.0)))
        assertTrue(acceptFix(null, fix(0, accuracy = 30.0)))
    }

    @Test
    fun `a fix with the same timestamp is out of order`() {
        assertEquals(FixVerdict.OutOfOrder, judgeFix(fix(5), fix(5, northMeters = 1.0)))
        assertFalse(acceptFix(fix(5), fix(4)))
    }

    @Test
    fun `stats skip the paused gap between segments`() {
        val points = listOf(
            fix(0, 0.0),
            fix(10, 100.0),
            // 10-minute pause, then the user is 5 km further on
            fix(610, 5_000.0).copy(segment = 1),
            fix(620, 5_100.0).copy(segment = 1),
        )
        val stats = tripStats(points)
        assertEquals(200.0, stats.distanceMeters, 0.5)
        assertEquals(20_000, stats.movingMillis)
        assertEquals(10.0, stats.averageSpeedMps, 0.05)
    }

    @Test
    fun `max speed prefers the speed reported by the chip`() {
        val stats = tripStats(listOf(fix(0, speed = 3.0), fix(10, 10.0, speed = 7.5), fix(20, 20.0, speed = 2.0)))
        assertEquals(7.5, stats.maxSpeedMps)
    }

    @Test
    fun `stats of an empty or single point list are zero`() {
        assertEquals(0.0, tripStats(emptyList()).distanceMeters)
        assertEquals(0.0, tripStats(listOf(fix(0))).averageSpeedMps)
    }

    @Test
    fun `speed series is averaged down to the sample limit`() {
        val points = listOf(1.0, 3.0, 5.0, 7.0).mapIndexed { i, s -> fix(i.toLong(), speed = s) }
        assertEquals(listOf(2.0, 6.0), speedSeries(points, maxSamples = 2))
        assertEquals(listOf(1.0, 3.0, 5.0, 7.0), speedSeries(points))
    }

    @Test
    fun `fixed formatting never uses scientific notation`() {
        assertEquals("0.0001000", formatFixed(0.0001, 7))
        assertEquals("3.14", formatFixed(3.14159, 2))
        assertEquals("-2", formatFixed(-1.5, 0))
        assertEquals("106.7009000", formatFixed(106.7009, 7))
        assertEquals("0.0", formatFixed(-0.01, 1))
    }

    @Test
    fun `distance duration and speed print the same on both platforms`() {
        assertEquals("850 m", formatDistance(850.4))
        assertEquals("1.23 km", formatDistance(1234.0))
        assertEquals("0:00", formatDuration(0))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
        assertEquals("36.0", formatSpeedKmh(10.0))
    }
}
