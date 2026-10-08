package com.learn.nativedevicemonitor.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * LAYER 1: the WHOLE APP's color palette.
 *
 * Source: copied verbatim from the CarMD Connect Ionic app — `src/theme/variables.scss`.
 * Change it here and you MUST change `UI/Theme/AppColor.swift` on iOS, otherwise the 2 apps drift apart.
 *
 * For TypeScript folks: this is the Kotlin version of `:root { --ion-color-primary: #1a75cf }`.
 * Unlike CSS, a wrong variable name is a BUILD ERROR, not a transparent color at runtime.
 */
object AppColor {

    // --- Brand ---
    /** `--ion-color-primary` — header blue, icons, primary buttons. */
    val primary = Color(0xFF1A75CF)
    /** `--ion-color-secondary` — lighter blue, secondary icons. */
    val secondary = Color(0xFF4B9CDF)

    // --- Backgrounds ---
    /** `--app-content-bg` — very light blue, background of every page (not white). */
    val contentBackground = Color(0xFFEFF5FF)
    /** Background of cards / drawer / top bar. */
    val surface = Color(0xFFFFFFFF)

    // --- Text ---
    /** `--ion-text-color` — primary text. */
    val textPrimary = Color(0xFF2F3134)
    /** `--app-color-light-1` (the SCSS comment literally says "Secondary Text"). */
    val textSecondary = Color(0xFFA2A7AB)

    // --- States ---
    /** `--ion-color-danger` — Logout, errors. */
    val danger = Color(0xFFEC5B5B)
    /** `--ion-color-success`. */
    val success = Color(0xFF38D298)
    /** `--app-color-warning-1`. */
    val warning = Color(0xFFF18641)

    // --- Borders / disabled ---
    /** `--ion-color-light` — disabled button background, light border. */
    val disabled = Color(0xFFDFE4E8)
    /** `--app-color-light-2` — grey input border. */
    val border = Color(0xFFCBCFD2)
    /** `--app-color-splitter-line` — divider line. */
    val divider = Color(0xFFD1DCED)

    // --- Raw data box (not in CarMD) ---
    /** Terminal-style "Received Data" background — from the Learning Plan §4 mockup, not the SCSS. */
    val terminalBackground = Color(0xFF111827)
    /** Green hex text on the terminal background. */
    val terminalText = Color(0xFF4ADE80)
}
