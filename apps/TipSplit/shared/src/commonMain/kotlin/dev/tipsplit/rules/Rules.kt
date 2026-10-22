package dev.tipsplit.rules

import dev.tipsplit.model.InputError
import dev.tipsplit.model.Split
import dev.tipsplit.model.SplitInput
import dev.tipsplit.model.SplitResult
import dev.tipsplit.model.SplitState

/**
 * [App 1 · B1] The business rules of Tip Split: pure functions, input → output, no state and no I/O.
 *
 * Why pure functions: they run the same on Android, iOS and the JVM test runner, and a test is one line
 * (`assertEquals(expected, splitBill(...))`). Everything clever in the app lives in this file.
 *
 * Called by: TipCore (core/TipCore.kt). Calls: parseMoney (Money.kt).
 *
 * [Bài 4 · Chia sẻ code: luật] This is the "share the logic" level of KMP: the rules are written once in
 * commonMain, tested once in commonTest, and both UIs only display what they return.
 * Nothing here needs an Android or Apple API, so it needs neither expect/actual nor an interface.
 */

/** The tip buttons on screen. */
val TIP_OPTIONS = listOf(0, 5, 10, 15)

/** "Round up" rounds each share up to this many đồng. */
const val ROUND_TO = 1_000L

/** 1.000 tỷ đồng. Above this the number is almost certainly a typo. */
const val MAX_BILL = 1_000_000_000_000L

/**
 * Checks a parsed bill and a head count. Returns the first problem found, or null when the input can be split.
 *
 * Example: validate(null, 2) → NotNumeric · validate(0, 2) → NotPositive · validate(50_000, 0) → NoPeople.
 */
fun validate(bill: Long?, people: Int): InputError? = when {
    bill == null -> InputError.NotNumeric
    bill <= 0 -> InputError.NotPositive
    bill > MAX_BILL -> InputError.TooLarge
    people < 1 -> InputError.NoPeople
    people > SplitState.MAX_PEOPLE -> InputError.TooManyPeople
    else -> null
}

/**
 * Splits [bill] plus a [tipPercent] tip among [people]. Each share is rounded UP to a multiple of [roundTo],
 * so the group never pays less than the bill.
 *
 * Example: bill = 100_000, tip = 10 %, people = 3, roundTo = 1_000
 *   → tip 10_000, total 110_000, 36_667 exact → 37_000 each, collected 111_000 (extra 1_000).
 *
 * Callers must [validate] first; invalid numbers throw IllegalArgumentException.
 */
fun splitBill(bill: Long, tipPercent: Int, people: Int, roundTo: Long = 1): Split {
    require(bill > 0 && people > 0 && roundTo > 0 && tipPercent in 0..100) { "validate() the input first" }

    // TRAP: (bill * tipPercent) / 100 truncates: 15 % of 99.999 = 14.999,85 → 14.999. Adding 50 first rounds half up.
    val tip = (bill * tipPercent + 50) / 100
    val total = bill + tip

    // TRAP: round EACH share up, never down. Plain `/` rounds down, and then the group pays less than the bill.
    // TEST: RulesTest.`shares are rounded up so the group never pays less than the total`
    val perPerson = roundUpTo(ceilDiv(total, people.toLong()), roundTo)

    return Split(bill = bill, tip = tip, total = total, perPerson = perPerson, collected = perPerson * people)
}

/**
 * The single entry point TipCore uses: raw input → what the screen shows.
 *
 * Example: SplitInput(billText = "") → Waiting · SplitInput(billText = "abc") → Invalid(NotNumeric).
 */
fun calculate(input: SplitInput): SplitResult {
    if (input.billText.isBlank()) return SplitResult.Waiting
    val bill = parseMoney(input.billText)
    validate(bill, input.people)?.let { return SplitResult.Invalid(it) }
    return SplitResult.Ready(
        splitBill(
            bill = bill!!, // validate() returned null, so bill is not null here
            tipPercent = input.tipPercent,
            people = input.people,
            roundTo = if (input.roundUp) ROUND_TO else 1,
        ),
    )
}

/** Integer division that rounds up: ceilDiv(7, 2) = 4. Positive numbers only, which is all this app has. */
internal fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b

/** Rounds [value] up to the next multiple of [step]: roundUpTo(36_667, 1_000) = 37_000. */
internal fun roundUpTo(value: Long, step: Long): Long = ceilDiv(value, step) * step
