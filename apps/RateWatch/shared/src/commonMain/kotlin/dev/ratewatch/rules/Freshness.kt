package dev.ratewatch.rules

import dev.ratewatch.model.Age
import dev.ratewatch.model.AgeUnit
import dev.ratewatch.model.Freshness
import dev.ratewatch.model.RateTable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * [App 3 · B1] "Is the table on screen new or old?" as pure functions. `now` is always passed in, never read here.
 *
 * Why `now` is a parameter: a rule that calls the clock itself cannot be tested at "exactly one hour".
 * RatesCore reads the clock once and hands the number in.
 *
 * Called by: RatesCore (auto-refresh, state). Calls: nothing.
 */

/** Tables older than this are refreshed automatically when the app opens and shown in red. */
val MAX_AGE: Duration = 1.hours

/**
 * True when the table written at [fetchedAt] is OLDER than [maxAge] at [now] (all epoch ms).
 * Exactly [maxAge] old is still fresh.
 *
 * Examples (maxAge = 1 h): age 59 min → false · age 60 min → false · age 60 min + 1 ms → true · fetchedAt in the future → true.
 */
fun isStale(fetchedAt: Long, now: Long, maxAge: Duration = MAX_AGE): Boolean {
    val age = now - fetchedAt
    // TRAP: a negative age means the phone clock moved backwards (manual change, timezone bug).
    // Treating it as fresh would freeze the table until the clock catches up, so call it stale and refetch.
    if (age < 0) return true
    // TEST: RulesTest.`a table exactly one hour old is still fresh`
    return age > maxAge.inWholeMilliseconds
}

/**
 * Missing / Fresh / Stale for the table on screen.
 *
 * Example: freshnessOf(null, now) → Missing · freshnessOf(table fetched 10 min ago, now) → Fresh.
 */
fun freshnessOf(table: RateTable?, now: Long, maxAge: Duration = MAX_AGE): Freshness = when {
    table == null -> Freshness.Missing
    isStale(table.fetchedAt, now, maxAge) -> Freshness.Stale
    else -> Freshness.Fresh
}

/**
 * How long ago [fetchedAt] was, in the biggest whole unit. Negative ages count as "just now".
 *
 * Examples: 30 s → Age(0, JustNow) · 5 min → Age(5, Minutes) · 2 h 59 min → Age(2, Hours) · 49 h → Age(2, Days).
 */
fun ageOf(fetchedAt: Long, now: Long): Age {
    val minutes = (now - fetchedAt).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> Age(0, AgeUnit.JustNow)
        minutes < 60 -> Age(minutes, AgeUnit.Minutes)
        minutes < 24 * 60 -> Age(minutes / 60, AgeUnit.Hours)
        else -> Age(minutes / (24 * 60), AgeUnit.Days)
    }
}
