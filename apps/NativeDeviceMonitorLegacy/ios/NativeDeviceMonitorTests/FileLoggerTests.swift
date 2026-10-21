import Foundation
import Testing
@testable import NativeDeviceMonitor

/// Week 3 · Day 12–13 — same cases as `android/.../nativecore/logging/FileLoggerTest.kt`.
struct FileLoggerTests {

    private func sample(ts: String = "2026-10-05T09:30:00+07:00", source: String? = "loop") -> DeviceStatus {
        DeviceStatus(timestamp: ts, battery: 82, wifiEnabled: nil, wifiConnected: true, cellularAvailable: false,
                     bluetoothEnabled: true, bluetoothConnected: nil, source: source)
    }

    /// Each test gets its own temp directory → running in parallel never clobbers each other's files.
    private func logger() -> FileLogger {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        return FileLogger(url: dir.appendingPathComponent(FileLogger.fileName))
    }

    @Test func jsonHasPlanFieldNamesAndNullForUnmeasured() throws {
        let json = try sample().toJson()
        for key in ["timestamp", "battery", "wifiEnabled", "wifiConnected", "cellularAvailable", "bluetoothEnabled"] {
            #expect(json.contains("\"\(key)\""))
        }
        #expect(json.contains("\"bluetoothConnected\":null"))
        #expect(json.contains("\"wifiEnabled\":null"))
    }

    @Test func jsonRoundTrip() throws {
        let s = sample()
        #expect(try DeviceStatus.fromJson(s.toJson()) == s)
    }

    @Test func jsonWithoutSourceOmitsFieldAndParsesBack() throws {
        let s = sample(source: nil)
        #expect(!(try s.toJson()).contains("source"))
        #expect(try DeviceStatus.fromJson(s.toJson()) == s)
    }

    @Test func readMissingFileIsEmptyNotError() async {
        let r = await logger().readLogs()
        #expect(r.records.isEmpty)
        #expect(r.corruptLines == 0)
        #expect(r.error == nil)
    }

    @Test func append3Writes3LinesInOrder() async throws {
        let log = logger()
        for i in 0..<3 { #expect(await log.append(sample(ts: "2026-10-05T09:30:0\(i)+07:00"))) }
        let text = try String(contentsOf: log.url, encoding: .utf8)
        #expect(text.split(separator: "\n").count == 3)
        let r = await log.readLogs()
        #expect(r.records.map { String($0.timestamp.dropFirst(17).prefix(2)) } == ["00", "01", "02"])
    }

    @Test func readSkipsCorruptLinesAndCountsThem() async throws {
        let log = logger()
        await log.append(sample())
        let h = try FileHandle(forWritingTo: log.url)
        try h.seekToEnd()
        try h.write(contentsOf: Data("not json\n{\"battery\":1}\n\n".utf8))   // garbage · missing timestamp · empty line
        try h.close()
        await log.append(sample())
        let r = await log.readLogs()
        #expect(r.records.count == 2)
        #expect(r.corruptLines == 2)
    }

    @Test func clearEmptiesAndAppendStillWorks() async {
        let log = logger()
        await log.append(sample())
        #expect(await log.clearLogs())
        #expect(await log.readLogs().records.isEmpty)
        await log.append(sample())
        #expect(await log.readLogs().records.count == 1)
    }

    @Test func clearMissingFileIsOk() async {
        #expect(await logger().clearLogs())
    }
}
