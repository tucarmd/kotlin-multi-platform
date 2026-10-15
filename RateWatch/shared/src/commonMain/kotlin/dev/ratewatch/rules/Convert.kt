package dev.ratewatch.rules

import dev.ratewatch.model.AmountError
import dev.ratewatch.model.ConvertInput
import dev.ratewatch.model.ConvertResult
import dev.ratewatch.model.RateTable

/**
 * [App 3 · B1] Currency conversion as pure functions.
 *
 * Why every conversion goes through the base currency: the API returns ONE table, "1 base = x of each currency".
 * There is no EUR → JPY rate in it, but there is USD → EUR and USD → JPY, so EUR → JPY = rate[JPY] / rate[EUR].
 *
 * Called by: RatesCore (Convert screen state). Calls: parseAmount, formatAmount, formatRate (Amount.kt).
 */

/**
 * How many [to] one [from] buys, using any table whose rates contain both codes. null when a code is missing.
 *
 * Example (base USD, EUR 0.9, JPY 150): rateBetween("EUR", "JPY") → 166.67 · rateBetween("USD", "XYZ") → null.
 */
fun rateBetween(from: String, to: String, table: RateTable): Double? {
    val fromRate = table.rates[from] ?: return null
    val toRate = table.rates[to] ?: return null
    if (fromRate <= 0.0) return null // a zero rate would divide by zero; treat as "no rate"
    return toRate / fromRate
}

/**
 * Converts [amount] (hundredths of [from], from [parseAmount]) into [to]: `amount / rate[from] * rate[to]`.
 *
 * Example (base USD, EUR 0.9, JPY 150): convert(9_000 = 90.00 EUR, "EUR", "JPY") → 15_000.0 JPY.
 * Same currency returns the amount unchanged; an unknown code returns null.
 */
fun convert(amount: Long, from: String, to: String, table: RateTable): Double? {
    val fromRate = table.rates[from] ?: return null
    val toRate = table.rates[to] ?: return null
    if (fromRate <= 0.0) return null
    // TEST: RulesTest.`converts EUR to JPY through the USD base`
    return amount / 100.0 / fromRate * toRate
}

/**
 * The single entry point RatesCore uses for the Convert screen: raw input + saved table → what the screen shows.
 *
 * Examples: amountText "" → Waiting · "abc" → Invalid(NotNumeric) · table null → NoRates ·
 * "100" USD → VND (26_312.5) → Ready(2_631_250.0, "2.631.250", "26.312,50").
 */
fun convertInput(input: ConvertInput, table: RateTable?): ConvertResult {
    if (input.amountText.isBlank()) return ConvertResult.Waiting
    val amount = parseAmount(input.amountText) ?: return ConvertResult.Invalid(AmountError.NotNumeric)
    if (amount > MAX_AMOUNT) return ConvertResult.Invalid(AmountError.TooLarge)
    if (table == null) return ConvertResult.NoRates
    val result = convert(amount, input.from, input.to, table) ?: return ConvertResult.NoRates
    val rate = rateBetween(input.from, input.to, table) ?: return ConvertResult.NoRates
    return ConvertResult.Ready(
        amount = result,
        amountText = formatAmount(result, input.to),
        rateText = formatRate(rate),
    )
}
