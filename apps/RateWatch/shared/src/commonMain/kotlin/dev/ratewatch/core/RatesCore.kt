package dev.ratewatch.core

import dev.ratewatch.data.RateApiException
import dev.ratewatch.data.RateRepository
import dev.ratewatch.model.ConvertInput
import dev.ratewatch.model.ConvertState
import dev.ratewatch.model.DEFAULT_BASE
import dev.ratewatch.model.RateTable
import dev.ratewatch.model.RatesState
import dev.ratewatch.model.RefreshError
import dev.ratewatch.rules.ageOf
import dev.ratewatch.rules.convertInput
import dev.ratewatch.rules.currencyCodes
import dev.ratewatch.rules.freshnessOf
import dev.ratewatch.rules.isStale
import dev.ratewatch.rules.rateRows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * [App 3 · B2] RatesCore: holds both screens' state and receives every user action. Shared by both UIs.
 *
 * Offline-first: [state] is built ONLY from the repository's database flows. [refresh] asks the repository to
 * download and save; when that succeeds the database flow emits the new table by itself. When it fails, only
 * `error` changes. The table on screen is never cleared by a failed refresh.
 *
 * Why [scope] and [clock] are constructor parameters: tests pass the test scheduler's scope and a fake clock, so
 * "auto-refresh after exactly one hour" is tested without waiting an hour. In the app Koin builds ONE RatesCore
 * with the defaults; it lives as long as the process, like the database.
 *
 * Called by: RatesViewModel (Android), RatesModel (iOS). Calls: RateRepository, rules/.
 */
class RatesCore(
    private val repository: RateRepository,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<RefreshError?>(null)

    // The time used for "updated … ago". Re-read on start() and after each refresh, so the label is current
    // whenever the user is likely to look at it, without a ticking timer.
    private val now = MutableStateFlow(clock.millis())

    private val base: StateFlow<String> = repository.observeBase()
        .map { it ?: DEFAULT_BASE }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_BASE)

    private val convertInput = MutableStateFlow(ConvertInput())
    private var refreshJob: Job? = null

    /** Rates screen state. Android: collectAsStateWithLifecycle(). Swift: `for await` (SKIE). */
    val state: StateFlow<RatesState> = combine(
        base,
        // flatMapLatest: when the base changes, stop watching the old base's rows and watch the new one.
        base.flatMapLatest { repository.observe(it) },
        repository.observeFavorites(),
        combine(refreshing, error, now, ::Triple),
    ) { base, table, favorites, (refreshing, error, now) ->
        // TRAP: right after a base change, flatMapLatest may still hold the OLD base's table for one emission.
        // Showing it under the new base name would display wrong numbers, so drop a table whose base does not match.
        val current = table?.takeIf { it.base == base }
        buildState(base, current, favorites, refreshing, error, now)
    }.stateIn(scope, SharingStarted.Eagerly, RatesState())

    /** Convert screen state, computed from the same table as the Rates screen. */
    val convert: StateFlow<ConvertState> = combine(convertInput, state) { input, rates ->
        ConvertState(input = input, result = convertInput(input, rates.table), currencies = rates.currencies)
    }.stateIn(scope, SharingStarted.Eagerly, ConvertState())

    /**
     * Call when the app comes to the foreground. Updates the "… ago" label and refreshes if the saved table is
     * missing or older than one hour.
     */
    fun start() {
        now.value = clock.millis()
        scope.launch { refreshIfStale(base.value) }
    }

    /** Pull-to-refresh / retry button. Ignored while a refresh is already running. */
    fun refresh() {
        launchRefresh(base.value)
    }

    /** Picks a new base currency, remembers it, and downloads it if needed. */
    fun selectBase(code: String) {
        if (code == base.value) return
        error.value = null // the old error belonged to the old base
        scope.launch {
            repository.setBase(code)
            refreshIfStale(code)
        }
    }

    /** Stars or un-stars [code]; the database flow moves the row to or from the top. */
    fun toggleFavorite(code: String) {
        scope.launch { repository.toggleFavorite(code) }
    }

    /** Stores the text as typed, so "12," stays "12," while the user is still typing. */
    fun onAmountChanged(text: String) = convertInput.update { it.copy(amountText = text) }

    /** Currency to convert from. */
    fun onFromSelected(code: String) = convertInput.update { it.copy(from = code) }

    /** Currency to convert to. */
    fun onToSelected(code: String) = convertInput.update { it.copy(to = code) }

    /** USD → VND becomes VND → USD; the typed amount stays. */
    fun swapCurrencies() = convertInput.update { it.copy(from = it.to, to = it.from) }

    private suspend fun refreshIfStale(base: String) {
        val saved = repository.observe(base).first()
        if (saved == null || isStale(saved.fetchedAt, clock.millis())) launchRefresh(base)
    }

    private fun launchRefresh(base: String) {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            refreshing.value = true
            try {
                // The repository never throws except CancellationException, which must escape untouched:
                // the finally block still runs, so the spinner stops, but no error is shown.
                val result = repository.refresh(base, clock.millis())
                error.value = result.exceptionOrNull()?.let(::toRefreshError)
            } finally {
                refreshing.value = false
                now.value = clock.millis()
            }
        }
    }

    private fun toRefreshError(e: Throwable): RefreshError = (e as? RateApiException)?.reason ?: RefreshError.Unknown

    private fun buildState(
        base: String,
        table: RateTable?,
        favorites: Set<String>,
        refreshing: Boolean,
        error: RefreshError?,
        now: Long,
    ) = RatesState(
        base = base,
        table = table,
        rows = rateRows(table, favorites),
        freshness = freshnessOf(table, now),
        age = table?.let { ageOf(it.fetchedAt, now) },
        refreshing = refreshing,
        error = error,
        currencies = currencyCodes(table),
    )

    private fun Clock.millis(): Long = now().toEpochMilliseconds()
}
