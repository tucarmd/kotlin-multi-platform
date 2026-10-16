package dev.obdlink.obd.transport

/**
 * [App 5 · B3] PromptCollector — glues notification chunks back into one ELM327 answer.
 *
 * Why it exists: BLE delivers at most ~20 bytes per notification (the default ATT MTU minus headers), so
 * "41 0C 1A F8\r\r>" can arrive as "41 0C 1", "A F8\r", "\r>". The ELM327 always ends an answer with the prompt
 * `>`, which is the ONLY reliable "done" signal.
 *
 * TRAP: never treat a single notification as the answer, and never stop at "\r": multi-line answers
 * ("SEARCHING...\r41 0C …\r") contain several "\r" before the `>`.
 *
 * Used by: KableElmTransport, FakeElmTransport. Pure and synchronous, so PromptCollectorTest needs no coroutines.
 */
class PromptCollector {
    private val buffer = StringBuilder()

    /**
     * Adds one chunk. Returns the full answer (text before `>`) once the prompt has arrived, otherwise null.
     *
     * Example: feed("41 0C 1") → null, feed("A F8\r") → null, feed("\r>") → "41 0C 1A F8\r\r".
     */
    fun feed(chunk: String): String? {
        buffer.append(chunk)
        val prompt = buffer.indexOf(">")
        if (prompt < 0) return null
        val answer = buffer.substring(0, prompt)
        // Anything after '>' is noise from a previous, timed-out command: drop it with the answer.
        buffer.clear()
        return answer
    }

    /** Forgets a half-received answer (used before each new command). */
    fun reset() {
        buffer.clear()
    }
}
