package com.learn.nativedevicemonitor.nativecore.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/**
 * Day 10 · result of one request — the UI only does `when` over these 5 branches, never sees an exception.
 *
 * Why `HttpError` is separate from `Failure`: the server DID answer (500, 404…) is very different from "couldn't reach the server" —
 * the user needs to know where it failed. `Timeout` / `NoNetwork` are split out per the spec ("timeout handling",
 * "network error handling"). Mirrors `ios/.../NativeCore/Network/ApiClient.swift` (`enum ApiResult`).
 */
sealed interface ApiResult {
    val durationMs: Long

    data class Success(val status: Int, val body: String, val contentType: String?, override val durationMs: Long) : ApiResult
    data class HttpError(val status: Int, val body: String, override val durationMs: Long) : ApiResult
    data class Timeout(val timeoutMs: Int, override val durationMs: Long) : ApiResult
    data class NoNetwork(val message: String, override val durationMs: Long) : ApiResult
    data class Failure(val message: String, override val durationMs: Long) : ApiResult
}

/**
 * Day 10 — minimal API Client: `get(url, params, headers)` · `post(url, body, headers)`.
 *
 * ```
 *  UI (tab API) ──► ApiClient.get/post  (suspend)
 *                     └─ withContext(IO) ─► HttpURLConnection ─► responseCode
 *                                              ├─ 2xx  → inputStream → Success
 *                                              ├─ otherwise → errorStream → HttpError   (inputStream would THROW IOException)
 *                                              └─ IOException → classify → Timeout / NoNetwork / Failure
 * ```
 *
 * Why `HttpURLConnection` and not Retrofit/OkHttp: the spec says "Do not implement: complex API architecture"
 * and the lesson is to see status / stream / timeout first-hand. Built into Android, 0 dependencies.
 *
 * Runs on the plain JVM too (no `android.*` calls) → unit tests hit a local HttpServer, see `ApiClientTest`.
 *
 * @param readTimeoutMs 8 s — under 10 s so the `httpbin.org/delay/10` preset reliably ends in Timeout.
 */
class ApiClient(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 8_000,
) {

    suspend fun get(
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): ApiResult = request("GET", buildUrl(url, params), body = null, headers)

    /** `body` is a JSON string — adds `Content-Type: application/json` automatically. */
    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResult = request("POST", url, body, mapOf("Content-Type" to "application/json; charset=utf-8") + headers)

    // withContext(IO): network on the main thread = immediate NetworkOnMainThreadException (Android blocks it hard).
    private suspend fun request(
        method: String,
        url: String,
        body: String?,
        headers: Map<String, String>,
    ): ApiResult = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        fun elapsed() = (System.nanoTime() - start) / 1_000_000
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs   // wait for the TCP/TLS handshake
                readTimeout = readTimeoutMs         // wait between received bytes — a "hanging" server ends up here
                (DEFAULT_HEADERS + headers).forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }   // use{} = auto close
                }
            }
            val status = conn.responseCode
            // Classic trap: status ≥ 400 and reading inputStream is an IOException → error body lost. Read errorStream instead.
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status in 200..299) {
                ApiResult.Success(status, text, conn.contentType, elapsed())
            } else {
                ApiResult.HttpError(status, text, elapsed())
            }
        } catch (e: IOException) {
            classify(e, readTimeoutMs, elapsed())
        } catch (e: IllegalArgumentException) {
            ApiResult.Failure("Invalid URL: ${e.message}", elapsed())   // e.g. URL with odd characters in the host
        } finally {
            conn?.disconnect()   // return the socket — release resources (refactor checklist #5)
        }
    }

    companion object {
        /** Sent with every request — the "Headers" in the spec. An echo server (httpbin) will see `X-Client`. */
        val DEFAULT_HEADERS = mapOf(
            "Accept" to "application/json",
            "X-Client" to "NativeDeviceMonitor-Android",
        )

        /**
         * Append query `?a=1&b=2`. Keys sorted alphabetically for a stable result (iOS does the same → tests can compare).
         * `URLEncoder` is a FORM encoder (space → `+`); swap `+` → `%20` to match iOS `URLComponents`.
         */
        fun buildUrl(base: String, params: Map<String, String>): String {
            if (params.isEmpty()) return base
            val query = params.toSortedMap().entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
            return base + (if ('?' in base) "&" else "?") + query
        }

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

        /** IOException → one of 3 branches. A pure function so unit tests need no network. */
        fun classify(e: IOException, timeoutMs: Int, durationMs: Long): ApiResult = when (e) {
            is SocketTimeoutException -> ApiResult.Timeout(timeoutMs, durationMs)
            // Airplane mode → UnknownHostException (no DNS) · server down → ConnectException
            is UnknownHostException, is ConnectException, is NoRouteToHostException ->
                ApiResult.NoNetwork("${e.javaClass.simpleName}: ${e.message}", durationMs)
            else -> ApiResult.Failure("${e.javaClass.simpleName}: ${e.message}", durationMs)
        }

        /** JSON → 2-space indent for reading; not JSON (an HTML error page…) → returned as-is. */
        fun prettyJson(body: String): String {
            val t = body.trim()
            return try {
                when {
                    t.startsWith("{") -> JSONObject(t).toString(2)
                    t.startsWith("[") -> JSONArray(t).toString(2)
                    else -> body
                }.replace("\\/", "/")   // org.json escapes "/" as "\/" — undo it for readability
            } catch (e: JSONException) {
                body
            }
        }
    }
}
