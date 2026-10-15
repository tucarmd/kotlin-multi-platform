package dev.obdlink.obd

import dev.obdlink.obd.rules.ConsoleInput
import dev.obdlink.obd.rules.parseConsoleInput
import kotlin.test.Test
import kotlin.test.assertEquals

/** [App 5 · B7] What the Console tab may send. */
class ConsoleInputTest {

    @Test
    fun `spaces and case are normalised`() {
        assertEquals(ConsoleInput.Command("010C"), parseConsoleInput(" 01 0c "))
        assertEquals(ConsoleInput.Command("ATRV"), parseConsoleInput("atrv"))
    }

    @Test
    fun `blank input is empty`() {
        assertEquals(ConsoleInput.Empty, parseConsoleInput("   "))
    }

    @Test
    fun `non hex text is invalid`() {
        assertEquals(ConsoleInput.Invalid, parseConsoleInput("hello"))
        assertEquals(ConsoleInput.Invalid, parseConsoleInput("01 0X"))
    }

    @Test
    fun `mode 04 is never sent from the console`() {
        assertEquals(ConsoleInput.ClearBlocked, parseConsoleInput("04"))
        assertEquals(ConsoleInput.ClearBlocked, parseConsoleInput(" 0 4 "))
    }
}
