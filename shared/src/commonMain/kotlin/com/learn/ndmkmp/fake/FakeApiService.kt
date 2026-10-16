package com.learn.ndmkmp.fake

import com.learn.ndmkmp.core.network.ApiFormat
import com.learn.ndmkmp.core.network.ApiResult
import com.learn.ndmkmp.core.network.ApiService
import kotlinx.coroutines.delay

/**
 * P2 stand-in for the Ktor client (P6). No network: answers by URL so each preset button lands in a different
 * [ApiResult] branch — `/status/500` → HttpError, `/delay/` → Timeout, POST → 201, anything else → 200 JSON.
 */
class FakeApiService : ApiService {

    override suspend fun get(url: String, params: Map<String, String>): ApiResult {
        val full = ApiFormat.buildUrl(url, params)
        return when {
            "/status/500" in url -> {
                delay(LATENCY_MS)
                ApiResult.HttpError(500, "", LATENCY_MS)
            }
            "/delay/" in url -> {
                delay(TIMEOUT_WAIT_MS)   // shorter than the real 8 s, long enough to see the locked buttons
                ApiResult.Timeout(timeoutMs = 8_000, durationMs = TIMEOUT_WAIT_MS)
            }
            else -> {
                delay(LATENCY_MS)
                ApiResult.Success(200, """{"fake":true,"method":"GET","url":"$full","userId":1,"id":1,"title":"demo"}""", JSON, LATENCY_MS)
            }
        }
    }

    override suspend fun post(url: String, body: String): ApiResult {
        delay(LATENCY_MS)
        // jsonplaceholder echoes the body and adds an id
        val echoed = body.trim().removeSuffix("}") + ""","id":101}"""
        return ApiResult.Success(201, echoed, JSON, LATENCY_MS)
    }

    private companion object {
        const val LATENCY_MS = 400L
        const val TIMEOUT_WAIT_MS = 2_000L
        const val JSON = "application/json; charset=utf-8"
    }
}
