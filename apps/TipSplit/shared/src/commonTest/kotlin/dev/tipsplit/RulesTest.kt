package dev.tipsplit

import dev.tipsplit.model.InputError
import dev.tipsplit.model.SplitInput
import dev.tipsplit.model.SplitResult
import dev.tipsplit.rules.ROUND_TO
import dev.tipsplit.rules.calculate
import dev.tipsplit.rules.splitBill
import dev.tipsplit.rules.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** [App 1 · B7] The rules from rules/Rules.kt. Each test name says the behaviour it protects. */
class RulesTest {

    @Test
    fun `the example from the docs gives 37000 each`() {
        val split = splitBill(bill = 100_000, tipPercent = 10, people = 3, roundTo = ROUND_TO)
        assertEquals(10_000, split.tip)
        assertEquals(110_000, split.total)
        assertEquals(37_000, split.perPerson)
        assertEquals(111_000, split.collected)
        assertEquals(1_000, split.extra)
    }

    @Test
    fun `zero tip and an even split leaves nothing extra`() {
        val split = splitBill(bill = 90_000, tipPercent = 0, people = 3)
        assertEquals(30_000, split.perPerson)
        assertEquals(0, split.extra)
    }

    @Test
    fun `shares are rounded up so the group never pays less than the total`() {
        // 100.000 / 3 = 33.333,33…: rounding down would collect 99.999
        val split = splitBill(bill = 100_000, tipPercent = 0, people = 3)
        assertEquals(33_334, split.perPerson)
        assertTrue(split.collected >= split.total)
    }

    @Test
    fun `tip is rounded half up instead of truncated`() {
        // 15 % of 99.999 = 14.999,85 → 15.000 (truncating would give 14.999)
        assertEquals(15_000, splitBill(bill = 99_999, tipPercent = 15, people = 1).tip)
    }

    @Test
    fun `validate reports the first problem`() {
        assertEquals(InputError.NotNumeric, validate(null, 2))
        assertEquals(InputError.NotPositive, validate(0, 2))
        assertEquals(InputError.TooLarge, validate(2_000_000_000_000, 2))
        assertEquals(InputError.NoPeople, validate(50_000, 0))
        assertEquals(InputError.TooManyPeople, validate(50_000, 51))
        assertEquals(null, validate(50_000, 2))
    }

    @Test
    fun `splitBill refuses input that was not validated`() {
        assertFailsWith<IllegalArgumentException> { splitBill(bill = 0, tipPercent = 10, people = 2) }
    }

    @Test
    fun `calculate maps raw input to the three screen cases`() {
        assertEquals(SplitResult.Waiting, calculate(SplitInput(billText = "  ")))
        assertEquals(SplitResult.Invalid(InputError.NotNumeric), calculate(SplitInput(billText = "abc")))
        val ready = calculate(SplitInput(billText = "100.000", tipPercent = 10, people = 3, roundUp = false))
        assertEquals(36_667, (ready as SplitResult.Ready).split.perPerson)
    }
}
