package dev.tipsplit

import dev.tipsplit.model.SplitState
import dev.tipsplit.rules.ROUND_TO
import dev.tipsplit.rules.TIP_OPTIONS
import dev.tipsplit.rules.splitBill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Phụ lục P1 · test bảng] splitBill checked two ways:
 *
 * 1. A TABLE of hand-picked cases: one test function, many rows. Adding a case is adding one line.
 * 2. INVARIANTS over thousands of generated inputs: rules that must hold for EVERY input, not just the examples.
 *
 * Why not one @Test per case: 12 near-identical functions hide the data. A table puts the numbers side by side,
 * so a missing edge case (1 person? 0 % tip? a bill smaller than ROUND_TO?) is visible at a glance.
 */
class SplitTableTest {

    /** One row of the table. [name] is printed when the row fails, so the report says WHICH case broke. */
    private data class Case(
        val name: String,
        val bill: Long,
        val tipPercent: Int,
        val people: Int,
        val roundUp: Boolean,
        val perPerson: Long,
    )

    private val cases = listOf(
        Case("docs example", bill = 100_000, tipPercent = 10, people = 3, roundUp = true, perPerson = 37_000),
        Case("docs example exact", bill = 100_000, tipPercent = 10, people = 3, roundUp = false, perPerson = 36_667),
        Case("one person pays all", bill = 250_000, tipPercent = 0, people = 1, roundUp = false, perPerson = 250_000),
        Case("even split no tip", bill = 90_000, tipPercent = 0, people = 3, roundUp = true, perPerson = 30_000),
        Case("bill below ROUND_TO", bill = 500, tipPercent = 0, people = 2, roundUp = true, perPerson = 1_000),
        Case("tip rounds half up", bill = 99_999, tipPercent = 15, people = 1, roundUp = false, perPerson = 114_999),
        Case("max people", bill = 1_000_000, tipPercent = 5, people = SplitState.MAX_PEOPLE, roundUp = true, perPerson = 21_000),
        Case("1 dong each", bill = 3, tipPercent = 0, people = 3, roundUp = false, perPerson = 1),
    )

    @Test
    fun `every row of the table gives the expected share`() {
        for (c in cases) {
            val split = splitBill(c.bill, c.tipPercent, c.people, if (c.roundUp) ROUND_TO else 1)
            // The third argument is the failure message: without it a broken row only says "expected 37000 but was 36000".
            assertEquals(c.perPerson, split.perPerson, "case '${c.name}'")
        }
    }

    @Test
    fun `invariants hold for every bill and tip and head count and rounding`() {
        val bills = listOf(1L, 999, 1_000, 1_001, 33_333, 100_000, 123_456, 999_999, 10_000_000, 987_654_321)
        var checked = 0
        for (bill in bills) for (tip in TIP_OPTIONS + 100) for (people in 1..SplitState.MAX_PEOPLE) for (roundTo in listOf(1L, ROUND_TO)) {
            val s = splitBill(bill, tip, people, roundTo)
            val where = "bill=$bill tip=$tip people=$people roundTo=$roundTo"

            assertTrue(s.collected >= s.total, "group pays less than the total: $where")
            assertTrue(s.perPerson % roundTo == 0L, "share is not a multiple of $roundTo: $where")
            // Rounding up adds at most (roundTo - 1) per person for the ROUND_TO step, plus < 1 per person for the
            // ceil division. More than that means a share was rounded up twice.
            assertTrue(s.extra < people * roundTo, "rounded up too much (extra=${s.extra}): $where")
            assertEquals(s.bill + s.tip, s.total, "total is not bill + tip: $where")
            checked++
        }
        assertEquals(bills.size * (TIP_OPTIONS.size + 1) * SplitState.MAX_PEOPLE * 2, checked)
    }
}
