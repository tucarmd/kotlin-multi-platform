package dev.obdlink.obd

import dev.obdlink.obd.model.DtcSystem
import dev.obdlink.obd.rules.decodeDtc
import dev.obdlink.obd.rules.decodeDtcResponse
import dev.obdlink.obd.rules.isClearConfirmed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [App 5 · B7] DTC bit layout: the two top bits pick P C B U, then four digits. */
class DtcTest {

    @Test
    fun `43 01 03 01 decodes to P0301`() {
        val codes = decodeDtcResponse(listOf(listOf(0x43, 0x01, 0x03, 0x01)))
        assertEquals(listOf("P0301"), codes.map { it.code })
        assertEquals("Cylinder 1 misfire", codes.single().summary)
    }

    @Test
    fun `top two bits choose the letter`() {
        assertEquals("P0171", decodeDtc(0x01, 0x71).code)
        assertEquals("C0123", decodeDtc(0x41, 0x23).code)
        assertEquals("B1234", decodeDtc(0x92, 0x34).code)
        assertEquals("U0100", decodeDtc(0xC1, 0x00).code)
        assertEquals(DtcSystem.Network, decodeDtc(0xC1, 0x00).system)
    }

    @Test
    fun `hex digits A to F stay letters`() {
        assertEquals("P2AFF", decodeDtc(0x2A, 0xFF).code)
    }

    @Test
    fun `manufacturer codes are not generic`() {
        assertFalse(decodeDtc(0x12, 0x34).isGeneric) // P1234
        assertTrue(decodeDtc(0x22, 0x34).isGeneric) // P2234
    }

    @Test
    fun `older protocols send pairs without a count and pad with zeros`() {
        val codes = decodeDtcResponse(listOf(listOf(0x43, 0x03, 0x01, 0x01, 0x71, 0x00, 0x00)))
        assertEquals(listOf("P0301", "P0171"), codes.map { it.code })
    }

    @Test
    fun `no stored codes gives an empty list`() {
        assertEquals(emptyList(), decodeDtcResponse(listOf(listOf(0x43, 0x00))))
    }

    @Test
    fun `same code from two ECUs is listed once`() {
        val messages = listOf(listOf(0x43, 0x01, 0x03, 0x01), listOf(0x43, 0x01, 0x03, 0x01))
        assertEquals(1, decodeDtcResponse(messages).size)
    }

    @Test
    fun `multi frame answer uses the count byte`() {
        val message = listOf(0x43, 0x04, 0x01, 0x03, 0x01, 0x04, 0x04, 0x20, 0x01, 0x71)
        assertEquals(listOf("P0103", "P0104", "P0420", "P0171"), decodeDtcResponse(listOf(message)).map { it.code })
    }

    @Test
    fun `clear is confirmed by 44`() {
        assertTrue(isClearConfirmed(listOf(listOf(0x44))))
        assertFalse(isClearConfirmed(listOf(listOf(0x7F, 0x04, 0x22))))
    }
}
