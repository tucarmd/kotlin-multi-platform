package dev.devicepulse.android.ui

import androidx.compose.ui.res.stringResource
import dev.devicepulse.android.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(snapshots: List<Snapshot>, onBack: () -> Unit, onClear: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🕘 " + stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (snapshots.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text(stringResource(R.string.clear_history)) }
                    }
                },
            )
        },
    ) { padding ->
        if (snapshots.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.history_empty),
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
            val lat = s.latitude
            val lon = s.longitude
            val location = if (lat != null && lon != null) {
                "${lat.fixed(5)}, ${lon.fixed(5)} (± ${s.accuracyMeters?.fixed(0) ?: "?"} m)"
            } else "—"
            Text("📍 $location")
            val connection = s.connection
            val network = when {
                connection == null -> "—"
                s.online == false -> "${connection.label()} · ${stringResource(R.string.offline)}"
                else -> connection.label()
            }
            Text("📶 $network" + (s.latencyMs?.let { " · $it ms" } ?: ""))
        }
    }
}
