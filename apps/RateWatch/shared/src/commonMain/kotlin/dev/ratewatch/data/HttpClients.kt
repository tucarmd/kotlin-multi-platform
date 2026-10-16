package dev.ratewatch.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * [App 3 · B3] Builds the one HttpClient the app uses. Everything except the engine is common.
 *
 * Why a factory that takes the engine: the engine (OkHttp on Android, Darwin/NSURLSession on iOS, MockEngine in tests)
 * is the ONLY thing that differs. platformModule passes the real one, RateApiTest passes a MockEngine, and both get
 * the same JSON and timeout setup, so the tests exercise the real configuration.
 *
 * Called by: httpClient() in PlatformModule.android.kt / PlatformModule.ios.kt, RateApiTest. Calls: Ktor.
 */

/** The JSON settings for every response. */
val RateJson: Json = Json {
    // TRAP: without this, the day the API adds a field (it already sends "provider", "terms_of_use"…
    // that RateResponse does not declare) every installed app fails with "Encountered an unknown key".
    ignoreUnknownKeys = true
}

/** Request timeout for real use. Short enough that a dead network shows an error before the user gives up. */
const val DEFAULT_TIMEOUT_MILLIS = 10_000L

/**
 * HttpClient with JSON decoding and a timeout on top of [engine].
 *
 * Example: createHttpClient(OkHttp.create()) in the app · createHttpClient(MockEngine { … }, timeoutMillis = 100) in tests.
 */
fun createHttpClient(engine: HttpClientEngine, timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) { json(RateJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
        }
        // Default is already false; written out because RateApi checks the status itself and relies on it.
        expectSuccess = false
    }
