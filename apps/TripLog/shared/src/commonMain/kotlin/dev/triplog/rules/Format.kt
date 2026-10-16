package dev.triplog.rules

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * [App 4 · B1] Number → text helpers shared by both UIs and GpxWriter, so Android and iOS print the same digits.
 *
 * Why not `String.format`: it does not exist in commonMain. And why not `Double.toString()`: the JVM prints
 * 0.0001 as "1.0E-4", which is not a valid GPX coordinate.
 *
 * Called by: GpxWriter, RecordScreen / TripsScreen / TripDetailScreen, the Swift views. Tested by: RulesTest.
 */

/**
 * [value] with exactly [decimals] digits after the dot, rounded half away from zero. Never scientific notation.
 *
 * Examples: (3.14159, 2) → "3.14" · (0.0001, 7) → "0.0001000" · (-1.5, 0) → "-2" · (2.0, 1) → "2.0".
 */
fun formatFixed(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToLong()
    val whole = scaled / factor
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    if (decimals == 0) return "$sign$whole"
    val fraction = (scaled % factor).toString().padStart(decimals, '0')
    return "$sign$whole.$fraction"
}

/**
 * Distance for display: metres below 1 km, kilometres with 2 decimals above.
 *
 * Examples: 0.0 → "0 m" · 850.4 → "850 m" · 1234.0 → "1.23 km" · 1_137_000.0 → "1137.00 km".
 */
fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.roundToLong()} m" else "${formatFixed(meters / 1000, 2)} km"

/**
 * Duration as a stopwatch: "m:ss" below an hour, "h:mm:ss" above.
 *
 * Examples: 0 → "0:00" · 65_000 → "1:05" · 3_723_000 → "1:02:03".
 */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}

/**
 * m/s → km/h with one decimal, without the unit (the UI adds "km/h").
 *
 * Examples: 0.0 → "0.0" · 10.0 → "36.0" · 1.3889 → "5.0".
 */
fun formatSpeedKmh(metersPerSecond: Double): String = formatFixed(metersPerSecond * 3.6, 1)
