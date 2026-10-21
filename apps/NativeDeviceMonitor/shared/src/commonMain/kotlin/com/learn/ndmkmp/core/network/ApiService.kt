package com.learn.ndmkmp.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Day 10 · result of one request — the UI only does `when` over these 5 branches, never sees an exception.
 * `HttpError` (server answered 500/404) is kept apart from `Failure` (couldn't reach it); `Timeout` / `NoNetwork`
 * are split out per the spec ("timeout handling", "network error handling").
 */
sealed interface ApiResult {
    val durationMs: Long

    data class Success(val status: Int, val body: String, val contentType: String?, override val durationMs: Long) : ApiResult
    data class HttpError(val status: Int, val body: String, override val durationMs: Long) : ApiResult
    data class Timeout(val timeoutMs: Int, override val durationMs: Long) : ApiResult
    data class NoNetwork(val message: String, override val durationMs: Long) : ApiResult
    data class Failure(val message: String, override val durationMs: Long) : ApiResult
}

/** What the API tab calls. P2: [com.learn.ndmkmp.fake.FakeApiService] · P6: Ktor (OkHttp on Android, Darwin on iOS). */
interface ApiService {
    suspend fun get(url: String, params: Map<String, String> = emptyMap()): ApiResult

    /** [body] is a JSON string — sent with `Content-Type: application/json`. */
    suspend fun post(url: String, body: String): ApiResult
}

/** Pure helpers shared by the screen and every [ApiService]. */
object ApiFormat {

    /** Append `?a=1&b=2`, keys sorted for a stable result; space → `%20` (not the form-style `+`). */
    fun buildUrl(base: String, params: Map<String, String>): String {
        if (params.isEmpty()) return base
        val query = params.entries.sortedBy { it.key }.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
        return base + (if ('?' in base) "&" else "?") + query
    }

    /** RFC 3986 percent-encoding: unreserved chars stay, everything else becomes `%XX` of its UTF-8 bytes. */
    private fun encode(s: String): String = buildString {
        s.encodeToByteArray().forEach { b ->
            val c = b.toInt() and 0xFF
            if (c.toChar().isLetterOrDigit() && c < 0x80 || c.toChar() in "-._~") append(c.toChar())
            else append('%').append(c.toString(16).uppercase().padStart(2, '0'))
        }
    }

    private val pretty = Json { prettyPrint = true }

    /** JSON → indented for reading; not JSON (an HTML error page…) → returned as-is. */
    fun prettyJson(body: String): String {
        val t = body.trim()
        if (!t.startsWith("{") && !t.startsWith("[")) return body
        return try {
            pretty.encodeToString(JsonElement.serializer(), pretty.parseToJsonElement(t))
        } catch (e: IllegalArgumentException) {   // SerializationException = broken JSON
            body
        }
    }
}
