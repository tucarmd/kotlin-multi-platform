package dev.ratewatch.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import dev.ratewatch.db.RateDb
import dev.ratewatch.model.RateTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * [App 3 · B3] RateRepository: the app's two data sources (network + database) behind one interface.
 *
 * Offline-first contract: readers ([observe], [observeFavorites], [observeBase]) only ever see the DATABASE.
 * The network is used in exactly one place, [refresh], and only to WRITE into the database. So "offline" needs
 * no special code: the flows keep emitting what was saved last time.
 *
 * Why an interface: RatesCoreTest uses a FakeRateRepository (no database, no network, runs in milliseconds).
 *
 * Called by: RatesCore. Implemented by: SqlRateRepository (below), FakeRateRepository (tests).
 */
interface RateRepository {
    /** The saved table for [base], or null when nothing was saved yet. Emits again after every write. */
    fun observe(base: String): Flow<RateTable?>

    /** Starred currency codes. */
    fun observeFavorites(): Flow<Set<String>>

    /** The base the user picked last time, or null on first launch. */
    fun observeBase(): Flow<String?>

    /**
     * Downloads the table for [base] and replaces the saved one in ONE transaction, stamped with [now] (epoch ms).
     * Never throws except CancellationException: failures come back as `Result.failure(RateApiException)`,
     * and the saved table is left untouched.
     */
    suspend fun refresh(base: String, now: Long): Result<Unit>

    /** Stars [code] if it is not starred, un-stars it otherwise. */
    suspend fun toggleFavorite(code: String)

    /** Remembers [code] as the base for the next launch. */
    suspend fun setBase(code: String)
}

/**
 * [App 3 · B3] The real repository: Ktor ([api]) in, SQLDelight ([driver]) out.
 *
 * Why the rows are written in a transaction: `observe` re-queries after every statement. Without a transaction the UI
 * could see the table half-deleted (empty list flash) or new rates with the old time. A transaction notifies once,
 * after commit, with the complete new table.
 */
class SqlRateRepository(driver: SqlDriver, private val api: RateApi) : RateRepository {

    private val db = RateDb(driver)

    override fun observe(base: String): Flow<RateTable?> =
        db.rateQueries.selectTable(base).asFlow().mapToList(Dispatchers.Default).map { rows ->
            if (rows.isEmpty()) {
                null
            } else {
                RateTable(base = base, rates = rows.associate { it.code to it.rate }, fetchedAt = rows.first().fetchedAtMillis)
            }
        }

    override fun observeFavorites(): Flow<Set<String>> =
        db.favoriteQueries.selectAll().asFlow().mapToList(Dispatchers.Default).map { it.toSet() }

    override fun observeBase(): Flow<String?> =
        db.settingQueries.select(KEY_BASE).asFlow().mapToOneOrNull(Dispatchers.Default).distinctUntilChanged()

    override suspend fun refresh(base: String, now: Long): Result<Unit> = resultOf {
        // Network first, outside the transaction: a slow download must not hold a database lock.
        val response = api.latest(base)
        val rates = response.rates.orEmpty() // latest() already guarantees non-empty
        withContext(Dispatchers.Default) {
            db.rateQueries.transaction {
                db.rateQueries.deleteRates(base)
                rates.forEach { (code, rate) -> db.rateQueries.insertRate(base, code, rate) }
                // TRAP: stamp with OUR clock (now), not the provider's time_last_update_unix. The provider updates
                // once a day, so its time is usually > 1 h old and every launch would look stale and refetch.
                db.rateQueries.upsertTable(base, now)
            }
        }
    }

    override suspend fun toggleFavorite(code: String) {
        withContext(Dispatchers.Default) {
            db.favoriteQueries.transaction {
                if (db.favoriteQueries.isFavorite(code).executeAsOne() > 0) {
                    db.favoriteQueries.delete(code)
                } else {
                    db.favoriteQueries.insert(code)
                }
            }
        }
    }

    override suspend fun setBase(code: String) {
        withContext(Dispatchers.Default) { db.settingQueries.upsert(KEY_BASE, code) }
    }

    private companion object {
        const val KEY_BASE = "base"
    }
}

/**
 * Like `runCatching`, but lets CancellationException through.
 *
 * Example: resultOf { 42 } → success(42) · resultOf { error("x") } → failure · resultOf { throw CancellationException() } → throws.
 */
internal suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    // TRAP: stdlib runCatching catches CancellationException too. The cancelled coroutine would then keep running
    // and the core would show "Unknown error" when the user simply left the screen.
    // TEST: RatesCoreTest.`resultOf lets cancellation through`
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
