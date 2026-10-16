package dev.tipsplit.model

/**
 * [App 1 · B1] Plain data the whole app passes around. No logic lives here: rules/ computes, core/ stores.
 *
 * Why money is Long and not Double: every amount is a whole number of đồng, the smallest unit of VND.
 * Integer maths is exact; Double is not (see rules/Rules.kt).
 *
 * [Bài 4 · Chia sẻ code: model] Lives in commonMain, so these exact classes exist on Android AND in Swift
 * (`import Shared`). Neither UI declares its own copy, so the two apps cannot disagree on what a "split" is.
 * Swift sees: data class → class with the same properties, enum → Swift enum (SKIE), sealed class → `onEnum(of:)`.
 */

/** What the user typed or picked, kept exactly as entered so the text field never jumps while typing. */
data class SplitInput(
    val billText: String = "",
    val tipPercent: Int = 10,
    val people: Int = 2,
    /** Round each share up to 1.000đ so nobody has to hand over coins. */
    val roundUp: Boolean = true,
)

/** The answer for one valid input. All amounts are in đồng. */
data class Split(
    val bill: Long,
    val tip: Long,
    /** bill + tip: what the restaurant must receive. */
    val total: Long,
    /** What each person pays, already rounded. */
    val perPerson: Long,
    /** perPerson × people: what the group actually hands over. Never less than [total]. */
    val collected: Long,
) {
    /** Money left over because of rounding (0 when rounding is off and the bill divides evenly). */
    val extra: Long get() = collected - total
}

/**
 * Why an input cannot be split. The UI turns each case into a message; the core never holds UI text.
 *
 * TRAP: avoid single capital letters inside names. SKIE lower-cases runs of capitals for Swift,
 * so `NotANumber` became `.notAnumber`. `NotNumeric` stays `.notNumeric` on both sides.
 */
enum class InputError { NotNumeric, NotPositive, TooLarge, NoPeople, TooManyPeople }

/**
 * The result the screen draws. Exactly one of three cases, so the UI cannot forget one (`when` / `switch` must cover all).
 *
 * TRAP: sealed **class**, not sealed interface. Objective-C keeps subclass info only for classes,
 * so Swift (via SKIE `onEnum(of:)`) sees a proper enum. Same lesson as Reading in DevicePulse.
 */
sealed class SplitResult {
    /** Bill field is empty: show a hint, not an error. */
    data object Waiting : SplitResult()
    data class Invalid(val error: InputError) : SplitResult()
    data class Ready(val split: Split) : SplitResult()
}

/** Everything the screen needs, in one object. TipCore publishes a new one on every change. */
data class SplitState(
    val input: SplitInput = SplitInput(),
    val result: SplitResult = SplitResult.Waiting,
) {
    val canRemovePerson: Boolean get() = input.people > 1
    val canAddPerson: Boolean get() = input.people < MAX_PEOPLE

    companion object {
        /** Upper limit of the people stepper. Lives here (not in rules/) so Swift reads it from the state's type. */
        const val MAX_PEOPLE = 50
    }
}
