package dev.ratewatch.data

import dev.ratewatch.model.RefreshError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

/**
 * [App 3 · B3] RateApi: one HTTP call, `GET https://open.er-api.com/v6/latest/{base}`, decoded into [RateResponse].
 *
 * Why it knows nothing about the database: it can be tested with a MockEngine alone (RateApiTest), and the
 * repository decides what to keep. Every failure leaves this class as a [RateApiException] carrying a
 * [RefreshError], so the layers above never have to know Ktor's exception types.
 *
 * Called by: SqlRateRepository.refresh. Calls: HttpClient (built by createHttpClient).
 */
class RateApi(
    private val client: HttpClient,
    private val baseUrl: String = BASE_URL,
) {

    /**
     * Downloads the latest table for [base] (an ISO code such as "USD").
     * Returns only a response with `result == "success"` and a non-empty `rates`; anything else throws.
     *
     * Example: latest("USD") → RateResponse(result = "success", baseCode = "USD", rates = {"USD": 1.0, "EUR": 0.89, …}).
     * @throws RateApiException with Offline / Timeout / ServerError / BadResponse / UnknownBase.
     * @throws CancellationException when the caller's coroutine is cancelled (never wrapped).
     */
    suspend fun latest(base: String): RateResponse {
        val response = send("$baseUrl/latest/$base")
        if (!response.status.isSuccess()) throw RateApiException(RefreshError.ServerError, "HTTP ${response.status.value}")

        val body = try {
            response.body<RateResponse>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Broken JSON, a missing required field, or a non-JSON body (an HTML error page with status 200).
            throw RateApiException(RefreshError.BadResponse, e.message, e)
        }

        // TRAP: this API reports errors with HTTP 200 and {"result":"error","error-type":"unsupported-code"}.
        // Checking only the status code would save an empty table as if it were a success.
        if (body.result != "success") {
            val reason = if (body.errorType == "unsupported-code") RefreshError.UnknownBase else RefreshError.ServerError
            throw RateApiException(reason, body.errorType)
        }
        if (body.rates.isNullOrEmpty()) throw RateApiException(RefreshError.BadResponse, "no rates")
        return body
    }

    /** Sends the GET and turns transport failures into [RateApiException]. */
    private suspend fun send(url: String): HttpResponse = try {
        client.get(url)
    } catch (e: CancellationException) {
        // TRAP: leaving the screen cancels the coroutine. That is not a network error: rethrow it so the coroutine
        // really stops, and the user never sees "Offline" for something they did themselves.
        throw e
    } catch (e: HttpRequestTimeoutException) {
        throw RateApiException(RefreshError.Timeout, e.message, e)
    } catch (e: ConnectTimeoutException) {
        throw RateApiException(RefreshError.Timeout, e.message, e)
    } catch (e: SocketTimeoutException) {
        throw RateApiException(RefreshError.Timeout, e.message, e)
    } catch (e: IOException) {
        // PLATFORM: OkHttp throws UnknownHostException / ConnectException, Darwin throws DarwinHttpRequestException
        // (NSURLErrorNotConnectedToInternet). Both are IOExceptions, so one branch covers both phones.
        throw RateApiException(RefreshError.Offline, e.message, e)
    } catch (e: Exception) {
        throw RateApiException(RefreshError.Unknown, e.message, e)
    }

    companion object {
        const val BASE_URL = "https://open.er-api.com/v6"
    }
}

/**
 * The JSON from open.er-api.com, checked with curl. Only the fields the app reads are declared; the rest
 * ("provider", "documentation", "time_next_update_utc"…) are skipped thanks to `ignoreUnknownKeys`.
 *
 * Every field except [result] has a default because the ERROR body is `{"result":"error","error-type":"…"}`:
 * no rates, no base_code. RateApi.latest checks [rates] itself and reports BadResponse when it is missing.
 */
@Serializable
data class RateResponse(
    val result: String,
    @SerialName("base_code") val baseCode: String? = null,
    /** When the PROVIDER last updated (once a day). Not used for freshness: see rules/Freshness.kt. */
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    val rates: Map<String, Double>? = null,
    @SerialName("error-type") val errorType: String? = null,
)

/** The only exception that leaves RateApi. [reason] is what the UI will show. */
class RateApiException(
    val reason: RefreshError,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: reason.name, cause)
