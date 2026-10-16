package dev.tipsplit.rules

/**
 * [App 1 · B1] Text ↔ amount conversion, shared by both UIs so "120.000" means the same thing everywhere.
 *
 * Why here and not in each UI: Android and iOS keyboards insert different separators depending on the
 * phone's language. Parsing once in commonMain gives one behaviour and one set of tests.
 */

/** Characters people type as thousands separators: dot (vi), comma (en), space, no-break space, apostrophe (de-CH). */
private val SEPARATORS = setOf('.', ',', ' ', ' ', '\'')

/**
 * Reads a whole-đồng amount, ignoring thousands separators. Returns null when the text is not a number.
 *
 * Examples: "120.000" → 120000 · "120,000" → 120000 · " 5 000 " → 5000 · "12a" → null · "" → null · "-5" → null.
 */
fun parseMoney(text: String): Long? {
    val digits = text.trim().filterNot { it in SEPARATORS }
    if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
    // TRAP: more than 18 digits does not fit in a Long. toLongOrNull() returns null instead of overflowing.
    return digits.toLongOrNull()
}

/**
 * Formats with a dot every 3 digits, the Vietnamese way: 1234567 → "1.234.567".
 * TEST: MoneyTest.`formats with a dot every three digits`
 */
fun formatMoney(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    val grouped = (if (amount < 0) -amount else amount).toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    return sign + grouped
}
