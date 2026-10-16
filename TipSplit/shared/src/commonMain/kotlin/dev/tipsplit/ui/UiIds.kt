package dev.tipsplit.ui

/**
 * [Phụ lục P2 · Bảng id] One name per thing a test taps or reads, shared by every kind of UI test:
 *
 * - Android: `Modifier.testTag(UiIds.BILL_INPUT)`, found by Compose UI test and (with testTagsAsResourceId) by Maestro.
 * - iOS: `.accessibilityIdentifier(UiIds.shared.BILL_INPUT)`, found by XCUITest and Maestro.
 * - Maestro: `id: split_bill_input` in the .maestro flows runs unchanged on both platforms.
 *
 * TRAP: never write a glob like "folder/ *.yaml" without the space inside a KDoc. Kotlin comments NEST, so the
 * slash-star opens a second comment and the file fails with "Syntax error: Unclosed comment".
 *
 * Why in commonMain: a renamed id breaks the tests of BOTH apps at compile time instead of silently on one.
 *
 * TRAP: SwiftUI's segmented Picker has no id per segment, so the tip buttons are found by their text ("10%") on iOS.
 * Android still tags each one with [tip] for Compose UI test.
 */
object UiIds {
    const val BILL_INPUT = "split_bill_input"
    const val BILL_ERROR = "split_bill_error"
    const val PEOPLE_MINUS = "split_people_minus"
    const val PEOPLE_COUNT = "split_people_count"
    const val PEOPLE_PLUS = "split_people_plus"
    const val ROUND_UP = "split_round_up"
    const val RESET = "split_reset"

    /** Waiting hint or "fix the input" message: whatever the result card shows when there is no split. */
    const val RESULT_MESSAGE = "split_result_message"
    const val RESULT_PER_PERSON = "split_result_per_person"
    const val RESULT_TIP = "split_result_tip"
    const val RESULT_TOTAL = "split_result_total"

    /** Tip button for [percent]: split_tip_0, split_tip_5, split_tip_10, split_tip_15. */
    fun tip(percent: Int) = "split_tip_$percent"
}
