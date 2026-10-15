package dev.obdlink.obd.rules

import dev.obdlink.obd.model.ObdPid

/**
 * [App 5 · B1] PID formulas (SAE J1979, mode 01). Every "bytes → engineering value" conversion lives here,
 * so each formula is written once and tested with a known answer.
 *
 * Called by: ObdCore (live loop). Calls: nothing.
 */

/** One decoded live value. The unit is [ObdPid.unit]: rpm, km/h or °C. */
data class PidReading(val pid: ObdPid, val value: Int)

/**
 * Decodes one answer message `[0x41, pid, A, B…]`. Returns null if it is not the answer to [pid]
 * (wrong mode byte, other PID, too short), so a stray answer can never be shown on the wrong gauge.
 *
 * Examples:
 * - EngineRpm, `[0x41, 0x0C, 0x1A, 0xF8]` → 1726 rpm, because (256 × 0x1A + 0xF8) / 4 = (6656 + 248) / 4
 * - VehicleSpeed, `[0x41, 0x0D, 0x32]` → 50 km/h (A)
 * - CoolantTemp, `[0x41, 0x05, 0x7B]` → 83 °C (A − 40)
 */
fun decodePid(pid: ObdPid, bytes: List<Int>): PidReading? {
    if (bytes.size < 2 + pid.dataBytes) return null
    // 0x41 = 0x40 + mode 01: the ECU answers mode N with N + 0x40.
    if (bytes[0] != 0x41 || bytes[1] != pid.code) return null
    val a = bytes[2]
    val value = when (pid) {
        // TRAP: RPM is a 16-bit number in quarter-rpm steps. Using only A gives 26 instead of 1726.
        ObdPid.EngineRpm -> (256 * a + bytes[3]) / 4
        ObdPid.VehicleSpeed -> a
        // TRAP: offset -40 so that one unsigned byte covers -40..215 °C. A cold engine at 0x14 is -20 °C, not 20.
        ObdPid.CoolantTemp -> a - 40
    }
    return PidReading(pid, value)
}

/**
 * Same as [decodePid] for a whole answer: takes the first ECU message that answers [pid].
 * TRAP: named differently on purpose. `List<Int>` and `List<List<Int>>` erase to the same JVM type,
 * so two `decodePid` overloads do not compile ("Platform declaration clash").
 * Example: two ECUs, `[[0x41, 0x0D, 0x00], [0x41, 0x0D, 0x00]]` → 0 km/h.
 */
fun decodePidAnswer(pid: ObdPid, messages: List<List<Int>>): PidReading? =
    messages.firstNotNullOfOrNull { decodePid(pid, it) }
