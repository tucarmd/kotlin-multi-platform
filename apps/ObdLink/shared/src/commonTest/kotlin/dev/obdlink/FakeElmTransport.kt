package dev.obdlink

import dev.obdlink.obd.model.Dongle
import dev.obdlink.obd.model.LinkStatus
import dev.obdlink.obd.transport.BluetoothUnavailableException
import dev.obdlink.obd.transport.ElmTransport
import dev.obdlink.obd.transport.LinkLostException
import dev.obdlink.obd.transport.PromptCollector
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * [App 5 · B7] FakeElmTransport — an ELM327 in memory, so CommandQueue and ObdCore are tested without Bluetooth.
 *
 * - [answers]: table "command → raw answer" (with "\r" and the final `>`, exactly like a dongle).
 * - [chunkSize]: cuts each answer into pieces glued by the same PromptCollector as the real transport (BLE notify).
 * - [silent] / [silentTimes]: commands that never answer (always / the first N times) → timeouts.
 * - [dropLink]: the dongle vanishes; a waiting send() fails with LinkLostException.
 * - [failConnects]: the next N connect() calls throw.
 * - [maxInFlight]: proves the queue never sends two commands at once.
 *
 * Runs on virtual time (runTest): [answerDelay] and [delays] cost no real time.
 */
class FakeElmTransport(
    val answers: MutableMap<String, String> = defaultAnswers(),
) : ElmTransport {
    val dongle = Dongle(id = "fake-1", name = "OBDII", rssi = -55)

    var chunkSize: Int = Int.MAX_VALUE
    var answerDelay: Duration = 10.milliseconds
    val delays = mutableMapOf<String, Duration>()
    val silent = mutableSetOf<String>()
    val silentTimes = mutableMapOf<String, Int>()
    var failConnects = 0
    var bluetoothOff = false

    val sent = mutableListOf<String>()
    var connectCalls = 0
        private set
    var maxInFlight = 0
        private set
    private var inFlight = 0

    private val _link = MutableStateFlow(LinkStatus.Disconnected)
    override val link: StateFlow<LinkStatus> = _link

    override fun scan(): Flow<Dongle> = flow {
        if (bluetoothOff) throw BluetoothUnavailableException()
        delay(100.milliseconds)
        emit(dongle)
        delay(100.milliseconds)
        emit(dongle.copy(rssi = -50)) // same dongle again, stronger signal
        awaitCancellation() // a real scan runs until stopped
    }

    override suspend fun connect(dongle: Dongle) {
        connectCalls++
        delay(20.milliseconds)
        if (failConnects > 0) {
            failConnects--
            throw IllegalStateException("fake connect failure")
        }
        _link.value = LinkStatus.Connected
    }

    override suspend fun disconnect() {
        _link.value = LinkStatus.Disconnected
    }

    /** Simulates the dongle being unplugged. */
    fun dropLink() {
        _link.value = LinkStatus.Lost
    }

    override suspend fun send(command: String): String {
        if (_link.value != LinkStatus.Connected) throw LinkLostException()
        sent += command
        inFlight++
        maxInFlight = maxOf(maxInFlight, inFlight)
        try {
            delay(delays[command] ?: answerDelay)
            val skips = silentTimes[command] ?: 0
            if (command in silent || skips > 0) {
                if (skips > 0) silentTimes[command] = skips - 1
                // Never answers; only a timeout (cancellation) or a dropped link ends the wait.
                _link.first { it != LinkStatus.Connected }
                throw LinkLostException()
            }
            val raw = answers[command] ?: "?\r\r>"
            val collector = PromptCollector()
            for (chunk in raw.chunked(chunkSize)) {
                if (_link.value != LinkStatus.Connected) throw LinkLostException()
                collector.feed(chunk)?.let { return it }
                delay(1.milliseconds) // next BLE notification
            }
            error("Fake answer for $command has no '>' prompt")
        } finally {
            inFlight--
        }
    }

    companion object {
        /** Answers copied from a generic ELM327 v1.5 clone on a CAN car, engine idling. */
        fun defaultAnswers() = mutableMapOf(
            "ATZ" to "ATZ\r\r\rELM327 v1.5\r\r>",
            "ATE0" to "ATE0\rOK\r\r>",
            "ATL0" to "OK\r\r>",
            "ATSP0" to "OK\r\r>",
            "010C" to "41 0C 1A F8\r\r>",
            "010D" to "41 0D 32\r\r>",
            "0105" to "41 05 7B\r\r>",
            "03" to "43 01 03 01\r\r>",
            "04" to "44\r\r>",
            "ATRV" to "12.6V\r\r>",
        )
    }
}
