package dev.obdlink.model

import dev.obdlink.obd.model.Dongle
import dev.obdlink.obd.model.Dtc

/**
 * [App 5 · B2] ObdState — everything the four tabs draw, in one immutable value published by ObdCore.
 *
 * Why one state and not four flows: the tabs depend on each other (Live and Codes are disabled until the
 * connection is Ready). One value means the UI can never show "Polling" next to a Codes tab that thinks
 * we are disconnected.
 *
 * Made by: ObdCore. Read by: ObdViewModel (Android), ObdModel (iOS).
 */
data class ObdState(
    val connection: ConnectionState = ConnectionState.Idle,
    /** Dongles found by the last scan, in the order found (RSSI refreshed in place, so rows do not jump). Kept after the scan stops so the user can pick one. */
    val dongles: List<Dongle> = emptyList(),
    val live: LiveData = LiveData(),
    val codes: CodesState = CodesState.NotRead,
    /** Console log, oldest first, capped at [CONSOLE_LIMIT] lines. */
    val console: List<ConsoleLine> = emptyList(),
    /** Banner returned by ATZ, e.g. "ELM327 v1.5". Empty until the first init. */
    val adapterVersion: String = "",
) {
    /** Live, Codes and Console need an initialised dongle. */
    val isConnected: Boolean get() = connection is ConnectionState.Ready || connection is ConnectionState.Polling

    companion object {
        const val CONSOLE_LIMIT = 200
    }
}

/**
 * [App 5 · B2] The connection state machine (see README). Every branch is a class, so `when` / Swift `switch`
 * stops compiling when a state is added.
 *
 * ```
 * Idle → Scanning → Connecting → Initializing → Ready ⇄ Polling
 *                      ↓              ↓            ↓ link lost
 *                    Error ←──── Reconnecting (3 attempts) → Initializing
 * Error → reset() → Idle
 * ```
 */
sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Scanning : ConnectionState()
    data class Connecting(val dongle: Dongle) : ConnectionState()

    /** Link is up; sending ATZ / ATE0 / ATL0 / ATSP0. */
    data class Initializing(val dongle: Dongle) : ConnectionState()

    /** Dongle answered the init sequence. Codes and Console work; live polling is off. */
    data class Ready(val dongle: Dongle) : ConnectionState()

    /** Live loop running (RPM → speed → coolant, round and round). */
    data class Polling(val dongle: Dongle) : ConnectionState()

    /** Link lost; trying again. [attempt] counts 1..[maxAttempts]. */
    data class Reconnecting(val dongle: Dongle, val attempt: Int, val maxAttempts: Int) : ConnectionState()

    data class Error(val reason: ObdError) : ConnectionState()

    /** The dongle this state is about, if any. */
    val dongleOrNull: Dongle?
        get() = when (this) {
            is Connecting -> dongle
            is Initializing -> dongle
            is Ready -> dongle
            is Polling -> dongle
            is Reconnecting -> dongle
            Idle, Scanning, is Error -> null
        }
}

/** [App 5 · B2] Why the connection ended in [ConnectionState.Error]. The UI maps each to a localised message. */
enum class ObdError {
    /** Radio off, BLE unsupported or permission missing. */
    BluetoothOff,

    /** Scan stopped with an unexpected error. */
    ScanFailed,

    /** The dongle did not accept the BLE connection in time, or has no ELM327 serial service. */
    ConnectFailed,

    /**
     * Connected, but the dongle did not answer the AT init sequence (not an ELM327, or a broken clone).
     * TRAP: not `InitFailed`. Objective-C treats members starting with "init" as initializers, so Kotlin/Native
     * mangles them and the Swift case name is no longer what you typed.
     */
    SetupFailed,

    /** The link dropped and 3 reconnect attempts failed. */
    ReconnectFailed,
}

/**
 * [App 5 · B2] Latest live values. A null reading = no answer yet, or "NO DATA" (engine off, unsupported PID).
 * [refreshHz] = full rounds (all three PIDs) per second; the done-criteria want ≥ 2.
 */
data class LiveData(
    val rpm: Reading? = null,
    val speedKmh: Reading? = null,
    val coolantC: Reading? = null,
    val refreshHz: Double = 0.0,
)

/**
 * [App 5 · B2] One gauge value. Wrapped in a class (not `Int?`) because a nullable Kotlin Int reaches Swift as
 * `KotlinInt?`, an object; `Reading?` reaches Swift as a normal optional with a plain `value: Int32`.
 */
data class Reading(val value: Int)

/** [App 5 · B2] Codes tab. Clearing goes through [ConfirmClear] so no UI can clear by a single tap. */
sealed class CodesState {
    data object NotRead : CodesState()
    data object Reading : CodesState()
    data class Loaded(val codes: List<Dtc>) : CodesState()

    /** Waiting for the user to confirm mode 04. [codes] = what was on screen, restored on cancel. */
    data class ConfirmClear(val codes: List<Dtc>) : CodesState()
    data object Clearing : CodesState()
    data class Failed(val reason: CodesFailure) : CodesState()
}

/** [App 5 · B2] Why reading or clearing codes failed. */
enum class CodesFailure {
    /** Timed out (after the retry) or the link dropped. */
    NoAnswer,

    /** The car answered with an error ("?", "UNABLE TO CONNECT", a 7F negative answer…). */
    Rejected,
}

/** [App 5 · B2] One console line. For notes the UI shows a localised message for [kind]; [text] is the command. */
data class ConsoleLine(val kind: ConsoleKind, val text: String)

/** [App 5 · B2] Console line kinds. Only [Sent] and [Received] carry dongle text. */
enum class ConsoleKind {
    Sent,
    Received,

    /** No answer within the timeout, even after the retry. */
    NoAnswer,

    /** Typed text is not an AT command nor hex. */
    InvalidInput,

    /** Mode 04 typed in the console: refused, use the Codes tab. */
    ClearBlocked,

    /** Typed while no dongle is connected. */
    NotConnected,
}
