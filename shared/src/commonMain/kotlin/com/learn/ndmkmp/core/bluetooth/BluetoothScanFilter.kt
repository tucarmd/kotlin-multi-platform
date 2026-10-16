package com.learn.ndmkmp.core.bluetooth

/**
 * Day 2 (nRF-style) — filter devices during scan by NAME, like the CarMD Ionic app but the names are a PARAMETER.
 *
 * ```
 *  onScanResult ─► name = device.name ?: scanRecord.deviceName
 *                   └─ filter.matches(name)?
 *                        ├─ true  ─► found[address] (shown in the list)
 *                        └─ false ─► hiddenCount++   (UI: "N other devices hidden")
 * ```
 *
 * Ionic filters in 3 layers, same rule `== "CarMD2"` or `startsWith("CMD-")`, dropping unnamed devices:
 * - native Android `local_plugin/cordova-plugin-ble-central-master/src/android/BLECentralPlugin.java:1965-1970`
 * - native iOS `…/src/ios/BLECentralPlugin.m:663`
 * - JS `libs/ble-tool/src/lib/services/tool.service.ts:152-157` + `src/app/configs/app.config.ts:157`
 * There the names are **hard-coded in native** (risk #8, bluetooth 05) → renaming the product means editing + re-releasing
 * the plugin. Here the service takes the filter as a parameter, the UI decides on/off.
 *
 * Comparison is **case-insensitive** (Ionic JS `toUpperCase()`), unlike Ionic native which compares exactly.
 */
data class BluetoothScanFilter(
    /** Exact name match, e.g. `CarMD2`. */
    val names: List<String> = emptyList(),
    /** Prefix, e.g. `CMD-` (newer dongles: `CMD-` + 12 chars). */
    val prefixes: List<String> = emptyList(),
) {
    /** No rules = see everything (including unnamed devices) — debug mode, this app's default. */
    val isOff: Boolean get() = names.isEmpty() && prefixes.isEmpty()

    fun matches(name: String?): Boolean {
        if (isOff) return true
        // No name means it can't be a CarMD tool — Ionic drops it too (`name == null → return`)
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return false
        return names.any { it.equals(n, ignoreCase = true) } ||
            prefixes.any { n.startsWith(it, ignoreCase = true) }
    }

    /** `CarMD2 · CMD-*` — printed in the UI so the tester knows what is being filtered. */
    val label: String get() = (names + prefixes.map { "$it*" }).joinToString(" · ")

    companion object {
        /** Default = exactly the name set of the current Ionic app. */
        val CARMD_TOOLS = BluetoothScanFilter(names = listOf("CarMD2"), prefixes = listOf("CMD-"))
        val NONE = BluetoothScanFilter()
    }
}
