package com.learn.ndmkmp.core.network

import com.learn.ndmkmp.platformName
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

/** What a thrown exception means for the user. Timeouts are common Ktor types; "no network" differs per platform. */
enum class NetworkErrorKind { TIMEOUT, NO_NETWORK, OTHER }

/**
 * Android: `UnknownHostException` (airplane mode, no DNS) · `ConnectException`.
 * iOS: `DarwinHttpRequestException` wrapping an `NSError` from `NSURLErrorDomain` (-1009 not connected, -1003 …).
 */
expect fun platformNetworkErrorKind(e: Throwable): NetworkErrorKind

/**
 * P6 — the real [ApiService]: one Ktor [HttpClient] in commonMain, the engine is picked per platform from the classpath
 * (OkHttp in androidMain, Darwin = `NSURLSession` in iosMain). Replaces the old `HttpURLConnection` / `URLSession` pair.
 *
 * ```
 *  get / post ─► HttpClient (HttpTimeout: connect 10 s · socket 8 s)
 *                 ├─ 2xx        → Success(status, body, content-type)
 *                 ├─ otherwise  → HttpError(status, body)     ← expectSuccess = false: no exception for 500
 *                 └─ throws     → Timeout / NoNetwork / Failure  (classify)
 * ```
 *
 * @param client injectable → tests pass `HttpClient(MockEngine)`, no network.
 */
class KtorApiService(private val client: HttpClient = defaultClient()) : ApiService {

    override suspend fun get(url: String, params: Map<String, String>): ApiResult =
        request { client.get(ApiFormat.buildUrl(url, params)) }

    override suspend fun post(url: String, body: String): ApiResult = request {
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend fun request(call: suspend () -> HttpResponse): ApiResult {
        val start = TimeSource.Monotonic.markNow()
        fun elapsed() = start.elapsedNow().inWholeMilliseconds
        return try {
            val response = call()
            val status = response.status.value
            val text = response.bodyAsText()
            if (status in 200..299) {
                ApiResult.Success(status, text, response.headers[HttpHeaders.ContentType], elapsed())
            } else {
                ApiResult.HttpError(status, text, elapsed())
            }
        } catch (e: CancellationException) {
            throw e                                   // the screen left — not an error, let the coroutine stop
        } catch (e: Throwable) {
            classify(e, elapsed())
        }
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
        /** 8 s — under 10 s so the `httpbin.org/delay/10` preset reliably ends in Timeout. */
        const val SOCKET_TIMEOUT_MS = 8_000L

        fun classify(e: Throwable, durationMs: Long): ApiResult {
            val name = e::class.simpleName ?: "Error"
            val kind = when (e) {
                is HttpRequestTimeoutException, is SocketTimeoutException, is ConnectTimeoutException -> NetworkErrorKind.TIMEOUT
                else -> platformNetworkErrorKind(e)
            }
            return when (kind) {
                NetworkErrorKind.TIMEOUT -> ApiResult.Timeout(SOCKET_TIMEOUT_MS.toInt(), durationMs)
                NetworkErrorKind.NO_NETWORK -> ApiResult.NoNetwork("$name: ${e.message}", durationMs)
                NetworkErrorKind.OTHER -> ApiResult.Failure("$name: ${e.message}", durationMs)
            }
        }

        /** `HttpClient { }` with no engine argument = the one engine on this platform's classpath. */
        fun defaultClient() = HttpClient { configure() }

        /** Shared by the app and the tests so both send the same headers and timeouts. */
        fun io.ktor.client.HttpClientConfig<*>.configure() {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
            defaultRequest {
                header(HttpHeaders.Accept, "application/json")
                // An echo server (httpbin /headers) shows which platform sent it
                header("X-Client", "NativeDeviceMonitor-KMP (${platformName()})")
            }
        }
    }
}
