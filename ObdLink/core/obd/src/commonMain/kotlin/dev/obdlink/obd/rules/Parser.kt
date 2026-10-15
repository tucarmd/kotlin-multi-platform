package dev.obdlink.obd.rules

import dev.obdlink.obd.model.ElmFailure
import dev.obdlink.obd.model.ObdResult

/**
 * [App 5 · B1] Parser — turns the raw text an ELM327 sends back into an [ObdResult].
 *
 * Why a pure function: cheap dongles answer with every kind of noise (our own command echoed back, "SEARCHING...",
 * "\r" instead of "\n", spaces or no spaces, a trailing ">"). All of that is handled HERE, once, and covered by
 * ObdParserTest with answers copied from real dongles. Nothing else in the app looks at raw text.
 *
 * Called by: CommandQueue (every answer), ObdCore (console log). Calls: nothing.
 */

/**
 * Splits [raw] into clean lines: no ">", no blank lines, no "SEARCHING...", no echo of [sent].
 *
 * Example: `"010C\r SEARCHING...\r41 0C 1A F8\r\r>"`, sent = "010C" → `["41 0C 1A F8"]`.
 */
fun cleanLines(raw: String, sent: String? = null): List<String> {
    // TRAP: ELM327 ends lines with "\r" only (no "\n"). Splitting on "\n" alone gives ONE long line.
    val lines = raw.replace('>', '\r')
        .split('\r', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filterNot { it.uppercase().startsWith("SEARCHING") }
    val echo = sent?.compact()
    // Echo is ON after power-up and after ATZ, so the first line can be our own command. Drop only that line.
    return if (echo != null && lines.firstOrNull()?.compact() == echo) lines.drop(1) else lines
}

/**
 * Classifies one answer. [sent] (optional) is the command, used to drop its echo.
 *
 * Examples:
 * - `"41 0C 1A F8\r\r>"` → `Data([[0x41, 0x0C, 0x1A, 0xF8]])`
 * - `"410D32>"` (spaces off) → `Data([[0x41, 0x0D, 0x32]])`
 * - `"NO DATA\r\r>"` → `NoData`
 * - `"?\r>"` → `Error(UnknownCommand)`
 * - `"ATE0\rOK\r\r>"`, sent = "ATE0" → `Ok`
 * - `"ELM327 v1.5\r\r>"` → `Text("ELM327 v1.5")`
 */
fun parseResponse(raw: String, sent: String? = null): ObdResult {
    val lines = cleanLines(raw, sent)
    if (lines.isEmpty()) return ObdResult.Error(ElmFailure.EmptyAnswer)
    val upper = lines.map { it.uppercase() }

    failureOf(upper)?.let { return it }
    if (upper.all { it == "OK" }) return ObdResult.Ok

    val messages = hexMessages(upper) ?: return ObdResult.Text(lines.joinToString("\n"))
    return ObdResult.Data(messages)
}

/** Raw answer as one tidy string for the Console tab: `"41 0C 1A F8\r\r>"` → `"41 0C 1A F8"`. */
fun displayAnswer(raw: String, sent: String? = null): String = cleanLines(raw, sent).joinToString("\n")

private fun failureOf(upper: List<String>): ObdResult? = when {
    // NO DATA is checked first: some clones print "NO DATA" after a "SEARCHING..." that already failed.
    upper.any { it == "NO DATA" } -> ObdResult.NoData
    upper.any { it == "?" } -> ObdResult.Error(ElmFailure.UnknownCommand)
    upper.any { it.startsWith("UNABLE TO CONNECT") } -> ObdResult.Error(ElmFailure.UnableToConnect)
    upper.any { it == "STOPPED" } -> ObdResult.Error(ElmFailure.Stopped)
    upper.any { it.startsWith("BUFFER FULL") } -> ObdResult.Error(ElmFailure.BufferFull)
    upper.any { "ERROR" in it || it == "BUS BUSY" } -> ObdResult.Error(ElmFailure.BusError)
    else -> null
}

/**
 * Hex lines → messages. Returns null as soon as one line is not hex (then the answer is plain text).
 *
 * TRAP: a CAN answer longer than 7 bytes (mode 03 with 3+ codes) comes as ISO-TP frames:
 * ```
 * 00A            ← total byte count (3 hex digits, odd length!)
 * 0: 43 04 01 03 01 04
 * 1: 04 20 01 71 00 00
 * ```
 * Those lines are ONE message. Treating them as separate lines would decode "1: 04 20" as a new answer.
 */
private fun hexMessages(upper: List<String>): List<List<Int>>? {
    val messages = mutableListOf<List<Int>>()
    var frames: MutableList<Int>? = null
    var expected: Int? = null

    fun flushFrames() {
        val joined = frames ?: return
        messages += expected?.let { joined.take(it) } ?: joined
        frames = null
        expected = null
    }

    for (line in upper) {
        val compact = line.compact()
        val frameIndex = Regex("^[0-9A-F]:").find(compact)
        when {
            compact.length == 3 && compact.isHex() -> {
                flushFrames()
                expected = compact.toInt(16)
                frames = mutableListOf()
            }
            frameIndex != null -> {
                val bytes = hexBytes(compact.substring(2)) ?: return null
                (frames ?: mutableListOf<Int>().also { frames = it }).addAll(bytes)
            }
            else -> {
                flushFrames()
                messages += hexBytes(compact) ?: return null
            }
        }
    }
    flushFrames()
    return messages
}

/** `"410C1AF8"` → `[0x41, 0x0C, 0x1A, 0xF8]`; odd length or a non-hex char → null. */
internal fun hexBytes(compact: String): List<Int>? {
    if (compact.isEmpty() || compact.length % 2 != 0 || !compact.isHex()) return null
    return compact.chunked(2).map { it.toInt(16) }
}

private fun String.isHex(): Boolean = all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }

private fun String.compact(): String = filterNot { it == ' ' }.uppercase()
