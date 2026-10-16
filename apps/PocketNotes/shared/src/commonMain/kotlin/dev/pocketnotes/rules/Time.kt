package dev.pocketnotes.rules

import dev.pocketnotes.model.RelativeTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * [App 2 · B1] "Edited 5 min ago" as a pure function.
 *
 * Why [now] and [timeZone] are parameters: a rule that called Clock.System.now() itself would give a different answer
 * on every test run, and "yesterday" depends on where the phone is. The core passes the real values in;
 * tests pass fixed ones.
 *
 * Called by: NotesCore. Calls: kotlinx-datetime (Instant → LocalDate in [timeZone]).
 */

/**
 * How long before [now] the instant [from] was, as a case the UI can turn into text.
 *
 * Examples (time zone UTC, now = 2026-10-15 12:00):
 * 11:59:30 → JustNow · 11:55 → MinutesAgo(5) · 09:00 → HoursAgo(3) · 2026-10-14 23:00 → Yesterday ·
 * 2026-10-12 → DaysAgo(3) · 2026-09-01 → OnDate(2026, 9, 1) · 12:05 (future) → JustNow.
 */
fun relativeTime(from: Instant, now: Instant, timeZone: TimeZone): RelativeTime {
    // TRAP: never call Clock.System.now() in here. The answer must depend only on the arguments,
    // otherwise RulesTest passes at 10:00 and fails at 00:01. TEST: RulesTest.`relative time uses the now it is given`
    val elapsed = now - from
    if (elapsed < 1.minutes) return RelativeTime.JustNow // also covers a clock that moved backwards
    if (elapsed < 1.hours) return RelativeTime.MinutesAgo(elapsed.inWholeMinutes.toInt())

    // TRAP: "yesterday" is about calendar days in the phone's time zone, not "24 hours ago".
    // 23:00 → 01:00 next day is only 2 hours, but it is yesterday.
    val fromDate = from.toLocalDateTime(timeZone).date
    val today = now.toLocalDateTime(timeZone).date
    return when (val days = fromDate.daysUntil(today)) {
        0 -> RelativeTime.HoursAgo(elapsed.inWholeHours.toInt())
        1 -> RelativeTime.Yesterday
        in 2..6 -> RelativeTime.DaysAgo(days)
        else -> RelativeTime.OnDate(fromDate.year, fromDate.month.ordinal + 1, fromDate.day)
    }
}
