package dev.ratewatch

import dev.ratewatch.data.RateApi
import dev.ratewatch.data.RateApiException
import dev.ratewatch.data.createHttpClient
import dev.ratewatch.model.RefreshError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [App 3 · B7] RateApi against a MockEngine: no network, but the REAL client setup (createHttpClient), so the JSON
 * settings and the timeout under test are the ones the app ships with.
 *
 * Covers the four cases the spec asks for (200, 500, JSON missing a field, timeout) plus the traps found while
 * building: errors sent with HTTP 200, unknown fields, no connection, cancellation.
 */
class RateApiTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(timeoutMillis: Long = 10_000, handler: suspend MockRequestHandleScope.() -> HttpResponseData) =
        RateApi(createHttpClient(MockEngine { handler() }, timeoutMillis))

    private suspend fun failure(api: RateApi): RefreshError =
        assertFailsWith<RateApiException> { api.latest("USD") }.reason

    @Test
    fun `a 200 response is decoded and unknown fields are ignored`() = runTest {
        var path = ""
        val api = RateApi(
            createHttpClient(
                MockEngine { request ->
                    path = request.url.encodedPath
                    respond(SAMPLE_OK, HttpStatusCode.OK, json)
                },
            ),
        )
        val response = api.latest("USD")
        assertEquals("/v6/latest/USD", path)
        assertEquals("USD", response.baseCode)
        assertEquals(1_792_022_551, response.timeLastUpdateUnix)
        assertEquals(26_312.5, response.rates!!["VND"])
    }

    @Test
    fun `http 500 becomes a server error`() = runTest {
        assertEquals(RefreshError.ServerError, failure(api { respond("oops", HttpStatusCode.InternalServerError) }))
    }

    @Test
    fun `json without rates is a bad response`() = runTest {
        val body = """{"result":"success","base_code":"USD","time_last_update_unix":1}"""
        assertEquals(RefreshError.BadResponse, failure(api { respond(body, HttpStatusCode.OK, json) }))
    }

    @Test
    fun `json without the required result field is a bad response`() = runTest {
        val body = """{"base_code":"USD","rates":{"USD":1}}"""
        assertEquals(RefreshError.BadResponse, failure(api { respond(body, HttpStatusCode.OK, json) }))
    }

    @Test
    fun `broken json is a bad response`() = runTest {
        assertEquals(RefreshError.BadResponse, failure(api { respond("{\"result\":", HttpStatusCode.OK, json) }))
    }

    @Test
    fun `a slow server times out`() = runTest {
        val slow = api(timeoutMillis = 50) {
            delay(5_000)
            respond(SAMPLE_OK, HttpStatusCode.OK, json)
        }
        assertEquals(RefreshError.Timeout, failure(slow))
    }

    @Test
    fun `an error sent with status 200 is not a success`() = runTest {
        val body = """{"result":"error","error-type":"unsupported-code"}"""
        assertEquals(RefreshError.UnknownBase, failure(api { respond(body, HttpStatusCode.OK, json) }))
    }

    @Test
    fun `no connection is offline`() = runTest {
        assertEquals(RefreshError.Offline, failure(api { throw IOException("Unable to resolve host") }))
    }

    @Test
    fun `cancellation is passed through and never wrapped`() = runTest {
        assertFailsWith<CancellationException> {
            api { throw CancellationException("screen closed") }.latest("USD")
        }
    }

    private companion object {
        // Trimmed copy of a real answer from https://open.er-api.com/v6/latest/USD (checked with curl).
        const val SAMPLE_OK = """
            {"result":"success","provider":"https://www.exchangerate-api.com",
             "documentation":"https://www.exchangerate-api.com/docs/free",
             "time_last_update_unix":1792022551,"time_last_update_utc":"Thu, 15 Oct 2026 00:02:31 +0000",
             "time_next_update_unix":1792109581,"time_eol_unix":0,"base_code":"USD",
             "rates":{"USD":1,"EUR":0.894425,"JPY":150.12,"VND":26312.5}}
        """
    }
}
