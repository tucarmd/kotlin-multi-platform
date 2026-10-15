package dev.obdlink.ble

/**
 * [App 5 · B3] Which GATT service / characteristics carry the ELM327 serial stream.
 *
 * Why a table: BLE ELM327 clones are not standardised. Each chip vendor exposes its "UART over BLE" under a different
 * 16-bit service UUID. The common public ones:
 * - `FFF0` service: `FFF1` notify (answers), `FFF2` write (commands)
 * - `FFE0` service: `FFE1` does both notify and write (HM-10 style modules)
 * - `18F0` service: `2AF0` notify, `2AF1` write
 *
 * The chooser below is pure (plain strings, no Kable types), so ElmProfilesTest runs without a phone.
 *
 * Used by: KableElmTransport (scan filter + picking characteristics after connect).
 */
object ElmProfiles {
    /** 16-bit UUID → full 128-bit string on the Bluetooth base UUID, lower-case: 0xFFF0 → "0000fff0-0000-1000-8000-00805f9b34fb". */
    fun full(short: Int): String = "0000${short.toString(16).padStart(4, '0')}-0000-1000-8000-00805f9b34fb"

    /** Known service UUIDs, most common first. Also used as the scan filter. */
    val serviceUuids: List<String> = listOf(full(0xFFF0), full(0xFFE0), full(0x18F0))

    /**
     * Name prefixes seen on generic ELM327 BLE dongles.
     *
     * TRAP: many clones do NOT put their service UUID in the advertisement, so a service-only filter finds nothing.
     * We also match by name. PLATFORM: on iOS, mixing name and service filters turns off CoreBluetooth's native
     * filtering (Kable filters in the flow instead), which is fine in the foreground.
     */
    val namePrefixes: List<String> = listOf("OBD", "Obd", "ELM", "V-LINK", "VLINK", "IOS-Vlink", "Android-Vlink", "Vgate", "VEEPEAK", "KONNWEI")

    /** GATT services as seen after connect, reduced to what the chooser needs. */
    data class Service(val uuid: String, val characteristics: List<Characteristic>)

    /** One characteristic: its UUID and whether it can be written to / notifies. */
    data class Characteristic(val uuid: String, val canWrite: Boolean, val canNotify: Boolean)

    /** The pair of characteristics to use. They can be the same UUID (FFE1). */
    data class Channel(val service: String, val write: String, val notify: String)

    /**
     * Picks the write + notify characteristics.
     *
     * Order: a known service first (FFF0, FFE0, 18F0); otherwise the first non-standard service that has both a
     * notify and a writable characteristic. Returns null if nothing fits (not an ELM327).
     *
     * Example: FFF0 with FFF1 (notify) and FFF2 (write) → Channel(fff0, write = fff2, notify = fff1).
     */
    fun pick(services: List<Service>): Channel? {
        val known = serviceUuids.mapNotNull { uuid -> services.firstOrNull { it.uuid.lowercase() == uuid } }
        val others = services.filter { it !in known && it.uuid.lowercase() !in standardServices }
        return (known + others).firstNotNullOfOrNull { channelIn(it) }
    }

    private fun channelIn(service: Service): Channel? {
        val notify = service.characteristics.firstOrNull { it.canNotify } ?: return null
        // Prefer a characteristic that does both (FFE1), else the first writable one.
        val write = service.characteristics.firstOrNull { it.canWrite && it.canNotify }
            ?: service.characteristics.firstOrNull { it.canWrite }
            ?: return null
        return Channel(service.uuid.lowercase(), write.uuid.lowercase(), notify.uuid.lowercase())
    }

    // Generic Access, Generic Attribute, Device Information, Battery: never the serial stream.
    private val standardServices = setOf(full(0x1800), full(0x1801), full(0x180A), full(0x180F))
}
