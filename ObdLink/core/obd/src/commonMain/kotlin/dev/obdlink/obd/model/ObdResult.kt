package dev.obdlink.obd.model

/**
 * [App 5 · B1] ObdResult — what one ELM327 answer means, after [dev.obdlink.obd.rules.parseResponse] cleaned it.
 *
 * Why a sealed class and not a String: callers must handle "NO DATA" and "?" explicitly. With a String, a live
 * gauge would happily try to read bytes out of "NO DATA" and show nonsense.
 *
 * Used by: CommandQueue (returns it), ObdCore (switches on it). Made by: parseResponse.
 */
sealed class ObdResult {
    /**
     * Hex bytes. One entry per ECU message: two ECUs answering "010C" give two messages.
     * A multi-frame CAN answer ("0: 43 03 …", "1: …") is already joined into ONE message.
     * Example: "41 0C 1A F8" → `Data(listOf(listOf(0x41, 0x0C, 0x1A, 0xF8)))`.
     */
    data class Data(val messages: List<List<Int>>) : ObdResult()

    /** Plain "OK" (answer to most AT commands). */
    data object Ok : ObdResult()

    /** Any other readable text, e.g. the ATZ banner "ELM327 v1.5" or ATRV "12.6V". */
    data class Text(val text: String) : ObdResult()

    /** "NO DATA": the car is reachable but did not answer this PID (engine off, PID unsupported…). Not a failure. */
    data object NoData : ObdResult()

    /** The chip refused or the bus failed. [reason] says which, so the UI can explain it. */
    data class Error(val reason: ElmFailure) : ObdResult()
}

/** [App 5 · B1] Why an ELM327 answer is an error. Matches the error words in the ELM327 datasheet. */
enum class ElmFailure {
    /** "?": the chip did not understand the command (typo in the console, unsupported AT command). */
    UnknownCommand,

    /** "UNABLE TO CONNECT": no protocol found. Usually the ignition is off. */
    UnableToConnect,

    /** "CAN ERROR", "BUS INIT: ...ERROR", "BUS BUSY", "DATA ERROR"…: the wire to the car misbehaved. */
    BusError,

    /** "STOPPED": a new byte arrived while the chip was still working (we talked over it). */
    Stopped,

    /** "BUFFER FULL": the chip could not forward data fast enough. */
    BufferFull,

    /** Only the prompt came back, nothing before it. */
    EmptyAnswer,
}

/**
 * [App 5 · B1] One command and its answer, as CommandQueue returns it. [raw] is kept for the Console tab
 * (learners want to see exactly what the dongle said); [result] is what the code uses.
 */
data class Exchange(val command: String, val raw: String, val result: ObdResult)
