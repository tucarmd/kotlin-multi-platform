package dev.obdlink.obd.model

/**
 * [App 5 · B1] Dtc — one diagnostic trouble code such as "P0301", decoded from two bytes by
 * [dev.obdlink.obd.rules.decodeDtc].
 *
 * [summary] comes from a small table of generic SAE J2012 codes ([dev.obdlink.obd.rules.DtcCatalog]); it is
 * technical reference data like the code itself, so it stays in English on both platforms. `null` = not in the table
 * (manufacturer codes such as P1xxx always are).
 */
data class Dtc(
    val code: String,
    val system: DtcSystem,
    /** true for SAE-defined codes (P0xxx, P2xxx, C0xxx, B0xxx, U0xxx); false for manufacturer codes. */
    val isGeneric: Boolean,
    // TRAP: not called `description`. Every Objective-C object already has `description` (its toString), so
    // Kotlin/Native would rename ours to `description_` in Swift.
    val summary: String?,
)

/** [App 5 · B1] The first letter of a DTC, chosen by the two highest bits of the first byte. */
enum class DtcSystem(val letter: Char) {
    Powertrain('P'),
    Chassis('C'),
    Body('B'),
    Network('U'),
}
