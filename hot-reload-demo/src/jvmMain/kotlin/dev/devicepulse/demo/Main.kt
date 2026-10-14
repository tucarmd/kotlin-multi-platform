package dev.devicepulse.demo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DevicePulse – Hot Reload demo",
        state = rememberWindowState(width = 420.dp, height = 760.dp),
    ) {
        PulseScreen()
    }
}
