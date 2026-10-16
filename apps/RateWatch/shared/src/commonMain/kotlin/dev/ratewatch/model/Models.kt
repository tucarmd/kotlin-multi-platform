package dev.ratewatch.model

/**
 * [App 3 · B1] Plain data the whole app passes around. No logic lives here: rules/ computes, data/ loads and saves,
 * core/ stores.
 *
 * Why times are `Long` epoch milliseconds and not `Instant`: Swift sees a Kotlin Long as `Int64` (a plain number),
 * but an Instant as an opaque Kotlin object. Rules and tests also read better with plain numbers.
 *
 * Used by: rules/, data/RateRepository, core/RatesCore, both UIs.
 */

/** Base currency shown on first launch, before the user picks one. */
const val DEFAULT_BASE = "USD"

/**
 * One saved answer from the API: how many units of each currency one unit of [base] buys.
 *
 * Example: base = "USD", rates = {"EUR": 0.89, "VND": 26_312.5}, fetchedAt = 1_792_022_551_000.
 */
data class RateTable(
    val base: String,
    /** Currency code → units per 1 [base]. Includes [base] itself with rate 1.0. */
    val rates: Map<String, Double>,
    /** When THIS phone wrote the table (epoch ms), not when the provider updated it. See rules/Freshness.kt. */
    val fetchedAt: Long,
)

/** How old the table on screen is. Drives the red "updated … ago" label and the auto-refresh. */
enum class Freshness { Missing, Fresh, Stale }

/** Unit for "updated 2 hours ago". The UI picks the sentence; the core only says how many of what. */
enum class AgeUnit { JustNow, Minutes, Hours, Days }

/** Example: Age(2, Hours) → "updated 2 hours ago" / "cập nhật 2 giờ trước". */
data class Age(val amount: Long, val unit: AgeUnit)

/**
 * Why the last refresh failed. The UI turns each case into a message; the core never holds UI text.
 *
 * TRAP: avoid single capital letters inside names. SKIE lower-cases runs of capitals for Swift,
 * so `HTTPError` would become `.httperror`. `ServerError` stays `.serverError` on both sides.
 */
enum class RefreshError {
    /** No connection, DNS failure, airplane mode. */
    Offline,
    /** The server did not answer within the timeout. */
    Timeout,
    /** HTTP 4xx/5xx, or the API said `"result": "error"`. */
    ServerError,
    /** HTTP 200 but the JSON is broken or a required field (rates) is missing. */
    BadResponse,
    /** The API does not know this base currency (`"error-type": "unsupported-code"`). */
    UnknownBase,
    /** Anything else (for example the database write failed). */
    Unknown,
}

/** One line of the rates list. [rateText] is pre-formatted in shared code so both platforms print the same digits. */
data class RateRow(
    val code: String,
    val rate: Double,
    val rateText: String,
    val favorite: Boolean,
)

/**
 * Everything the Rates screen needs, in one object. RatesCore publishes a new one on every change.
 *
 * Offline-first: [table] comes ONLY from the database. A failed refresh sets [error] but never clears [table].
 */
data class RatesState(
    val base: String = DEFAULT_BASE,
    /** null = nothing saved for [base] yet (first launch, or a base never loaded). */
    val table: RateTable? = null,
    /** Favorites first, then A→Z. The base currency itself is left out. */
    val rows: List<RateRow> = emptyList(),
    val freshness: Freshness = Freshness.Missing,
    /** null when [table] is null. */
    val age: Age? = null,
    val refreshing: Boolean = false,
    val error: RefreshError? = null,
    /** Codes for the base / currency pickers: every code in [table], or a short list before the first load. */
    val currencies: List<String> = emptyList(),
) {
    /** Banner "Offline · showing saved rates": the network failed but there is something to show. */
    val showOfflineBanner: Boolean get() = error != null && table != null

    /** Full-screen error with a retry button: the network failed AND there is nothing saved. */
    val showEmptyError: Boolean get() = error != null && table == null && !refreshing
}

/** What the user typed or picked on the Convert screen, kept exactly as entered. */
data class ConvertInput(
    val amountText: String = "100",
    val from: String = "USD",
    val to: String = "VND",
)

/** Why the typed amount cannot be converted. Same naming rule as [RefreshError]. */
enum class AmountError { NotNumeric, TooLarge }

/**
 * The answer on the Convert screen. Exactly one case, so the UI cannot forget one.
 *
 * TRAP: sealed **class**, not sealed interface. Objective-C keeps subclass info only for classes,
 * so Swift (via SKIE `onEnum(of:)`) sees a proper enum. Same lesson as SplitResult in Tip Split.
 */
sealed class ConvertResult {
    /** Amount field is empty: show a hint, not an error. */
    data object Waiting : ConvertResult()

    data class Invalid(val error: AmountError) : ConvertResult()

    /** No saved table yet, or the table does not contain [ConvertInput.from] / [ConvertInput.to]. */
    data object NoRates : ConvertResult()

    /**
     * [amountText] is the converted amount, [rateText] is "1 from = x to", both formatted in shared code.
     * Example: 100 USD → VND gives amount 2_631_250.0, amountText "2.631.250", rateText "26.312,50".
     */
    data class Ready(val amount: Double, val amountText: String, val rateText: String) : ConvertResult()
}

/** Everything the Convert screen needs. */
data class ConvertState(
    val input: ConvertInput = ConvertInput(),
    val result: ConvertResult = ConvertResult.Waiting,
    val currencies: List<String> = emptyList(),
)
