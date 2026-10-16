package dev.triplog.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.triplog.android.R
import dev.triplog.android.service.TrackingService
import dev.triplog.model.LiveTrip
import dev.triplog.model.RecordState
import dev.triplog.model.TrackPoint
import dev.triplog.rules.formatDistance
import dev.triplog.rules.formatDuration
import dev.triplog.rules.formatFixed
import dev.triplog.rules.formatSpeedKmh
import org.koin.compose.viewmodel.koinViewModel

/**
 * [App 4 · B5] Entry composable of the Record screen: ViewModel from Koin, permissions, and the service start.
 *
 * Why split Route / Screen: RecordScreen takes plain values and lambdas, so @Preview draws it without Koin.
 */
@Composable
fun RecordRoute(onOpenTrips: () -> Unit, viewModel: RecordViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissions = rememberTrackingPermissions()
    RecordScreen(
        state = state,
        permissionIssue = permissions.issue,
        // Start goes through the service (not the ViewModel), so the process survives the screen turning off.
        onStart = { permissions.requestThen { TrackingService.start(context) } },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onStop = viewModel::stop,
        onOpenSettings = permissions::openSettings,
        onOpenTrips = onOpenTrips,
    )
}

/** [App 4 · B5] The Record screen. Draws [state] and fires actions; computes nothing itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    state: RecordState,
    permissionIssue: PermissionIssue?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrips: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenTrips) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.trips))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val trip = when (state) {
                is RecordState.Recording -> state.trip
                is RecordState.Paused -> state.trip
                else -> null
            }
            RouteCanvas(trip?.points.orEmpty(), emptyText = stringResource(R.string.waiting_gps))
            permissionIssue?.let { PermissionBanner(it, onOpenSettings) }
            trip?.problem?.let { Banner(stringResource(it.label())) }
            if (trip == null) {
                if (state is RecordState.Idle) Text(stringResource(R.string.idle_hint), style = MaterialTheme.typography.bodyMedium)
            } else {
                LiveStats(trip, paused = state is RecordState.Paused)
            }
            Controls(state, onStart, onPause, onResume, onStop)
        }
    }
}

@Composable
private fun LiveStats(trip: LiveTrip, paused: Boolean) {
    Text(
        formatDuration(trip.elapsedMillis),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
    if (paused) Text(stringResource(R.string.paused), color = MaterialTheme.colorScheme.tertiary)
    StatRow(
        stringResource(R.string.distance) to formatDistance(trip.distanceMeters),
        stringResource(R.string.speed_now) to stringResource(R.string.kmh, trip.currentSpeedMps?.let(::formatSpeedKmh) ?: "–"),
    )
    StatRow(
        stringResource(R.string.speed_avg) to stringResource(R.string.kmh, formatSpeedKmh(trip.averageSpeedMps)),
        stringResource(R.string.accuracy) to (trip.lastAccuracyMeters?.takeIf { it < 10_000 }
            ?.let { stringResource(R.string.accuracy_value, formatFixed(it, 0)) } ?: "–"),
    )
    if (trip.droppedFixes > 0) {
        Text(
            stringResource(R.string.dropped_fixes, trip.droppedFixes.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One button row per state. `when` without `else` stops compiling if a RecordState case is added later. */
@Composable
private fun Controls(state: RecordState, onStart: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onStop: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        when (state) {
            RecordState.Idle -> Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.start)) }
            is RecordState.Recording -> {
                OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.pause)) }
                StopButton(onStop, Modifier.weight(1f))
            }
            is RecordState.Paused -> {
                Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.resume)) }
                StopButton(onStop, Modifier.weight(1f))
            }
            RecordState.Saving -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.saving))
            }
        }
    }
}

@Composable
private fun StopButton(onStop: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onStop,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) { Text(stringResource(R.string.stop)) }
}

@Composable
private fun PermissionBanner(issue: PermissionIssue, onOpenSettings: () -> Unit) {
    val text = when (issue) {
        PermissionIssue.LocationDenied -> R.string.permission_location_denied
        PermissionIssue.PreciseDenied -> R.string.permission_precise_denied
        PermissionIssue.NotificationsDenied -> R.string.permission_notifications_denied
    }
    Banner(stringResource(text)) {
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
    }
}

@Composable
private fun Banner(text: String, action: @Composable () -> Unit = {}) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onErrorContainer)
            action()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordScreenPreview() {
    val points = (0..20).map { TrackPoint(21.0285 + it * 0.0002, 105.8542 + (it % 5) * 0.0002, 5.0, 1.5, it * 5_000L) }
    val trip = LiveTrip(0, 754_000, 1_234.0, 1.5, 6.0, points, droppedFixes = 2, problem = null)
    MaterialTheme {
        RecordScreen(RecordState.Recording(trip), null, {}, {}, {}, {}, {}, {})
    }
}
