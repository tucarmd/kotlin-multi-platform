package dev.obdlink.obd.rules

import dev.obdlink.obd.model.Dtc
import dev.obdlink.obd.model.DtcSystem

/**
 * [App 5 · B1] Diagnostic trouble codes: mode 03 answer → list of [Dtc].
 *
 * Why its own file: the bit layout is the easiest thing in the app to get wrong, and a wrong letter
 * (P vs U) sends the driver to the wrong part of the car. DtcTest pins every branch.
 *
 * Called by: ObdCore.readCodes. Calls: DtcCatalog.
 */

/**
 * Two bytes → one code. Bit layout of the first byte `AA BB CCCC`:
 * AA = letter (00 P, 01 C, 10 B, 11 U) · BB = first digit 0–3 · CCCC = second digit; the second byte = last two digits.
 *
 * Examples: `(0x03, 0x01)` → P0301 · `(0x01, 0x71)` → P0171 · `(0x41, 0x23)` → C0123 · `(0xC1, 0x00)` → U0100.
 */
fun decodeDtc(first: Int, second: Int): Dtc {
    val system = DtcSystem.entries[(first shr 6) and 0b11]
    val firstDigit = (first shr 4) and 0b11
    val code = buildString {
        append(system.letter)
        append(firstDigit)
        append(hexDigit(first and 0x0F))
        append(hexDigit(second shr 4))
        append(hexDigit(second and 0x0F))
    }
    // SAE: P0 and P2 are generic; P1/P3 and x1/x2 of the other systems belong to the car maker.
    val generic = firstDigit == 0 || (system == DtcSystem.Powertrain && firstDigit == 2)
    return Dtc(code = code, system = system, isGeneric = generic, summary = DtcCatalog.describe(code))
}

/**
 * Whole mode-03 answer (one message per ECU, see ObdResult.Data) → distinct codes, in answer order.
 *
 * Examples:
 * - CAN, `[[0x43, 0x01, 0x03, 0x01]]` → [P0301]   (0x01 = count, then pairs)
 * - Older protocols, `[[0x43, 0x03, 0x01, 0x01, 0x71, 0x00, 0x00]]` → [P0301, P0171] (no count, padded with 00 00)
 * - No codes, `[[0x43, 0x00]]` → []
 *
 * TRAP: CAN puts a COUNT byte after 0x43, ISO 9141 / KWP do not. Both look like "43 01 03 01…". We tell them
 * apart by parity: CAN = 1 count byte + 2 per code = odd number of bytes after 0x43; old protocols always send
 * pairs = even. Reading the count byte as half a code gives P0103 instead of P0301.
 */
fun decodeDtcResponse(messages: List<List<Int>>): List<Dtc> =
    messages.flatMap { message ->
        if (message.firstOrNull() != 0x43) return@flatMap emptyList()
        val body = message.drop(1)
        val pairs = if (body.size % 2 == 1) {
            val count = body[0]
            body.drop(1).chunked(2).filter { it.size == 2 }.take(count)
        } else {
            body.chunked(2).filter { it.size == 2 }
        }
        pairs.filterNot { it[0] == 0 && it[1] == 0 } // 00 00 = padding, not "P0000"
            .map { decodeDtc(it[0], it[1]) }
    }.distinctBy { it.code }

/** Mode 04 succeeded when an ECU answered 0x44. Example: `[[0x44]]` → true. */
fun isClearConfirmed(messages: List<List<Int>>): Boolean = messages.any { it.firstOrNull() == 0x44 }

private fun hexDigit(value: Int): Char = "0123456789ABCDEF"[value and 0x0F]

/**
 * [App 5 · B1] Short English descriptions for common generic codes (SAE J2012). Deliberately small:
 * it is a learning app, not a repair database. Manufacturer codes return null.
 */
object DtcCatalog {
    private val known = mapOf(
        "P0100" to "Mass air flow circuit",
        "P0101" to "Mass air flow circuit range/performance",
        "P0113" to "Intake air temperature sensor high input",
        "P0117" to "Coolant temperature sensor low input",
        "P0118" to "Coolant temperature sensor high input",
        "P0128" to "Coolant thermostat below regulating temperature",
        "P0130" to "O2 sensor circuit (bank 1, sensor 1)",
        "P0133" to "O2 sensor slow response (bank 1, sensor 1)",
        "P0171" to "System too lean (bank 1)",
        "P0172" to "System too rich (bank 1)",
        "P0174" to "System too lean (bank 2)",
        "P0300" to "Random/multiple cylinder misfire",
        "P0301" to "Cylinder 1 misfire",
        "P0302" to "Cylinder 2 misfire",
        "P0303" to "Cylinder 3 misfire",
        "P0304" to "Cylinder 4 misfire",
        "P0305" to "Cylinder 5 misfire",
        "P0306" to "Cylinder 6 misfire",
        "P0325" to "Knock sensor 1 circuit",
        "P0335" to "Crankshaft position sensor circuit",
        "P0340" to "Camshaft position sensor circuit",
        "P0401" to "EGR flow insufficient",
        "P0420" to "Catalyst efficiency below threshold (bank 1)",
        "P0430" to "Catalyst efficiency below threshold (bank 2)",
        "P0440" to "Evaporative emission system",
        "P0442" to "Evaporative emission system small leak",
        "P0455" to "Evaporative emission system large leak",
        "P0500" to "Vehicle speed sensor",
        "P0505" to "Idle air control system",
        "P0562" to "System voltage low",
        "P0700" to "Transmission control system",
        "C0035" to "Left front wheel speed sensor circuit",
        "U0100" to "Lost communication with ECM/PCM",
    )

    /** "P0301" → "Cylinder 1 misfire"; unknown code → null. */
    fun describe(code: String): String? = known[code]
}
