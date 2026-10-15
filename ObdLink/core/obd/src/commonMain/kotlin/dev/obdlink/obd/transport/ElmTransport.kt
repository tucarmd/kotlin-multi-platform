package dev.obdlink.obd.transport

import dev.obdlink.obd.model.Dongle
import dev.obdlink.obd.model.LinkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * [App 5 · B3] ElmTransport — the one door between the OBD logic and "some wire" to an ELM327.
 *
 * Why an interface in :core:obd (and not in :core:ble): ObdCore and CommandQueue only need "send text, get the answer
 * up to '>'". Keeping the interface here lets the tests use FakeElmTransport with no BLE library at all, and lets
 * :core:ble (Kable) depend on :core:obd, never the other way round.
 *
 * Implemented by: KableElmTransport (:core:ble), FakeElmTransport (tests). Used by: CommandQueue, ObdCore.
 */
interface ElmTransport {
    /** Radio link state. Goes to [LinkStatus.Lost] when the dongle disappears without [disconnect]. */
    val link: StateFlow<LinkStatus>

    /**
     * Dongles nearby, one emission per advertisement (the same dongle repeats with a fresh RSSI).
     * Scanning runs while the flow is collected and stops when the collector is cancelled.
     * Throws [BluetoothUnavailableException] when the radio is off or permission is missing.
     */
    fun scan(): Flow<Dongle>

    /** Opens the link and subscribes to answers. Suspends until ready; throws on failure. */
    suspend fun connect(dongle: Dongle)

    /** Closes the link on purpose. [link] becomes Disconnected, never Lost. */
    suspend fun disconnect()

    /**
     * Writes [command] + "\r" and suspends until the ELM327 prompt `>` arrives.
     * Returns everything received before `>` (still raw: may contain echo, "SEARCHING...", "\r").
     * Has NO timeout of its own: CommandQueue owns timing. Throws [LinkLostException] if the link is down.
     */
    suspend fun send(command: String): String
}

/** [App 5 · B3] The link dropped (or was never up) while sending. ObdCore reacts with Reconnecting. */
class LinkLostException(message: String = "ELM327 link lost") : Exception(message)

/** [App 5 · B3] Bluetooth is off, unsupported, or the scan/connect permission is missing. */
class BluetoothUnavailableException(cause: Throwable? = null) : Exception("Bluetooth unavailable", cause)

/** [App 5 · B2] No answer within the timeout, even after the retry. Thrown by CommandQueue. */
class CommandTimeoutException(val command: String) : Exception("No answer to $command")
