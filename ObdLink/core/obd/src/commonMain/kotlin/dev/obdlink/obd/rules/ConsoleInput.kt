package dev.obdlink.obd.rules

/**
 * [App 5 · B1] Console input rule: what the learner typed → a command we may send, or why not.
 *
 * Why a rule and not a check in the UI: both UIs get the same behaviour, and the "mode 04 is never sent from the
 * console" safety rule is tested once instead of trusted twice.
 *
 * Called by: ObdCore.sendConsole. Calls: nothing.
 */
sealed class ConsoleInput {
    /** Ready to send. [text] is normalised: upper-case, no spaces. */
    data class Command(val text: String) : ConsoleInput()

    /** Nothing typed. */
    data object Empty : ConsoleInput()

    /** Not an AT command nor hex (e.g. "hello", "01 0X"). */
    data object Invalid : ConsoleInput()

    /** Mode 04 (clear codes). Only the Codes tab may send it, after a confirmation. */
    data object ClearBlocked : ConsoleInput()
}

/**
 * Normalises and checks one console line.
 *
 * Examples: `" 01 0c "` → Command("010C") · `"atrv"` → Command("ATRV") · `""` → Empty ·
 * `"hello"` → Invalid · `"04"` → ClearBlocked.
 */
fun parseConsoleInput(typed: String): ConsoleInput {
    val text = typed.filterNot { it.isWhitespace() }.uppercase()
    if (text.isEmpty()) return ConsoleInput.Empty
    // Longest real command is about 10 chars ("ATSH7E0" etc.); 20 also keeps it inside one 20-byte BLE write.
    if (text.length > 20) return ConsoleInput.Invalid
    if (text.startsWith("AT")) {
        return if (text.drop(2).all { it.isLetterOrDigit() || it == '@' }) ConsoleInput.Command(text) else ConsoleInput.Invalid
    }
    if (!text.all { it in '0'..'9' || it in 'A'..'F' }) return ConsoleInput.Invalid
    // TRAP: "04" clears codes AND readiness monitors. A console shortcut would skip the confirmation dialog.
    if (text.startsWith("04")) return ConsoleInput.ClearBlocked
    return ConsoleInput.Command(text)
}
