package dev.triplog.rules

import dev.triplog.model.TrackPoint
import dev.triplog.model.TripStats

/**
 * [App 4 · B1] Trip statistics from a list of points. Pure functions, no I/O.
 *
 * Why everything is computed from the points (and segments) instead of being tracked with timers:
 * the same function gives the live numbers, the saved numbers and the numbers in tests.
 *
 * Called by: TripRecorder (when saving), TripLibrary (speed chart), both UIs through those. Tested by: RulesTest.
 */

/**
 * Distance, moving time and speeds for [points]. Pairs that straddle a pause (different `segment`) are skipped,
 * so neither the distance nor the time spent paused is counted.
 *
 * Example: segment 0 = 2 points 100 m / 10 s apart, then a 10-minute pause, then segment 1 = 2 points
 * 100 m / 10 s apart → distance 200 m, moving 20 s, average 10 m/s (not 200 m / 620 s).
 */
fun tripStats(points: List<TrackPoint>): TripStats {
    var distance = 0.0
    var movingMillis = 0L
    var maxSpeed = 0.0
    for (i in points.indices) {
        val point = points[i]
        point.speedMps?.let { if (it > maxSpeed) maxSpeed = it }
        val prev = points.getOrNull(i - 1) ?: continue
        if (prev.segment != point.segment) continue
        val step = distanceMeters(prev, point)
        val dt = point.timeMillis - prev.timeMillis
        distance += step
        movingMillis += dt
        // Fall back to the computed speed when the chip did not report one.
        if (point.speedMps == null && dt > 0) maxSpeed = maxOf(maxSpeed, step / (dt / 1000.0))
    }
    val average = if (movingMillis > 0) distance / (movingMillis / 1000.0) else 0.0
    return TripStats(distanceMeters = distance, movingMillis = movingMillis, averageSpeedMps = average, maxSpeedMps = maxSpeed)
}

/**
 * Speed of each point in m/s (reported by the chip, else computed from the previous point in the same segment,
 * else 0), averaged down to at most [maxSamples] values so a chart stays readable on a 3-hour trip.
 *
 * Example: 4 points with speeds 1, 3, 5, 7 and maxSamples = 2 → [2.0, 6.0].
 */
fun speedSeries(points: List<TrackPoint>, maxSamples: Int = 60): List<Double> {
    if (points.isEmpty() || maxSamples <= 0) return emptyList()
    val speeds = points.mapIndexed { i, point ->
        point.speedMps ?: points.getOrNull(i - 1)?.takeIf { it.segment == point.segment }?.let { prev ->
            val dt = (point.timeMillis - prev.timeMillis) / 1000.0
            if (dt > 0) distanceMeters(prev, point) / dt else 0.0
        } ?: 0.0
    }
    if (speeds.size <= maxSamples) return speeds
    // Bucket i covers speeds[start until end]; buckets differ by at most one element.
    return List(maxSamples) { bucket ->
        val start = bucket * speeds.size / maxSamples
        val end = (bucket + 1) * speeds.size / maxSamples
        speeds.subList(start, end).average()
    }
}
