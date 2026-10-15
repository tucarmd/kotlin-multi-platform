package dev.triplog.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.triplog.android.R
import dev.triplog.model.Trip
import dev.triplog.rules.formatDistance
import dev.triplog.rules.formatDuration
import org.koin.compose.viewmodel.koinViewModel

/** [App 4 · B5] Entry composable of the Trips list. */
@Composable
fun TripsRoute(onBack: () -> Unit, onOpenTrip: (Long) -> Unit, viewModel: TripsViewModel = koinViewModel()) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    TripsScreen(trips, onBack, onOpenTrip, viewModel::delete)
}

/** [App 4 · B5] Saved trips, newest first. Swipe a row to the left to delete it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(trips: List<Trip>, onBack: () -> Unit, onOpenTrip: (Long) -> Unit, onDelete: (Long) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trips)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_trips), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // TRAP: key = trip.id. Without a stable key the swipe state of a deleted row is reused by the next row,
            // which then shows up already swiped away.
            items(trips, key = { it.id }) { trip ->
                SwipeToDelete(onDelete = { onDelete(trip.id) }) {
                    TripRow(trip, onClick = { onOpenTrip(trip.id) })
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
    ) { content() }
}

@Composable
private fun TripRow(trip: Trip, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatDateTime(trip.startedAtMillis), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.points_count, trip.pointCount.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDistance(trip.distanceMeters), style = MaterialTheme.typography.titleMedium)
            Text(formatDuration(trip.durationMillis), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripsScreenPreview() {
    val trips = listOf(
        Trip(2, 1_760_500_000_000, 1_760_502_000_000, 5_430.0, 1_900_000, 4.2, 1_900),
        Trip(1, 1_760_400_000_000, 1_760_400_600_000, 812.0, 600_000, 1.8, 600),
    )
    MaterialTheme { TripsScreen(trips, {}, {}, {}) }
}
