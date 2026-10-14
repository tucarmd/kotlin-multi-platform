package dev.devicepulse.sensor

import androidx.compose.runtime.Composable

interface PermissionRequester {
    /** Observable: reading it from a composable recomposes when the user answers the prompt. */
    val granted: Boolean

    /** True when [request] would show the system prompt; false when only the Settings app can change the answer. */
    val canPrompt: Boolean
    fun request()
}

@Composable
expect fun rememberLocationPermission(): PermissionRequester
