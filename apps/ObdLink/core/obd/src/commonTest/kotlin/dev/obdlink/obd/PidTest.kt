package dev.obdlink.obd

import dev.obdlink.obd.model.ObdPid
import dev.obdlink.obd.rules.PidReading
import dev.obdlink.obd.rules.decodePid
import dev.obdlink.obd.rules.decodePidAnswer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [App 5 · B7] SAE J1979 formulas with values worked out by hand. */
class PidTest {

    @Test
    fun `41 0C 1A F8 is 1726 rpm`() {
        assertEquals(PidReading(ObdPid.EngineRpm, 1726), decodePid(ObdPid.EngineRpm, listOf(0x41, 0x0C, 0x1A, 0xF8)))
    }

    @Test
    fun `rpm uses both bytes`() {
        // 0x0BB8 = 3000 quarter-rpm = 750 rpm (idle)
        assertEquals(750, decodePid(ObdPid.EngineRpm, listOf(0x41, 0x0C, 0x0B, 0xB8))?.value)
    }

    @Test
    fun `speed is byte A in kmh`() {
        assertEquals(50, decodePid(ObdPid.VehicleSpeed, listOf(0x41, 0x0D, 0x32))?.value)
    }

    @Test
    fun `coolant is A minus 40 and can be negative`() {
        assertEquals(83, decodePid(ObdPid.CoolantTemp, listOf(0x41, 0x05, 0x7B))?.value)
        assertEquals(-20, decodePid(ObdPid.CoolantTemp, listOf(0x41, 0x05, 0x14))?.value)
    }

    @Test
    fun `answer for another pid is rejected`() {
        assertNull(decodePid(ObdPid.EngineRpm, listOf(0x41, 0x0D, 0x32)))
    }

    @Test
    fun `wrong mode byte or short answer is rejected`() {
        assertNull(decodePid(ObdPid.VehicleSpeed, listOf(0x43, 0x0D, 0x32)))
        assertNull(decodePid(ObdPid.EngineRpm, listOf(0x41, 0x0C, 0x1A)))
    }

    @Test
    fun `first ECU that answers the pid wins`() {
        val messages = listOf(listOf(0x7F, 0x01, 0x12), listOf(0x41, 0x0D, 0x3C))
        assertEquals(60, decodePidAnswer(ObdPid.VehicleSpeed, messages)?.value)
    }
}
