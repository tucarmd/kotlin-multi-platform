package com.learn.ndmkmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nativedevicemonitorkmp.shared.generated.resources.Res
import nativedevicemonitorkmp.shared.generated.resources.app_title
import nativedevicemonitorkmp.shared.generated.resources.p0_hint
import org.jetbrains.compose.resources.stringResource

// P0 hello screen: one composable in commonMain, drawn by both Android (MainActivity) and iOS (MainViewController).
// The 5 tabs replace this in P2.
@Composable
fun App() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.app_title), style = MaterialTheme.typography.headlineMedium)
                Text(platformName(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.p0_hint), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
