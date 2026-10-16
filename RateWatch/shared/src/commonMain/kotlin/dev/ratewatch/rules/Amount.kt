package dev.ratewatch.rules

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * [App 3 · B1] Text ↔ number for amounts and rates, shared by both UIs so "1.234,5" means the same thing everywhere.
 *
 * Why the typed amount is a Long of hundredths and not a Double: same idea as Money in Tip Split. What the user types
 * is exact ("0.10" stays 10 hundredths). Only the RATE is a Double, because the API sends it as one and it is only
 * displayed, never added up. Output uses the Tip Split style: dot every 3 digits, comma before decimals.
 *
 * Called by: rules/Convert.kt, rules/Rates.kt. Calls: nothing.
 */

/** Largest amount accepted: 1.000 tỷ units (same limit as Tip Split), in hundredths. */
const val MAX_AMOUNT: Long = 1_000_000_000_000L * 100

/** Currencies that have no minor unit in daily use: amounts in them are shown without decimals. */
val ZERO_DECIMAL_CURRENCIES = setOf("VND", "JPY", "KRW", "IDR", "CLP", "ISK", "HUF", "PYG", "UGX")

/**
 * Reads an amount typed with either separator style and returns it in hundredths, or null when it is not a number.
 * The LAST `.` or `,` is the decimal point only when 1–2 digits follow it; every other separator is grouping.
 *
 * Examples: "100" → 10_000 · "12.5" → 1_250 · "12,50" → 1_250 · "120.000" → 12_000_000 · "1,234.56" → 123_456 ·
 * "" → null · "12a" → null · "-5" → null.
 */
fun parseAmount(text: String): Long? {
    val cleaned = text.trim().filterNot { it == ' ' || it == ' ' || it == '\'' }
    // TRAP: Char.isDigit() also accepts Arabic-Indic and other Unicode digits, and on the JVM "١٢".toLongOrNull() is 12.
    // Only ASCII 0–9 is a valid amount here, so check the range explicitly (see K012 in docs/issues/01-basics.md).
    if (cleaned.isEmpty() || !cleaned.all { it in '0'..'9' || it == '.' || it == ',' }) return null

    val lastSeparator = cleaned.indexOfLast { it == '.' || it == ',' }
    val digitsAfter = if (lastSeparator >= 0) cleaned.length - lastSeparator - 1 else 0
    val hasDecimals = lastSeparator >= 0 && digitsAfter in 1..2
    val wholeText = (if (hasDecimals) cleaned.substring(0, lastSeparator) else cleaned).filter { it in '0'..'9' }
    val fractionText = if (hasDecimals) cleaned.substring(lastSeparator + 1) else ""
    if (wholeText.isEmpty() && fractionText.isEmpty()) return null

    // TRAP: more than 15 whole digits would overflow Long once multiplied by 100. Return a value above MAX_AMOUNT
    // so the caller reports TooLarge instead of the misleading NotNumeric.
    if (wholeText.trimStart('0').length > 15) return Long.MAX_VALUE
    val whole = wholeText.ifEmpty { "0" }.toLong()
    return whole * 100 + fractionText.padEnd(2, '0').toLong()
}

/**
 * Formats [value] with [decimals] digits after the comma, rounding half up, Tip Split style.
 *
 * Examples: (2_631_250.0, 0) → "2.631.250" · (26_312.5, 2) → "26.312,50" · (0.000038, 6) → "0,000038" · (NaN, 2) → "—".
 */
fun formatDecimal(value: Double, decimals: Int): String {
    require(decimals in 0..10) { "decimals must be 0..10" }
    if (value.isNaN() || value.isInfinite()) return "—"
    // TRAP: roundToLong() clamps at Long.MAX_VALUE instead of overflowing; MAX_AMOUNT keeps real inputs far below it.
    val scaled = (abs(value) * 10.0.pow(decimals)).roundToLong()
    val unit = 10.0.pow(decimals).toLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    val whole = groupThousands(scaled / unit)
    if (decimals == 0) return sign + whole
    return sign + whole + "," + (scaled % unit).toString().padStart(decimals, '0')
}

/**
 * A converted amount in [code]: no decimals for VND, JPY…, 2 decimals for everything else.
 *
 * Examples: (2_631_250.4, "VND") → "2.631.250" · (89.44, "EUR") → "89,44".
 */
fun formatAmount(value: Double, code: String): String =
    formatDecimal(value, if (code in ZERO_DECIMAL_CURRENCIES) 0 else 2)

/**
 * A rate with about 6 significant digits, so both 26.312 and 0.000038 stay readable. Never fewer than 2 decimals.
 *
 * Examples: 26_312.5 → "26.312,50" · 1.433021 → "1,43302" · 0.894425 → "0,894425" · 0.0000380228 → "0,0000380228".
 */
fun formatRate(rate: Double): String {
    val decimals = if (rate > 0) (5 - floor(log10(rate)).toInt()).coerceIn(2, 10) else 2
    return formatDecimal(rate, decimals)
}

/** 1234567 → "1.234.567". Non-negative numbers only. */
internal fun groupThousands(value: Long): String =
    value.toString().reversed().chunked(3).joinToString(".").reversed()
