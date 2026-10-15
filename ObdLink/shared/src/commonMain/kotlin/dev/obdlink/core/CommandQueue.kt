package dev.obdlink.core

import dev.obdlink.obd.model.Exchange
import dev.obdlink.obd.model.ObdCommand
import dev.obdlink.obd.rules.parseResponse
import dev.obdlink.obd.transport.CommandTimeoutException
import dev.obdlink.obd.transport.ElmTransport
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [App 5 · B2] CommandQueue — sends ONE command at a time to the ELM327, with a timeout and one retry.
 *
 * Why a queue: the ELM327 is a single serial line with no request ids. If the live loop sends "010C" while the Codes
 * tab sends "03", the two answers come back on the same stream in some order and nothing says which is which:
 * the RPM gauge would show a DTC byte. A [Mutex] makes every other caller wait until the current answer's `>` arrived.
 *
 * Why the Mutex wraps the retry too: between a timeout and its retry another command must not slip in, or the late
 * answer to the first try would be read as the answer to the newcomer.
 *
 * Used by: ObdCore. Uses: ElmTransport.send, parseResponse.
 */
class CommandQueue(
    private val transport: ElmTransport,
    /** Normal per-command limit. A healthy dongle answers a PID in 50–300 ms. */
    private val timeout: Duration = 2.seconds,
    /** Limit for the first OBD request after ATZ / ATSP: the chip is searching the car's protocol. */
    private val searchTimeout: Duration = 10.seconds,
    private val retries: Int = 1,
) {
    private val mutex = Mutex()

    // Guarded by mutex. True after ATZ / ATSPx until the next OBD (non-AT) command finished.
    private var protocolSearchPending = false

    /** Sends a typed command. See [sendText]. */
    suspend fun send(command: ObdCommand): Exchange = sendText(command.text)

    /**
     * Sends [text], waits for the `>` prompt and parses the answer.
     * Throws [CommandTimeoutException] after `1 + retries` silent tries; lets LinkLostException through untouched
     * (retrying on a dead link is pointless: ObdCore reconnects instead).
     *
     * Example: sendText("010C") → Exchange("010C", "41 0C 1A F8\r\r", Data([[0x41, 0x0C, 0x1A, 0xF8]])).
     */
    suspend fun sendText(text: String): Exchange = mutex.withLock { sendLocked(text) }

    private suspend fun sendLocked(text: String): Exchange {
        val isAt = text.startsWith("AT")
        // TRAP: the first OBD request after ATSP0 prints "SEARCHING..." and may take seconds. With the normal
        // 2 s limit it times out, the retry interrupts the search ("STOPPED") and init never completes.
        val limit = if (protocolSearchPending && !isAt) searchTimeout else timeout
        var attempt = 0
        while (true) {
            try {
                val raw = withTimeout(limit) { transport.send(text) }
                updateSearchFlag(text, isAt)
                return Exchange(command = text, raw = raw, result = parseResponse(raw, sent = text))
            } catch (e: TimeoutCancellationException) {
                // TRAP: catch TimeoutCancellationException exactly. Catching CancellationException would also swallow
                // a real cancel (user pressed Stop) and keep the loop alive.
                // TEST: CommandQueueTest.`timeout then retry then error`
                if (attempt++ >= retries) {
                    updateSearchFlag(text, isAt)
                    throw CommandTimeoutException(text)
                }
            }
        }
    }

    private fun updateSearchFlag(text: String, isAt: Boolean) {
        when {
            text == "ATZ" || text.startsWith("ATSP") || text.startsWith("ATTP") -> protocolSearchPending = true
            !isAt -> protocolSearchPending = false
        }
    }
}
