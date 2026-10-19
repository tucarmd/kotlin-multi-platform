package com.learn.ndmkmp.ui.theme

import androidx.compose.ui.unit.dp

/**
 * LAYER 1: shared spacing scale + corner radii.
 *
 * Why: without this file screens scatter `16.dp`, `8.dp`, `12.dp` at whim — a design change
 * means hunting down every spot, and the 2 platforms easily drift a few pt apart unnoticed.
 *
 * `object` = singleton, initialised once. Closest to `export const AppSpacing = {...}` in TS.
 */
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    /** CarMD's `--app-page-padding: 1.5rem` — left/right margin of every page. */
    val lg = 24.dp
    val xl = 32.dp
}

/**
 * Corner radii. CarMD doesn't declare `--app-control-radius` as a number in `variables.scss`
 * (it's scattered across individual `.scss` files), so the 3 values below are MEASURED FROM THE DESIGN, not
 * copied from CSS — noted here so whoever checks against Figma later knows which parts are guesses.
 */
object AppRadius {
    /** White rounded card block. */
    val card = 16.dp
    /** Input field. */
    val field = 10.dp
    /** Fully rounded (pill) button — any number above half the height is round enough. */
    val pill = 28.dp
}
