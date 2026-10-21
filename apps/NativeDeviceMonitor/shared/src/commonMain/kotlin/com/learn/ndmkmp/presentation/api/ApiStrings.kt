package com.learn.ndmkmp.presentation.api

import com.learn.ndmkmp.core.network.ApiResult

/**
 * 1 test button = 1 row of the Day 10 test table (Step 7). `params` is only used for GET.
 * Mirrors `UI/Api/ApiStrings.swift` — same URLs, same order.
 */
data class ApiPreset(
    val label: String,
    val method: String,
    val url: String,
    val params: Map<String, String> = emptyMap(),
    val body: String? = null,
)

/** API tab strings (Day 10). */
object ApiStrings {
    const val HINT = "Day 10 — GET / POST JSON with headers, query params, HTTP status, timeout (8 s) and no-network handling."

    const val PRESETS_TITLE = "Test requests"
    const val CUSTOM_TITLE = "Custom GET"
    const val URL_LABEL = "URL"
    const val SEND_GET = "Send GET"
    const val RESULT_TITLE = "Response"
    const val NOTHING_YET = "Tap a test request."
    const val SENDING = "Sending…"
    const val AIRPLANE_HINT = "No network test: turn on airplane mode, then tap any request."

    const val POST_BODY = """{"title":"hello","body":"from Native Device Monitor","userId":1}"""

    val PRESETS = listOf(
        ApiPreset("GET /posts/1 → 200", "GET", "https://jsonplaceholder.typicode.com/posts/1"),
        ApiPreset("GET /posts?userId=1 (params)", "GET", "https://jsonplaceholder.typicode.com/posts", mapOf("userId" to "1")),
        ApiPreset("POST /posts → 201", "POST", "https://jsonplaceholder.typicode.com/posts", body = POST_BODY),
        ApiPreset("GET headers echo", "GET", "https://httpbin.org/headers"),
        ApiPreset("GET /status/500 → HTTP error", "GET", "https://httpbin.org/status/500"),
        ApiPreset("GET /delay/10 → Timeout", "GET", "https://httpbin.org/delay/10"),
    )

    /** Bold status line. `ok` = green, otherwise red. */
    fun statusLine(r: ApiResult): Pair<String, Boolean> = when (r) {
        is ApiResult.Success -> "SUCCESS · HTTP ${r.status} · ${r.durationMs} ms" to true
        is ApiResult.HttpError -> "HTTP ERROR · ${r.status} · ${r.durationMs} ms" to false
        is ApiResult.Timeout -> "TIMEOUT · no response after ${r.timeoutMs / 1000} s" to false
        is ApiResult.NoNetwork -> "NO NETWORK · ${r.message}" to false
        is ApiResult.Failure -> "FAILED · ${r.message}" to false
    }

    fun request(method: String, url: String) = "$method $url"
}
