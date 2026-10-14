package dev.devicepulse.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong

/** Fixed-decimal formatting without String.format, which commonMain does not have. */
fun Double.fixed(decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(this) * factor).roundToLong()
    val whole = scaled / factor
    val sign = if (this < 0 && scaled != 0L) "-" else ""
    if (decimals == 0) return "$sign$whole"
    val frac = (scaled % factor).toString().padStart(decimals, '0')
    return "$sign$whole.$frac"
}

fun kotlin.time.Instant.toClockText(): String {
    val t = toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.two() = toString().padStart(2, '0')
    return "${t.hour.two()}:${t.minute.two()}:${t.second.two()} · ${t.day.two()}/${t.month.number.two()}/${t.year}"
}
