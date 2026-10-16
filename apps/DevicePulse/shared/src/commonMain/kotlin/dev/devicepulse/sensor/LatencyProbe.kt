package dev.devicepulse.sensor

import co.touchlab.kermit.Logger
import dev.devicepulse.model.Reading
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

/** Times one HTTP round trip. `generate_204` returns an empty body, so the number is mostly network latency. */
class LatencyProbe(private val client: HttpClient) {

    suspend fun measure(url: String = DEFAULT_URL): Reading<Long> {
        val mark = TimeSource.Monotonic.markNow()
        return try {
            val response = client.get(url)
            val millis = mark.elapsedNow().inWholeMilliseconds
            if (response.status.isSuccess()) {
                Reading.Available(millis)
            } else {
                Reading.Error("HTTP ${response.status.value}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Latency probe failed" }
            Reading.Error(e.message ?: e::class.simpleName ?: "Unknown error")
        }
    }

    companion object {
        const val DEFAULT_URL = "https://www.gstatic.com/generate_204"
    }
}
