package dev.tipsplit

import dev.tipsplit.rules.formatMoney
import dev.tipsplit.rules.parseMoney
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [App 1 · B7] parseMoney / formatMoney. Runs on the JVM and the iOS Simulator with the same expectations. */
class MoneyTest {

    @Test
    fun `reads Vietnamese and English and spaced thousands the same`() {
        assertEquals(120_000, parseMoney("120.000"))
        assertEquals(120_000, parseMoney("120,000"))
        assertEquals(120_000, parseMoney(" 120 000 "))
        assertEquals(5, parseMoney("5"))
    }

    @Test
    fun `rejects anything that is not a whole number`() {
        assertNull(parseMoney(""))
        assertNull(parseMoney("12a"))
        assertNull(parseMoney("-5"))
        assertNull(parseMoney("..."))
    }

    @Test
    fun `returns null instead of overflowing on a huge number`() {
        assertNull(parseMoney("99999999999999999999")) // 20 digits > Long.MAX_VALUE
    }

    @Test
    fun `formats with a dot every three digits`() {
        assertEquals("0", formatMoney(0))
        assertEquals("999", formatMoney(999))
        assertEquals("1.000", formatMoney(1_000))
        assertEquals("1.234.567", formatMoney(1_234_567))
        assertEquals("-37.000", formatMoney(-37_000))
    }
}
