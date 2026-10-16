package com.learn.ndmkmp.core.bluetooth

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** Same cases as `ios/NativeDeviceMonitorTests/BluetoothLookupTests.swift`. */
class BluetoothLookupTest {

    @Test
    fun companyName_knownAndUnknown() {
        assertEquals("Apple", BluetoothLookup.companyName(0x004C))
        assertEquals("Nordic Semiconductor", BluetoothLookup.companyName(0x0059))
        assertNull(BluetoothLookup.companyName(0xFFFF))
    }

    @Test
    fun serviceName_shortUuid_caseInsensitive() {
        assertEquals("Battery", BluetoothLookup.serviceName("180F"))
        assertEquals("UART (CarMD dongle)", BluetoothLookup.serviceName("ffe0"))
        assertEquals("Nordic UART", BluetoothLookup.serviceName("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))
        assertNull(BluetoothLookup.serviceName("1234"))
    }

    @Test
    fun signal_thresholds() {
        assertEquals(BleSignal.STRONG, BleSignal.of(-40))
        assertEquals(BleSignal.STRONG, BleSignal.of(-60))   // boundary: -60 is still strong
        assertEquals(BleSignal.MEDIUM, BleSignal.of(-61))
        assertEquals(BleSignal.MEDIUM, BleSignal.of(-80))
        assertEquals(BleSignal.WEAK, BleSignal.of(-81))
        assertEquals(3, BleSignal.of(-50).bars)
    }

    @Test
    fun stale_after5Seconds() {
        assertFalse(BluetoothLookup.isStale(lastSeenMs = 10_000, nowMs = 15_000))   // exactly 5 s: not faded yet
        assertTrue(BluetoothLookup.isStale(lastSeenMs = 10_000, nowMs = 15_001))
    }

    @Test
    fun audioAppearance_categories() {
        assertTrue(BluetoothLookup.isAudioAppearance(0x0840))  // Audio Sink
        assertTrue(BluetoothLookup.isAudioAppearance(0x0880))  // Audio Source
        assertTrue(BluetoothLookup.isAudioAppearance(0x0942))  // Headset
        assertFalse(BluetoothLookup.isAudioAppearance(0x0340)) // Heart Rate Sensor
        assertFalse(BluetoothLookup.isAudioAppearance(0x0000)) // Unknown
    }

    @Test
    fun appearanceOf_readsAdType0x19_littleEndian() {
        // flags · appearance 0x0941 · name "Buds"
        val bytes = byteArrayOf(0x02, 0x01, 0x06, 0x03, 0x19, 0x41, 0x09, 0x05, 0x09, 0x42, 0x75, 0x64, 0x73, 0, 0, 0)
        assertEquals(0x0941, BluetoothDecoder.appearanceOf(bytes))
    }

    @Test
    fun appearanceOf_missingOrTruncated_isNull() {
        assertNull(BluetoothDecoder.appearanceOf(byteArrayOf(0x02, 0x01, 0x06, 0, 0)))  // flags only
        assertNull(BluetoothDecoder.appearanceOf(byteArrayOf(0x03, 0x19, 0x41)))      // truncated block
        assertNull(BluetoothDecoder.appearanceOf(ByteArray(0)))
    }
}
