package dev.obdlink.obd

import dev.obdlink.obd.model.ElmFailure
import dev.obdlink.obd.model.ObdResult
import dev.obdlink.obd.rules.cleanLines
import dev.obdlink.obd.rules.displayAnswer
import dev.obdlink.obd.rules.parseResponse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [App 5 · B7] parseResponse with answers shaped like real ELM327 clones send them
 * (echo on/off, spaces on/off, "\r" line ends, SEARCHING..., multi-frame CAN).
 */
class ObdParserTest {

    @Test
    fun `rpm answer with spaces becomes one data message`() {
        assertEquals(ObdResult.Data(listOf(listOf(0x41, 0x0C, 0x1A, 0xF8))), parseResponse("41 0C 1A F8\r\r>"))
    }

    @Test
    fun `answer without spaces gives the same bytes`() {
        assertEquals(ObdResult.Data(listOf(listOf(0x41, 0x0D, 0x32))), parseResponse("410D32\r>"))
    }

    @Test
    fun `searching line and echo are dropped`() {
        val raw = "010C\rSEARCHING...\r41 0C 0F A0\r\r>"
        assertEquals(ObdResult.Data(listOf(listOf(0x41, 0x0C, 0x0F, 0xA0))), parseResponse(raw, sent = "010C"))
    }

    @Test
    fun `two ECUs answering give two messages`() {
        val result = parseResponse("41 0D 00\r41 0D 00\r\r>")
        assertEquals(ObdResult.Data(listOf(listOf(0x41, 0x0D, 0x00), listOf(0x41, 0x0D, 0x00))), result)
    }

    @Test
    fun `NO DATA is its own result and not an error`() {
        assertEquals(ObdResult.NoData, parseResponse("NO DATA\r\r>"))
        assertEquals(ObdResult.NoData, parseResponse("SEARCHING...\rNO DATA\r\r>"))
    }

    @Test
    fun `question mark means unknown command`() {
        assertEquals(ObdResult.Error(ElmFailure.UnknownCommand), parseResponse("?\r\r>"))
    }

    @Test
    fun `bus and connection failures are classified`() {
        assertEquals(ObdResult.Error(ElmFailure.UnableToConnect), parseResponse("SEARCHING...\rUNABLE TO CONNECT\r\r>"))
        assertEquals(ObdResult.Error(ElmFailure.BusError), parseResponse("BUS INIT: ...ERROR\r\r>"))
        assertEquals(ObdResult.Error(ElmFailure.BusError), parseResponse("CAN ERROR\r\r>"))
        assertEquals(ObdResult.Error(ElmFailure.Stopped), parseResponse("STOPPED\r\r>"))
        assertEquals(ObdResult.Error(ElmFailure.BufferFull), parseResponse("BUFFER FULL\r>"))
    }

    @Test
    fun `only a prompt is an empty answer`() {
        assertEquals(ObdResult.Error(ElmFailure.EmptyAnswer), parseResponse("\r\r>"))
    }

    @Test
    fun `OK with echo still parses as OK`() {
        assertEquals(ObdResult.Ok, parseResponse("ATE0\rOK\r\r>", sent = "ATE0"))
        assertEquals(ObdResult.Ok, parseResponse("OK\r\r>"))
    }

    @Test
    fun `ATZ banner is plain text`() {
        assertEquals(ObdResult.Text("ELM327 v1.5"), parseResponse("ATZ\r\r\rELM327 v1.5\r\r>", sent = "ATZ"))
    }

    @Test
    fun `multi frame CAN answer is joined into one message cut to the byte count`() {
        val raw = "00A\r0: 43 04 01 03 01 04\r1: 04 20 01 71 00 00\r\r>"
        val expected = listOf(0x43, 0x04, 0x01, 0x03, 0x01, 0x04, 0x04, 0x20, 0x01, 0x71)
        assertEquals(ObdResult.Data(listOf(expected)), parseResponse(raw))
    }

    @Test
    fun `newline line ends work like carriage returns`() {
        assertEquals(listOf("41 0C 1A F8"), cleanLines("41 0C 1A F8\n\n>"))
    }

    @Test
    fun `console text drops the prompt and blank lines`() {
        assertEquals("41 0C 1A F8", displayAnswer("010C\r41 0C 1A F8\r\r>", sent = "010C"))
    }
}
