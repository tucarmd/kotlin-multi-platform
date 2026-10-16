package dev.obdlink.obd.model

/**
 * [App 5 · B1] ObdCommand — every request the app can send to an ELM327, as a type instead of a loose string.
 *
 * Why sealed: the queue, the console log and the tests all switch on the kind of command
 * (an AT setting, a live PID, read codes, clear codes). A typo like "01OC" (letter O) cannot compile
 * when the live loop builds commands from [ObdPid] instead of typing them.
 *
 * The wire text is always [text] + "\r" (ELM327 executes on carriage return). The transport adds the "\r".
 *
 * Used by: CommandQueue, ObdCore. Uses: nothing.
 */
sealed class ObdCommand {
    /** Exactly what goes on the wire, without the trailing "\r". Example: `At("E0").text` → "ATE0". */
    abstract val text: String

    /** ELM327 setting, e.g. `At("Z")` → "ATZ" (reset). Answers "OK" or a version text, never OBD data. */
    data class At(val code: String) : ObdCommand() {
        override val text: String get() = "AT$code"
    }

    /** Mode 01 "show current data" for one [pid]. Example: `Pid(ObdPid.EngineRpm).text` → "010C". */
    data class Pid(val pid: ObdPid) : ObdCommand() {
        override val text: String get() = "01" + pid.hex
    }

    /** Mode 03: stored diagnostic trouble codes. Answer starts with 0x43. */
    data object ReadDtc : ObdCommand() {
        override val text: String get() = "03"
    }

    /**
     * Mode 04: clear codes. Answer is 0x44.
     *
     * TRAP: this also turns the check-engine light off AND resets every readiness monitor, so the car fails an
     * emissions test until it has been driven again. ObdCore only sends it after an explicit confirmation.
     */
    data object ClearDtc : ObdCommand() {
        override val text: String get() = "04"
    }

    /** Text typed in the Console tab, already checked by [dev.obdlink.obd.rules.parseConsoleInput]. */
    data class Raw(override val text: String) : ObdCommand()

    companion object {
        /** ATZ: full reset. The answer is the chip version, e.g. "ELM327 v1.5". Echo is ON again after it. */
        val Reset = At("Z")

        /** ATE0: stop echoing our own command back, so the answer is only the answer. */
        val EchoOff = At("E0")

        /** ATL0: no extra line feed after each "\r". */
        val LinefeedsOff = At("L0")

        /**
         * ATSP0: let the chip find the car's protocol by itself (CAN, KWP, ISO 9141, J1850…).
         *
         * TRAP: the search only starts with the NEXT OBD request, which then prints "SEARCHING..." and can take
         * several seconds. CommandQueue gives that first request a longer timeout.
         */
        val AutoProtocol = At("SP0")

        /** Sent in this order after every (re)connect. ATZ first because the dongle may hold settings from another app. */
        val InitSequence: List<ObdCommand> = listOf(Reset, EchoOff, LinefeedsOff, AutoProtocol)
    }
}

/**
 * [App 5 · B1] The three mode-01 PIDs shown on the Live tab (SAE J1979). [hex] is the PID byte as sent;
 * [dataBytes] is how many data bytes (A, B…) follow "41 <pid>" in the answer.
 */
enum class ObdPid(val hex: String, val dataBytes: Int, val unit: ObdUnit) {
    EngineRpm("0C", 2, ObdUnit.Rpm),
    VehicleSpeed("0D", 1, ObdUnit.KmPerHour),
    CoolantTemp("05", 1, ObdUnit.Celsius),
    ;

    /** The PID as a number, to compare with the second byte of the answer: EngineRpm → 0x0C. */
    val code: Int get() = hex.toInt(16)
}

/** Unit of a decoded PID value. The UI turns it into "rpm", "km/h", "°C". */
enum class ObdUnit { Rpm, KmPerHour, Celsius }
