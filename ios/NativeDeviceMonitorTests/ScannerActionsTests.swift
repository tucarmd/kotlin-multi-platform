import Foundation
import Testing
@testable import NativeDeviceMonitor

/// Device Name box rules + the 2 Connect/Disconnect buttons. Same cases as
/// `android/.../ui/scanner/ScannerActionsTest.kt`.
struct ScannerActionsTests {

    private let connected = BluetoothConnectionState.connected(name: "CMD-TEST")

    // MARK: - matchesName

    @Test func emptyQueryShowsEverythingEvenUnnamed() {
        #expect(matchesName("CMD-3C71BF", query: ""))
        #expect(matchesName(nil, query: "   "))
    }

    @Test func queryMatchesContainsIgnoringCase() {
        #expect(matchesName("CMD-3C71BF", query: "cmd"))
        #expect(matchesName("My CarMD2", query: "carmd"))
        #expect(!matchesName("Pixel 8", query: "cmd"))
    }

    @Test func queryHidesUnnamedDevices() {
        #expect(!matchesName(nil, query: "cmd"))
    }

    @Test func queryIsTrimmed() {
        #expect(matchesName("CMD-3C71BF", query: "  cmd "))
    }

    // MARK: - scannerButtonsOf

    @Test func noSelectionConnectLocked() {
        #expect(scannerButtonsOf(hasSelection: false, selectedIsConnectable: nil, selectedIsConnectedDevice: false,
                                 connection: .idle, ready: true)
                == ScannerButtons(connectEnabled: false, connecting: false, disconnectEnabled: false))
    }

    @Test func selectedIdleReadyConnectEnabled() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: true, selectedIsConnectedDevice: false,
                                 connection: .idle, ready: true)
                == ScannerButtons(connectEnabled: true, connecting: false, disconnectEnabled: false))
    }

    @Test func notReadyConnectLocked() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: true, selectedIsConnectedDevice: false,
                                 connection: .idle, ready: false)
                == ScannerButtons(connectEnabled: false, connecting: false, disconnectEnabled: false))
    }

    @Test func nonConnectableConnectLocked() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: false, selectedIsConnectedDevice: false,
                                 connection: .idle, ready: true)
                == ScannerButtons(connectEnabled: false, connecting: false, disconnectEnabled: false))
    }

    @Test func unknownConnectableStillAllowsTrying() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: nil, selectedIsConnectedDevice: false,
                                 connection: .idle, ready: true)
                == ScannerButtons(connectEnabled: true, connecting: false, disconnectEnabled: false))
    }

    @Test func connectingShowsSpinnerBothLocked() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: true, selectedIsConnectedDevice: true,
                                 connection: .connecting, ready: true)
                == ScannerButtons(connectEnabled: false, connecting: true, disconnectEnabled: false))
    }

    @Test func connectedSameDeviceSelectedOnlyDisconnect() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: true, selectedIsConnectedDevice: true,
                                 connection: connected, ready: true)
                == ScannerButtons(connectEnabled: false, connecting: false, disconnectEnabled: true))
    }

    @Test func connectedOtherDeviceSelectedCanSwitch() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: true, selectedIsConnectedDevice: false,
                                 connection: connected, ready: true)
                == ScannerButtons(connectEnabled: true, connecting: false, disconnectEnabled: true))
    }

    @Test func afterErrorCanConnectAgain() {
        #expect(scannerButtonsOf(hasSelection: true, selectedIsConnectable: true, selectedIsConnectedDevice: true,
                                 connection: .error(message: "Unexpected disconnect"), ready: true)
                == ScannerButtons(connectEnabled: true, connecting: false, disconnectEnabled: false))
    }

    // MARK: - Hide unknown devices · Hide audio devices
    // Same cases as Android, except Appearance / device class (iOS can't read them).

    private func adv(serviceUuids: [String] = [], serviceDataUuids: [String] = [], appleType: UInt8? = nil) -> BluetoothAdvertisement {
        BluetoothAdvertisement(isConnectable: true, txPower: nil, serviceUuids: serviceUuids, manufacturerData: [],
                               serviceData: [], packetCount: 1, intervalMs: nil,
                               serviceDataUuids: serviceDataUuids, appleType: appleType)
    }

    private func device(_ name: String?, _ advertisement: BluetoothAdvertisement? = nil) -> BluetoothDeviceInfo {
        BluetoothDeviceInfo(id: UUID(), name: name, rssi: -60, advertisement: advertisement ?? adv())
    }

    @Test func unknownDeviceNullOrBlankName() {
        #expect(isUnknownDevice(nil))
        #expect(isUnknownDevice("  "))
        #expect(!isUnknownDevice("CMD-3C71BF"))
    }

    @Test func audioDeviceEachSignalAloneIsEnough() {
        #expect(isAudioDevice(adv(serviceUuids: ["184E"])))          // LE Audio ASCS
        #expect(isAudioDevice(adv(serviceDataUuids: ["FE2C"])))      // Fast Pair
        #expect(isAudioDevice(adv(appleType: 0x07)))                 // AirPods proximity pairing
    }

    @Test func audioDeviceDongleAndOtherBleAreNotAudio() {
        #expect(!isAudioDevice(adv(serviceUuids: ["FFE0"])))         // dongle CarMD (UART)
        #expect(!isAudioDevice(adv(serviceUuids: ["180D"])))         // nRF Advertiser Heart Rate
        #expect(!isAudioDevice(adv(appleType: 0x12)))                // Find My / AirTag
        #expect(!isAudioDevice(nil))
    }

    @Test func shownTogglesOffShowsEverything() {
        #expect(isDeviceShown(device(nil), query: "", hideUnknown: false, hideAudio: false, isPinned: false))
        #expect(isDeviceShown(device("Buds", adv(appleType: 0x07)), query: "", hideUnknown: false, hideAudio: false, isPinned: false))
    }

    @Test func shownTogglesHideMatchingDevices() {
        #expect(!isDeviceShown(device(nil), query: "", hideUnknown: true, hideAudio: false, isPinned: false))
        #expect(!isDeviceShown(device("AirPods", adv(appleType: 0x07)), query: "", hideUnknown: false, hideAudio: true, isPinned: false))
        #expect(isDeviceShown(device("CMD-Test", adv(serviceUuids: ["180D"])), query: "", hideUnknown: true, hideAudio: true, isPinned: false))
    }

    @Test func shownNameQueryAndTogglesCombine() {
        let audioCmd = device("CMD-Buds", adv(serviceDataUuids: ["FE2C"]))
        #expect(isDeviceShown(audioCmd, query: "cmd", hideUnknown: false, hideAudio: false, isPinned: false))
        #expect(!isDeviceShown(audioCmd, query: "cmd", hideUnknown: false, hideAudio: true, isPinned: false))
        #expect(!isDeviceShown(device("Pixel 8"), query: "cmd", hideUnknown: false, hideAudio: false, isPinned: false))
    }

    @Test func shownPinnedDeviceIgnoresTogglesButNotNameQuery() {
        #expect(isDeviceShown(device(nil), query: "", hideUnknown: true, hideAudio: true, isPinned: true))
        #expect(isDeviceShown(device("AirPods", adv(appleType: 0x07)), query: "", hideUnknown: true, hideAudio: true, isPinned: true))
        #expect(!isDeviceShown(device("AirPods", adv(appleType: 0x07)), query: "cmd", hideUnknown: true, hideAudio: true, isPinned: true))
    }
}
