package dev.devicepulse

import dev.devicepulse.model.Reading
import dev.devicepulse.sensor.LatencyProbe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LatencyProbeTest {

    private fun probe(engine: MockEngine) = LatencyProbe(HttpClient(engine))

    @Test
    fun successReturnsElapsedMillis() = runTest {
        val result = probe(MockEngine { respond("", HttpStatusCode.NoContent) }).measure()
        val available = assertIs<Reading.Available<Long>>(result)
        assertTrue(available.value >= 0)
    }

    @Test
    fun httpErrorBecomesErrorReading() = runTest {
        val result = probe(MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }).measure()
        assertEquals(Reading.Error("HTTP 503"), result)
    }

    @Test
    fun networkFailureNeverThrows() = runTest {
        val result = probe(MockEngine { error("no route to host") }).measure()
        assertEquals(Reading.Error("no route to host"), result)
    }
}
