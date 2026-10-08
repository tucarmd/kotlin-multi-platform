package com.learn.nativedevicemonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * wraps [MaterialTheme] with the CarMD palette.
 *
 * Why: if `MainActivity` calls a BARE `MaterialTheme { }` → every Material 3 `Button`,
 * `OutlinedTextField`, `TopAppBar` takes Google's default purple, not
 * CarMD blue. Stuffing colors into each component means remembering 20 places. Stuff them once into the colorScheme here
 * and every Material component gets the right color — that's how Material 3 wants you to use it.
 *
 * Closest web analogue: set variables in `:root` once instead of `style="color:#1a75cf"` on every tag.
 *
 * NO dark mode yet: the real CarMD app doesn't have one either, and the token table only has a light version.
 * Add it later with `darkColorScheme()` + `isSystemInDarkTheme()`, no component changes needed.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColor.primary,
            onPrimary = Color.White,
            secondary = AppColor.secondary,
            onSecondary = Color.White,
            error = AppColor.danger,
            onError = Color.White,
            // background = page background (very light blue), surface = card/top bar background (white)
            background = AppColor.contentBackground,
            onBackground = AppColor.textPrimary,
            surface = AppColor.surface,
            onSurface = AppColor.textPrimary,
            // Secondary text color Material uses for label/placeholder/supporting text
            onSurfaceVariant = AppColor.textSecondary,
            outline = AppColor.border,
            outlineVariant = AppColor.divider,
        ),
        content = content,
    )
}
