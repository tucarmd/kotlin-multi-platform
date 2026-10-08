import Foundation
import Testing
@testable import NativeDeviceMonitor

/// Same cases as `android/.../nativecore/bluetooth/BluetoothLookupTest.kt`.
struct BluetoothLookupTests {

    @Test func companyNameKnownAndUnknown() {
        #expect(BluetoothLookup.companyName(0x004C) == "Apple")
        #expect(BluetoothLookup.companyName(0x0059) == "Nordic Semiconductor")
        #expect(BluetoothLookup.companyName(0xFFFF) == nil)
    }

    @Test func serviceNameCaseInsensitive() {
        #expect(BluetoothLookup.serviceName("180F") == "Battery")
        #expect(BluetoothLookup.serviceName("ffe0") == "UART (CarMD dongle)")
        #expect(BluetoothLookup.serviceName("6E400001-B5A3-F393-E0A9-E50E24DCCA9E") == "Nordic UART")
        #expect(BluetoothLookup.serviceName("1234") == nil)
    }

    @Test func signalThresholds() {
        #expect(BleSignal.of(-40) == .strong)
        #expect(BleSignal.of(-60) == .strong)   // boundary: -60 is still strong
        #expect(BleSignal.of(-61) == .medium)
        #expect(BleSignal.of(-80) == .medium)
        #expect(BleSignal.of(-81) == .weak)
        #expect(BleSignal.of(-50).bars == 3)
    }

    @Test func staleAfterFiveSeconds() {
        let seen = Date(timeIntervalSince1970: 10)
        #expect(!BluetoothLookup.isStale(lastSeen: seen, now: seen.addingTimeInterval(5)))     // exactly 5 s: not faded yet
        #expect(BluetoothLookup.isStale(lastSeen: seen, now: seen.addingTimeInterval(5.001)))
    }
}
