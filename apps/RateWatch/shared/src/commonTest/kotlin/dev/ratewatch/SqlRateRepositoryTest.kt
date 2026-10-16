package dev.ratewatch

import app.cash.turbine.test
import dev.ratewatch.data.RateApi
import dev.ratewatch.data.RateApiException
import dev.ratewatch.data.SqlRateRepository
import dev.ratewatch.data.createHttpClient
import dev.ratewatch.model.RefreshError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * [Phụ lục P1 · DB trong RAM] Integration test: the REAL SqlRateRepository with real SQL (in-memory SQLite) and a
 * scripted server (MockEngine). Unlike RatesCoreTest (fake repository), this checks the .sq files, the transaction
 * and the row ↔ RateTable mapping. Runs in commonTest, so the SQL is checked on the JVM and the iOS simulator.
 */
class SqlRateRepositoryTest {

    private val driver = inMemoryRateDbDriver()

    @AfterTest
    fun close() = driver.close()

    /** [status] and [body] are what the fake server answers to every request. */
    private fun repository(status: HttpStatusCode = HttpStatusCode.OK, body: String = USD_OK) = SqlRateRepository(
        driver,
        RateApi(
            createHttpClient(
                MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
            ),
        ),
    )

    @Test
    fun `a successful refresh saves the table stamped with our clock`() = runTest {
        val repo = repository()
        assertNull(repo.observe("USD").first())

        repo.refresh("USD", now = 1_000)

        val table = repo.observe("USD").first()!!
        assertEquals(26_312.5, table.rates["VND"])
        assertEquals(3, table.rates.size)
        assertEquals(1_000, table.fetchedAt) // our clock, not the provider's time_last_update_unix
    }

    @Test
    fun `a failed refresh leaves the saved table untouched`() = runTest {
        repository().refresh("USD", now = 1_000)

        val result = repository(HttpStatusCode.InternalServerError, "oops").refresh("USD", now = 2_000)

        assertEquals(RefreshError.ServerError, assertIs<RateApiException>(result.exceptionOrNull()).reason)
        assertEquals(1_000, repository().observe("USD").first()!!.fetchedAt)
    }

    @Test
    fun `a refresh is seen as ONE update thanks to the transaction`() = runTest {
        val repo = repository()
        repo.refresh("USD", now = 1_000)
        repo.observe("USD").test {
            assertEquals(1_000, awaitItem()!!.fetchedAt)

            repo.refresh("USD", now = 2_000)

            // Without db.transaction { } the flow would also see the half-written table (deleted rows → null),
            // and the screen would flash empty. One commit = one notification = one new item.
            assertEquals(2_000, awaitItem()!!.fetchedAt)
            expectNoEvents()
        }
    }

    @Test
    fun `starring twice un-stars`() = runTest {
        val repo = repository()
        repo.toggleFavorite("EUR")
        assertEquals(setOf("EUR"), repo.observeFavorites().first())
        repo.toggleFavorite("EUR")
        assertEquals(emptySet(), repo.observeFavorites().first())
    }

    private companion object {
        const val USD_OK = """{"result":"success","base_code":"USD","time_last_update_unix":1792022551,
            "rates":{"USD":1,"EUR":0.894425,"VND":26312.5}}"""
    }
}
