package dev.obdlink.obd

import dev.obdlink.obd.transport.PromptCollector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [App 5 · B7] Answers are only complete at the prompt, however BLE cut them. */
class PromptCollectorTest {

    @Test
    fun `answer split into 3 chunks is complete only at the prompt`() {
        val collector = PromptCollector()
        assertNull(collector.feed("41 0C 1"))
        assertNull(collector.feed("A F8\r"))
        assertEquals("41 0C 1A F8\r\r", collector.feed("\r>"))
    }

    @Test
    fun `carriage return alone does not end the answer`() {
        val collector = PromptCollector()
        assertNull(collector.feed("SEARCHING...\r"))
        assertEquals("SEARCHING...\r41 0D 00\r", collector.feed("41 0D 00\r>"))
    }

    @Test
    fun `collector starts empty after each answer`() {
        val collector = PromptCollector()
        collector.feed("OK\r>")
        assertEquals("NO DATA\r", collector.feed("NO DATA\r>"))
    }

    @Test
    fun `reset forgets a half answer`() {
        val collector = PromptCollector()
        collector.feed("41 0C")
        collector.reset()
        assertEquals("OK", collector.feed("OK>"))
    }
}
