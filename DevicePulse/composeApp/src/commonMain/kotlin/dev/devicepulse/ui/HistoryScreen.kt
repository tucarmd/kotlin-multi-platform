package dev.devicepulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.devicepulse.data.Snapshot
import dev.devicepulse.resources.Res
import dev.devicepulse.resources.back
import dev.devicepulse.resources.clear_history
import dev.devicepulse.resources.history
import dev.devicepulse.resources.history_empty
import dev.devicepulse.resources.offline
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(snapshots: List<Snapshot>, onBack: () -> Unit, onClear: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🕘 " + stringResource(Res.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (snapshots.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text(stringResource(Res.string.clear_history)) }
                    }
                },
            )
        },
    ) { padding ->
        if (snapshots.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.history_empty),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(snapshots, key = { it.id }) { SnapshotRow(it) }
            }
        }
    }
}

@Composable
private fun SnapshotRow(s: Snapshot) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.takenAt.toClockText(), fontWeight = FontWeight.SemiBold)
            val battery = s.batteryPercent?.let { "${it}% · ${s.charging?.label() ?: ""}" } ?: "—"
            Text("🔋 $battery")
            val location = if (s.latitude != null && s.longitude != null) {
                "${s.latitude.fixed(5)}, ${s.longitude.fixed(5)} (± ${s.accuracyMeters?.fixed(0) ?: "?"} m)"
            } else "—"
            Text("📍 $location")
            val network = when {
                s.connection == null -> "—"
                s.online == false -> "${s.connection.label()} · ${stringResource(Res.string.offline)}"
                else -> s.connection.label()
            }
            Text("📶 $network" + (s.latencyMs?.let { " · $it ms" } ?: ""))
        }
    }
}
