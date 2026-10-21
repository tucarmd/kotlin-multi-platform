import Foundation

/// Day 2 + Day 3 — turn BLE bytes into human-readable text. Pure functions. Mirrors `BluetoothDecoder.kt`.
///
/// Day 2: hex of the advertising packet. Day 3: `decode` characteristic bytes → value.
/// Write (task 004): the opposite direction — text the user types → bytes, `fromHex` / `fromText`.
enum BluetoothDecoder {
    /// `Data([0x02, 0x15])` → `02 15`. Accepts `Data` or a slice (`data.dropFirst(2)`).
    static func toHex<D: DataProtocol>(_ data: D) -> String {
        data.map { String(format: "%02X", $0) }.joined(separator: " ")
    }

    // MARK: - Day 3: bytes → value

    /// Read / notified bytes → text for humans. Known UUID → decode per spec, unknown → a safe guess.
    ///
    /// ```
    ///  2A19 Battery Level      5A          → "90 %"       uint8
    ///  2A29 Manufacturer Name  4E 6F 72 …  → "Nordic"     UTF-8
    ///  2A37 Heart Rate         00 48       → "72 bpm"     flags bit0 = 0 → uint8 · 1 → uint16 LE
    ///  (unknown)               41 54 0D    → "\"AT\\r\""  all printable → treat as text (UART dongles answer in ASCII)
    ///  (unknown)               34 12       → "4660 (uint16 LE)"   1 / 2 / 4 bytes → little-endian number
    ///  (unknown)               01 02 03    → "—"                  give up, hex only
    /// ```
    /// **Little-endian**: BLE sends the LOW byte first — `34 12` is `0x1234` = 4660.
    static func decode(_ uuid: String, _ data: Data) -> String {
        let bytes = [UInt8](data)          // Data may be a slice (startIndex ≠ 0) → copy to an array for safe indexing
        guard !bytes.isEmpty else { return "(empty)" }
        switch uuid.uppercased() {
        case "2A19":
            return "\(bytes[0]) %"
        case _ where utf8Characteristics.contains(uuid.uppercased()):
            let text = String(decoding: bytes, as: UTF8.self)
            return String(text.reversed().drop(while: { $0 == "\u{0}" }).reversed())
        case "2A37":
            return heartRate(bytes)
        case "2A01":
            return String(format: "0x%04X", uintLE(bytes, 2))
        default:
            return guess(bytes)
        }
    }

    /// Device Name · Model · Serial · Firmware · Hardware · Software · Manufacturer — the spec says UTF-8 strings.
    private static let utf8Characteristics: Set<String> = ["2A00", "2A24", "2A25", "2A26", "2A27", "2A28", "2A29"]

    private static func heartRate(_ bytes: [UInt8]) -> String {
        let wide = bytes[0] & 0x01 != 0
        guard bytes.count >= (wide ? 3 : 2) else { return "—" }
        let bpm = wide ? uintLE(Array(bytes[1...2]), 2) : UInt64(bytes[1])
        return "\(bpm) bpm"
    }

    private static func guess(_ bytes: [UInt8]) -> String {
        let printable = bytes.allSatisfy { (0x20...0x7E).contains($0) || $0 == 0x0D || $0 == 0x0A || $0 == 0x09 }
        if printable {
            let text = String(decoding: bytes, as: UTF8.self)
                .replacingOccurrences(of: "\r", with: "\\r")
                .replacingOccurrences(of: "\n", with: "\\n")
                .replacingOccurrences(of: "\t", with: "\\t")
            return "\"\(text)\""
        }
        switch bytes.count {
        case 1: return "\(bytes[0]) (uint8)"
        case 2: return "\(uintLE(bytes, 2)) (uint16 LE)"
        case 4: return "\(uintLE(bytes, 4)) (uint32 LE)"
        default: return "—"
        }
    }

    // MARK: - Write: text → bytes

    /// Hex string the user typed → bytes. Accepts many styles, because people copy from nRF / Xcode console / docs:
    ///
    /// ```
    ///  "41 54 5A 0D"   "41545A0D"   "0x41,0x54,0x5A,0x0D"   "41-54-5a-0d"   → 41 54 5A 0D
    ///  ""  · "4"  (odd, 1 char)  · "4G" (not hex)                             → nil
    /// ```
    /// Strip `0x`, whitespace, `,` `-` `:` then cut into 2-char pairs. nil = invalid, the View shows red.
    static func fromHex(_ text: String) -> Data? {
        let clean = Array(text.replacingOccurrences(of: "0x", with: "", options: .caseInsensitive)
            .filter { !$0.isWhitespace && !",-:".contains($0) })
        guard !clean.isEmpty, clean.count % 2 == 0 else { return nil }
        var out = Data(capacity: clean.count / 2)
        for i in stride(from: 0, to: clean.count, by: 2) {
            guard let byte = UInt8(String(clean[i...i + 1]), radix: 16) else { return nil }
            out.append(byte)
        }
        return out
    }

    /// Typed text → UTF-8 bytes. The input field can't type Enter as `\r` → allow **escapes** like in code:
    /// `ATZ\r` (5 chars `A T Z \ r`) → `41 54 5A 0D`. Understands `\r` `\n` `\t` `\\`. Empty → nil.
    ///
    /// ELM327 dongles (CarMD) only run a command when they see `\r` (0x0D) — without it the dongle waits and never answers.
    static func fromText(_ text: String) -> Data? {
        guard !text.isEmpty else { return nil }
        let chars = Array(text)
        var out = ""
        var i = 0
        while i < chars.count {
            if chars[i] == "\\", i + 1 < chars.count {
                if let real = escapes[chars[i + 1]] {
                    out.append(real)
                    i += 2
                    continue
                }
            }
            out.append(chars[i])
            i += 1
        }
        return Data(out.utf8)
    }

    /// `\r` → CR · `\n` → LF · `\t` → tab · `\\` → `\`
    private static let escapes: [Character: Character] = ["r": "\r", "n": "\n", "t": "\t", "\\": "\\"]

    /// Combine the first `count` bytes as little-endian. UInt64 so uint32 doesn't overflow.
    private static func uintLE(_ bytes: [UInt8], _ count: Int) -> UInt64 {
        var v: UInt64 = 0
        for i in 0..<min(count, bytes.count) { v |= UInt64(bytes[i]) << (8 * UInt64(i)) }
        return v
    }
}
