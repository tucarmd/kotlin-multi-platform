package dev.ratewatch.rules

import dev.ratewatch.model.RateRow
import dev.ratewatch.model.RateTable

/**
 * [App 3 · B1] Turns a saved table into the rows of the Rates screen.
 *
 * Why here and not in each UI: "favorites on top, then A→Z, base left out" is a rule. Written twice (Compose and
 * SwiftUI) it would drift; written once it is tested once.
 *
 * Called by: RatesCore. Calls: formatRate (Amount.kt).
 */

/** Shown in the base picker before any table has been downloaded. */
val COMMON_CURRENCIES = listOf("USD", "EUR", "GBP", "JPY", "CNY", "KRW", "SGD", "THB", "AUD", "VND")

/**
 * Rows for [table]: favorites first, then everything else, each group sorted A→Z. The base itself is left out
 * (1 USD = 1 USD tells nobody anything). Empty when [table] is null.
 *
 * Example: base USD, rates {USD 1, EUR 0.9, VND 26312.5, AUD 1.43}, favorites {VND}
 *   → [VND ★, AUD, EUR].
 */
fun rateRows(table: RateTable?, favorites: Set<String>): List<RateRow> {
    if (table == null) return emptyList()
    return table.rates
        .filterKeys { it != table.base }
        .map { (code, rate) -> RateRow(code, rate, formatRate(rate), favorite = code in favorites) }
        .sortedWith(compareByDescending<RateRow> { it.favorite }.thenBy { it.code })
}

/**
 * Codes for the pickers: every code in [table] A→Z, or [COMMON_CURRENCIES] when nothing is saved yet.
 *
 * Example: currencyCodes(null) → COMMON_CURRENCIES · table with {VND, EUR, USD} → [EUR, USD, VND].
 */
fun currencyCodes(table: RateTable?): List<String> =
    table?.rates?.keys?.sorted() ?: COMMON_CURRENCIES
