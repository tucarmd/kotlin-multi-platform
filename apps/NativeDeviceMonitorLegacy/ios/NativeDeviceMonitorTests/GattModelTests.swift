import Foundation
import Testing
@testable import NativeDeviceMonitor

/// Day 3 + Day 4 + task 004 (write/MTU) — same cases as `android/.../nativecore/bluetooth/GattModelTest.kt`.
struct GattModelTests {

    // MARK: - GattProperties.of: bitmask → flags

    @Test func propertiesReadNotify0x12() {
        let p = GattProperties.of(0x12)
        #expect(p.read)
        #expect(p.notify)
        #expect(!p.write)
        #expect(p.canSubscribe)
        #expect(p.labels == ["READ", "NOTIFY"])
    }

    @Test func propertiesUartWriteNotify0x1C() {
        // HM-10 dongle FFE1: WRITE_NO_RESPONSE + WRITE + NOTIFY, no READ
        let p = GattProperties.of(0x1C)
        #expect(!p.read)
        #expect(p.labels == ["WRITE", "WRITE NO RESP", "NOTIFY"])
    }

    @Test func propertiesIndicateOnlyCanSubscribe() {
        let p = GattProperties.of(0x20)
        #expect(!p.notify)
        #expect(p.canSubscribe)
        #expect(GattProperties.of(0x00).labels.isEmpty)
    }

    // MARK: - BluetoothDecoder.decode: bytes → value

    @Test func decodeBattery() {
        #expect(BluetoothDecoder.decode("2A19", Data([0x5A])) == "90 %")
    }

    @Test func decodeUtf8Strings() {
        #expect(BluetoothDecoder.decode("2A29", Data("Nordic".utf8)) == "Nordic")
        #expect(BluetoothDecoder.decode("2a00", Data("CMD-01".utf8) + [0]) == "CMD-01")   // lowercase + trailing 0 byte
    }

    @Test func decodeHeartRateUint8AndUint16() {
        #expect(BluetoothDecoder.decode("2A37", Data([0x00, 0x48])) == "72 bpm")
        #expect(BluetoothDecoder.decode("2A37", Data([0x01, 0x2C, 0x01])) == "300 bpm")
        #expect(BluetoothDecoder.decode("2A37", Data([0x01, 0x2C])) == "—")                // truncated
    }

    @Test func decodeUnknownAsciiText() {
        #expect(BluetoothDecoder.decode("FFE1", Data([0x41, 0x54, 0x0D])) == "\"AT\\r\"")
    }

    @Test func decodeUnknownLittleEndianNumbers() {
        #expect(BluetoothDecoder.decode("1234", Data([0xFF])) == "255 (uint8)")
        #expect(BluetoothDecoder.decode("1234", Data([0x34, 0x12])) == "4660 (uint16 LE)")
        #expect(BluetoothDecoder.decode("1234", Data([0xFF, 0xFF, 0xFF, 0xFF])) == "4294967295 (uint32 LE)")
        #expect(BluetoothDecoder.decode("1234", Data([0x01, 0x02, 0x03])) == "—")
        #expect(BluetoothDecoder.decode("2A19", Data()) == "(empty)")
    }

    // MARK: - Target characteristic

    @Test func readTargetPrefersBatteryThenFirstReadable() {
        let withBattery = [service("1800", chr("1800", "2A00", 0x02)), service("180F", chr("180F", "2A19", 0x12))]
        #expect(readTargetOf(withBattery)?.uuid == "2A19")

        let noKnown = [service("AAAA", chr("AAAA", "BBBB", 0x10), chr("AAAA", "CCCC", 0x02))]
        #expect(readTargetOf(noKnown)?.uuid == "CCCC")
        #expect(readTargetOf([service("AAAA", chr("AAAA", "BBBB", 0x10))]) == nil)
    }

    @Test func notifyTargetPrefersDongleUartThenFirstSubscribable() {
        let dongle = [service("180F", chr("180F", "2A19", 0x12)), service("FFE0", chr("FFE0", "FFE1", 0x1C))]
        #expect(notifyTargetOf(dongle)?.uuid == "FFE1")

        let indicateOnly = [service("AAAA", chr("AAAA", "BBBB", 0x02), chr("AAAA", "CCCC", 0x20))]
        #expect(notifyTargetOf(indicateOnly)?.uuid == "CCCC")
        #expect(notifyTargetOf([]) == nil)
    }

    // MARK: - Received Data cap

    @Test func appendCappedDropsOldest() {
        #expect(appendCapped([1, 2], 3, max: 3) == [1, 2, 3])
        #expect(appendCapped([1, 2, 3], 4, max: 3) == [2, 3, 4])
    }

    // MARK: - Task 004: Write + MTU

    @Test func fromHexAcceptsCommonSpellings() {
        let atz = Data([0x41, 0x54, 0x5A, 0x0D])
        #expect(BluetoothDecoder.fromHex("41 54 5A 0D") == atz)
        #expect(BluetoothDecoder.fromHex("41545a0d") == atz)
        #expect(BluetoothDecoder.fromHex("0x41,0x54,0x5A,0x0D") == atz)
        #expect(BluetoothDecoder.fromHex(" 41-54:5A 0d ") == atz)
        #expect(BluetoothDecoder.fromHex("FF") == Data([0xFF]))
    }

    @Test func fromHexRejectsInvalid() {
        #expect(BluetoothDecoder.fromHex("") == nil)
        #expect(BluetoothDecoder.fromHex("   ") == nil)
        #expect(BluetoothDecoder.fromHex("4") == nil)          // odd length
        #expect(BluetoothDecoder.fromHex("4G") == nil)         // not hex
        #expect(BluetoothDecoder.fromHex("41 5") == nil)
    }

    @Test func fromTextExpandsEscapes() {
        #expect(BluetoothDecoder.fromText("ATZ\\r") == Data([0x41, 0x54, 0x5A, 0x0D]))
        #expect(BluetoothDecoder.fromText("010C\\r\\n") == Data([0x30, 0x31, 0x30, 0x43, 0x0D, 0x0A]))
        #expect(BluetoothDecoder.fromText("\\\\\\t") == Data([0x5C, 0x09]))     // \\ → \   \t → tab
        #expect(BluetoothDecoder.fromText("A\\") == Data([0x41, 0x5C]))          // trailing \ kept as-is
        #expect(BluetoothDecoder.fromText("\\x") == Data([0x5C, 0x78]))          // unknown escape kept as-is
        #expect(BluetoothDecoder.fromText("") == nil)
    }

    @Test func writeTargetPrefersDongleUartThenNordicRxThenFirstWritable() {
        let dongle = [service("AAAA", chr("AAAA", "BBBB", 0x08)), service("FFE0", chr("FFE0", "FFE1", 0x1C))]
        #expect(writeTargetOf(dongle)?.uuid == "FFE1")

        let nus = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
        let nordic = [service(nus, chr(nus, "6E400003-B5A3-F393-E0A9-E50E24DCCA9E", 0x10),
                                   chr(nus, "6E400002-B5A3-F393-E0A9-E50E24DCCA9E", 0x0C))]
        #expect(writeTargetOf(nordic)?.uuid == "6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

        let noResponseOnly = [service("AAAA", chr("AAAA", "BBBB", 0x02), chr("AAAA", "CCCC", 0x04))]
        #expect(writeTargetOf(noResponseOnly)?.uuid == "CCCC")
        #expect(writeTargetOf([service("180F", chr("180F", "2A19", 0x12))]) == nil)
    }

    @Test func maxPayloadIsMtuMinusHeader() {
        #expect(maxPayloadOf(attDefaultMtu) == 20)
        #expect(maxPayloadOf(attRequestedMtu) == 244)
        #expect(maxPayloadOf(0) == 0)
    }

    @Test func planWriteOkPicksWriteType() {
        #expect(planWrite("ATZ\\r", asHex: false, properties: .of(0x0C), maxPayload: 20)
                == .ok(Data([0x41, 0x54, 0x5A, 0x0D]), withResponse: true))       // has WRITE → prefer ACK
        #expect(planWrite("01", asHex: true, properties: .of(0x04), maxPayload: 20)
                == .ok(Data([0x01]), withResponse: false))
    }

    @Test func planWriteInvalidReasons() {
        let writable = GattProperties.of(0x08)
        #expect(planWrite("01", asHex: true, properties: .of(0x12), maxPayload: 20) == .invalid("This characteristic is not writable"))
        #expect(planWrite("", asHex: false, properties: writable, maxPayload: 20) == .invalid("Type something to send"))
        #expect(planWrite("4G", asHex: true, properties: writable, maxPayload: 20) == .invalid("Invalid hex — use pairs like 41 54 5A 0D"))
        #expect(planWrite(String(repeating: "A", count: 21), asHex: false, properties: writable, maxPayload: 20)
                == .invalid("21 bytes > max 20 for MTU 23"))
        // exactly at the limit still sends
        #expect(planWrite(String(repeating: "A", count: 20), asHex: false, properties: writable, maxPayload: 20)
                == .ok(Data(repeating: 0x41, count: 20), withResponse: true))
    }

    private func chr(_ service: String, _ uuid: String, _ props: Int) -> GattCharacteristicInfo {
        GattCharacteristicInfo(serviceUuid: service, uuid: uuid, properties: .of(props))
    }

    private func service(_ uuid: String, _ c: GattCharacteristicInfo...) -> GattServiceInfo {
        GattServiceInfo(uuid: uuid, characteristics: c)
    }
}
