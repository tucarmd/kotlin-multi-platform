package dev.obdlink.obd.model

/**
 * [App 5 · B3] Dongle — one ELM327 adapter found by a scan. Plain data so the core and the UIs never touch a
 * BLE library type.
 *
 * PLATFORM: [id] is the MAC address on Android but a per-phone random UUID on iOS (Apple hides MAC addresses).
 * Never store a "known dongle" by MAC, and never compare ids across two phones.
 */
data class Dongle(
    val id: String,
    /** Advertised name, e.g. "OBDII" or "IOS-Vlink". Empty when the dongle advertises none. */
    val name: String,
    /** Signal strength in dBm when found: -40 is next to the phone, -90 is barely reachable. */
    val rssi: Int,
)

/** [App 5 · B3] State of the radio link as the transport sees it. ObdCore turns [Lost] into Reconnecting. */
enum class LinkStatus {
    /** Never connected, or we disconnected on purpose. */
    Disconnected,
    Connected,

    /** The link dropped without us asking (dongle unplugged, car started, out of range). */
    Lost,
}
