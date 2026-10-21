package com.learn.ndmkmp.presentation.scanner

import com.learn.ndmkmp.core.bluetooth.BluetoothAdvertisement
import com.learn.ndmkmp.core.bluetooth.BluetoothConnectionState
import com.learn.ndmkmp.core.bluetooth.BluetoothDeviceInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

/**
 * Device Name box rules + the 2 Connect/Disconnect buttons. Same cases as
 * `ios/NativeDeviceMonitorTests/ScannerActionsTests.swift`.
 * Run: `./gradlew :app:testDebugUnitTest`
 */
class ScannerActionsTest {

    private val idle = BluetoothConnectionState.Idle
    private val connecting = BluetoothConnectionState.Connecting
    private val connected = BluetoothConnectionState.Connected("CMD-TEST")

    // --- matchesName ---

    @Test
    fun emptyQuery_showsEverything_evenUnnamed() {
        assertTrue(matchesName("CMD-3C71BF", ""))
        assertTrue(matchesName(null, "   "))
    }

    @Test
    fun query_matchesContains_ignoringCase() {
        assertTrue(matchesName("CMD-3C71BF", "cmd"))
        assertTrue(matchesName("My CarMD2", "carmd"))
        assertFalse(matchesName("Pixel 8", "cmd"))
    }

    @Test
    fun query_hidesUnnamedDevices() {
        assertFalse(matchesName(null, "cmd"))
    }

    @Test
    fun query_isTrimmed() {
        assertTrue(matchesName("CMD-3C71BF", "  cmd "))
    }

    // --- scannerButtonsOf ---

    @Test
    fun noSelection_connectLocked() {
        assertEquals(ScannerButtons(false, false, false), scannerButtonsOf(false, null, false, idle, ready = true))
    }

    @Test
    fun selected_idle_ready_connectEnabled() {
        assertEquals(ScannerButtons(true, false, false), scannerButtonsOf(true, true, false, idle, ready = true))
    }

    @Test
    fun notReady_connectLocked() {
        assertEquals(ScannerButtons(false, false, false), scannerButtonsOf(true, true, false, idle, ready = false))
    }

    @Test
    fun nonConnectable_connectLocked() {
        assertEquals(ScannerButtons(false, false, false), scannerButtonsOf(true, false, false, idle, ready = true))
    }

    @Test
    fun unknownConnectable_stillAllowsTrying() {
        assertEquals(ScannerButtons(true, false, false), scannerButtonsOf(true, null, false, idle, ready = true))
    }

    @Test
    fun connecting_showsSpinner_bothLocked() {
        assertEquals(ScannerButtons(false, true, false), scannerButtonsOf(true, true, true, connecting, ready = true))
    }

    @Test
    fun connected_sameDeviceSelected_onlyDisconnect() {
        assertEquals(ScannerButtons(false, false, true), scannerButtonsOf(true, true, true, connected, ready = true))
    }

    @Test
    fun connected_otherDeviceSelected_canSwitch() {
        assertEquals(ScannerButtons(true, false, true), scannerButtonsOf(true, true, false, connected, ready = true))
    }

    @Test
    fun afterError_canConnectAgain() {
        val error = BluetoothConnectionState.Error("Unexpected disconnect")
        assertEquals(ScannerButtons(true, false, false), scannerButtonsOf(true, true, true, error, ready = true))
    }

    // --- Hide unknown devices · Hide audio devices ---

    /** Minimal advertising packet — each test only changes the field it checks. */
    private fun adv(
        serviceUuids: List<String> = emptyList(),
        appearance: Int? = null,
        serviceDataUuids: List<String> = emptyList(),
        appleType: Int? = null,
        isAudioClass: Boolean = false,
    ) = BluetoothAdvertisement(
        isConnectable = true, txPower = null, bondState = "Not bonded", flags = null,
        serviceUuids = serviceUuids, manufacturerData = emptyList(), serviceData = emptyList(),
        rawHex = "", packetCount = 1, intervalMs = null,
        appearance = appearance, serviceDataUuids = serviceDataUuids, appleType = appleType, isAudioClass = isAudioClass,
    )

    private fun device(name: String?, advertisement: BluetoothAdvertisement? = adv()) =
        BluetoothDeviceInfo(address = "AA:BB:CC:DD:EE:FF", name = name, rssi = -60, advertisement = advertisement)

    @Test
    fun unknownDevice_nullOrBlankName() {
        assertTrue(isUnknownDevice(null))
        assertTrue(isUnknownDevice("  "))
        assertFalse(isUnknownDevice("CMD-3C71BF"))
    }

    @Test
    fun audioDevice_eachSignalAloneIsEnough() {
        assertTrue(isAudioDevice(adv(appearance = 0x0941)))            // Earbud
        assertTrue(isAudioDevice(adv(appearance = 0x0843)))            // Audio Sink (loa)
        assertTrue(isAudioDevice(adv(serviceUuids = listOf("184E"))))  // LE Audio ASCS
        assertTrue(isAudioDevice(adv(serviceDataUuids = listOf("FE2C")))) // Fast Pair
        assertTrue(isAudioDevice(adv(appleType = 0x07)))               // AirPods proximity pairing
        assertTrue(isAudioDevice(adv(isAudioClass = true)))
    }

    @Test
    fun audioDevice_dongleAndOtherBleAreNotAudio() {
        assertFalse(isAudioDevice(adv(serviceUuids = listOf("FFE0"))))  // dongle CarMD (UART)
        assertFalse(isAudioDevice(adv(serviceUuids = listOf("180D"))))  // nRF Advertiser Heart Rate
        assertFalse(isAudioDevice(adv(appearance = 0x0340)))            // Heart Rate Sensor
        assertFalse(isAudioDevice(adv(appleType = 0x12)))               // Find My / AirTag
        assertFalse(isAudioDevice(null))
    }

    @Test
    fun shown_togglesOff_showsEverything() {
        assertTrue(isDeviceShown(device(null), "", hideUnknown = false, hideAudio = false, isPinned = false))
        assertTrue(isDeviceShown(device("Buds", adv(appleType = 0x07)), "", false, false, false))
    }

    @Test
    fun shown_togglesHideMatchingDevices() {
        assertFalse(isDeviceShown(device(null), "", hideUnknown = true, hideAudio = false, isPinned = false))
        assertFalse(isDeviceShown(device("AirPods", adv(appleType = 0x07)), "", false, hideAudio = true, isPinned = false))
        assertTrue(isDeviceShown(device("CMD-Test", adv(serviceUuids = listOf("180D"))), "", true, true, false))
    }

    @Test
    fun shown_nameQueryAndTogglesCombine() {
        val audioCmd = device("CMD-Buds", adv(serviceDataUuids = listOf("FE2C")))
        assertTrue(isDeviceShown(audioCmd, "cmd", hideUnknown = false, hideAudio = false, isPinned = false))
        assertFalse(isDeviceShown(audioCmd, "cmd", hideUnknown = false, hideAudio = true, isPinned = false))
        assertFalse(isDeviceShown(device("Pixel 8"), "cmd", false, false, false))
    }

    @Test
    fun shown_pinnedDeviceIgnoresToggles_butNotNameQuery() {
        assertTrue(isDeviceShown(device(null), "", hideUnknown = true, hideAudio = true, isPinned = true))
        assertTrue(isDeviceShown(device("AirPods", adv(appleType = 0x07)), "", true, true, isPinned = true))
        assertFalse(isDeviceShown(device("AirPods", adv(appleType = 0x07)), "cmd", true, true, isPinned = true))
    }
}
