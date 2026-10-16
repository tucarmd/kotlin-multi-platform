package com.learn.ndmkmp.core.bluetooth

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** Day 3 + Day 4 + task 004 (write/MTU) — same cases as `ios/NativeDeviceMonitorTests/GattModelTests.swift`. */
class GattModelTest {

    // --- GattProperties.of: bitmask → flags ---

    @Test
    fun properties_readNotify_0x12() {
        val p = GattProperties.of(0x12)
        assertTrue(p.read)
        assertTrue(p.notify)
        assertFalse(p.write)
        assertTrue(p.canSubscribe)
        assertEquals(listOf("READ", "NOTIFY"), p.labels)
    }

    @Test
    fun properties_uartWriteNotify_0x1C() {
        // HM-10 dongle FFE1: WRITE_NO_RESPONSE + WRITE + NOTIFY, no READ
        val p = GattProperties.of(0x1C)
        assertFalse(p.read)
        assertEquals(listOf("WRITE", "WRITE NO RESP", "NOTIFY"), p.labels)
    }

    @Test
    fun properties_indicateOnly_canSubscribe() {
        val p = GattProperties.of(0x20)
        assertFalse(p.notify)
        assertTrue(p.canSubscribe)
        assertTrue(GattProperties.of(0x00).labels.isEmpty())
    }

    // --- BluetoothDecoder.decode: bytes → value ---

    @Test
    fun decode_battery() {
        assertEquals("90 %", BluetoothDecoder.decode("2A19", bytes(0x5A)))
    }

    @Test
    fun decode_utf8Strings() {
        assertEquals("Nordic", BluetoothDecoder.decode("2A29", "Nordic".encodeToByteArray()))
        assertEquals("CMD-01", BluetoothDecoder.decode("2a00", "CMD-01".encodeToByteArray() + 0))   // lowercase + trailing 0 byte
    }

    @Test
    fun decode_heartRate_uint8AndUint16() {
        assertEquals("72 bpm", BluetoothDecoder.decode("2A37", bytes(0x00, 0x48)))
        assertEquals("300 bpm", BluetoothDecoder.decode("2A37", bytes(0x01, 0x2C, 0x01)))
        assertEquals("—", BluetoothDecoder.decode("2A37", bytes(0x01, 0x2C)))                // truncated
    }

    @Test
    fun decode_unknown_asciiText() {
        assertEquals("\"AT\\r\"", BluetoothDecoder.decode("FFE1", bytes(0x41, 0x54, 0x0D)))
    }

    @Test
    fun decode_unknown_littleEndianNumbers() {
        assertEquals("255 (uint8)", BluetoothDecoder.decode("1234", bytes(0xFF)))
        assertEquals("4660 (uint16 LE)", BluetoothDecoder.decode("1234", bytes(0x34, 0x12)))
        assertEquals("4294967295 (uint32 LE)", BluetoothDecoder.decode("1234", bytes(0xFF, 0xFF, 0xFF, 0xFF)))
        assertEquals("—", BluetoothDecoder.decode("1234", bytes(0x01, 0x02, 0x03)))
        assertEquals("(empty)", BluetoothDecoder.decode("2A19", ByteArray(0)))
    }

    // --- Target characteristic ---

    @Test
    fun readTarget_prefersBattery_thenFirstReadable() {
        val withBattery = listOf(
            service("1800", chr("1800", "2A00", 0x02)),
            service("180F", chr("180F", "2A19", 0x12)),
        )
        assertEquals("2A19", readTargetOf(withBattery)?.uuid)

        val noKnown = listOf(service("AAAA", chr("AAAA", "BBBB", 0x10), chr("AAAA", "CCCC", 0x02)))
        assertEquals("CCCC", readTargetOf(noKnown)?.uuid)
        assertNull(readTargetOf(listOf(service("AAAA", chr("AAAA", "BBBB", 0x10)))))
    }

    @Test
    fun notifyTarget_prefersDongleUart_thenFirstSubscribable() {
        val dongle = listOf(
            service("180F", chr("180F", "2A19", 0x12)),
            service("FFE0", chr("FFE0", "FFE1", 0x1C)),
        )
        assertEquals("FFE1", notifyTargetOf(dongle)?.uuid)

        val indicateOnly = listOf(service("AAAA", chr("AAAA", "BBBB", 0x02), chr("AAAA", "CCCC", 0x20)))
        assertEquals("CCCC", notifyTargetOf(indicateOnly)?.uuid)
        assertNull(notifyTargetOf(emptyList()))
    }

    // --- Received Data cap ---

    @Test
    fun appendCapped_dropsOldest() {
        assertEquals(listOf(1, 2, 3), appendCapped(listOf(1, 2), 3, max = 3))
        assertEquals(listOf(2, 3, 4), appendCapped(listOf(1, 2, 3), 4, max = 3))
    }

    // --- Task 004: Write + MTU ---

    @Test
    fun fromHex_acceptsCommonSpellings() {
        val atz = bytes(0x41, 0x54, 0x5A, 0x0D)
        assertContentEquals(atz, BluetoothDecoder.fromHex("41 54 5A 0D"))
        assertContentEquals(atz, BluetoothDecoder.fromHex("41545a0d"))
        assertContentEquals(atz, BluetoothDecoder.fromHex("0x41,0x54,0x5A,0x0D"))
        assertContentEquals(atz, BluetoothDecoder.fromHex(" 41-54:5A 0d "))
        assertContentEquals(bytes(0xFF), BluetoothDecoder.fromHex("FF"))
    }

    @Test
    fun fromHex_rejectsInvalid() {
        assertNull(BluetoothDecoder.fromHex(""))
        assertNull(BluetoothDecoder.fromHex("   "))
        assertNull(BluetoothDecoder.fromHex("4"))          // odd length
        assertNull(BluetoothDecoder.fromHex("4G"))         // not hex
        assertNull(BluetoothDecoder.fromHex("41 5"))
    }

    @Test
    fun fromText_expandsEscapes() {
        assertContentEquals(bytes(0x41, 0x54, 0x5A, 0x0D), BluetoothDecoder.fromText("ATZ\\r"))
        assertContentEquals(bytes(0x30, 0x31, 0x30, 0x43, 0x0D, 0x0A), BluetoothDecoder.fromText("010C\\r\\n"))
        assertContentEquals(bytes(0x5C, 0x09), BluetoothDecoder.fromText("\\\\\\t"))     // \\ → \   \t → tab
        assertContentEquals(bytes(0x41, 0x5C), BluetoothDecoder.fromText("A\\"))          // trailing \ kept as-is
        assertContentEquals(bytes(0x5C, 0x78), BluetoothDecoder.fromText("\\x"))          // unknown escape kept as-is
        assertNull(BluetoothDecoder.fromText(""))
    }

    @Test
    fun writeTarget_prefersDongleUart_thenNordicRx_thenFirstWritable() {
        val dongle = listOf(
            service("AAAA", chr("AAAA", "BBBB", 0x08)),
            service("FFE0", chr("FFE0", "FFE1", 0x1C)),
        )
        assertEquals("FFE1", writeTargetOf(dongle)?.uuid)

        val nordic = listOf(
            service(
                "6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
                chr("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", "6E400003-B5A3-F393-E0A9-E50E24DCCA9E", 0x10),
                chr("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", "6E400002-B5A3-F393-E0A9-E50E24DCCA9E", 0x0C),
            ),
        )
        assertEquals("6E400002-B5A3-F393-E0A9-E50E24DCCA9E", writeTargetOf(nordic)?.uuid)

        val noResponseOnly = listOf(service("AAAA", chr("AAAA", "BBBB", 0x02), chr("AAAA", "CCCC", 0x04)))
        assertEquals("CCCC", writeTargetOf(noResponseOnly)?.uuid)
        assertNull(writeTargetOf(listOf(service("180F", chr("180F", "2A19", 0x12)))))
    }

    @Test
    fun maxPayload_isMtuMinusHeader() {
        assertEquals(20, maxPayloadOf(ATT_DEFAULT_MTU))
        assertEquals(244, maxPayloadOf(ATT_REQUESTED_MTU))
        assertEquals(0, maxPayloadOf(0))
    }

    @Test
    fun planWrite_okPicksWriteType() {
        val both = planWrite("ATZ\\r", asHex = false, GattProperties.of(0x0C), maxPayload = 20) as WritePlan.Ok
        assertContentEquals(bytes(0x41, 0x54, 0x5A, 0x0D), both.bytes)
        assertTrue(both.withResponse)                          // has WRITE → prefer ACK

        val noResp = planWrite("01", asHex = true, GattProperties.of(0x04), maxPayload = 20) as WritePlan.Ok
        assertFalse(noResp.withResponse)
    }

    @Test
    fun planWrite_invalidReasons() {
        val writable = GattProperties.of(0x08)
        assertEquals(WritePlan.Invalid("This characteristic is not writable"), planWrite("01", true, GattProperties.of(0x12), 20))
        assertEquals(WritePlan.Invalid("Type something to send"), planWrite("", false, writable, 20))
        assertEquals(WritePlan.Invalid("Invalid hex — use pairs like 41 54 5A 0D"), planWrite("4G", true, writable, 20))
        assertEquals(WritePlan.Invalid("21 bytes > max 20 for MTU 23"), planWrite("A".repeat(21), false, writable, 20))
        assertTrue(planWrite("A".repeat(20), false, writable, 20) is WritePlan.Ok)   // exactly at the limit still sends
    }

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }
    private fun chr(service: String, uuid: String, props: Int) = GattCharacteristicInfo(service, uuid, GattProperties.of(props))
    private fun service(uuid: String, vararg c: GattCharacteristicInfo) = GattServiceInfo(uuid, c.toList())
}
