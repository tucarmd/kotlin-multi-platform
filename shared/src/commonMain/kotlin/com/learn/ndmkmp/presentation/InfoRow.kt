package com.learn.ndmkmp.presentation

/**
 * One "label · value" line of a table (media info, device status). A named class instead of `Pair<String, String>`:
 * Swift sees `Pair` as `KotlinPair<NSString, NSString>` with optional `first` / `second` — this reads `row.label`.
 */
data class InfoRow(val label: String, val value: String)
