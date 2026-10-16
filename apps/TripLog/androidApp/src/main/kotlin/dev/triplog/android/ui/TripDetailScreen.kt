package dev.triplog.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.triplog.android.R
import dev.triplog.model.TripDetail
import dev.triplog.rules.formatDistance
import dev.triplog.rules.formatDuration
import dev.triplog.rules.formatSpeedKmh
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** [App 4 · B5] What the detail screen is showing: still loading, gone, or the trip. */
private sealed class DetailUi {
    data object Loading : DetailUi()
    data object Missing : DetailUi()
    data class Loaded(val detail: TripDetail, val speeds: List<Double>) : DetailUi()
}

/** [App 4 · B5] Entry composable of Trip detail: loads the trip once, wires GPX sharing. */
@Composable
fun TripDetailRoute(tripId: Long, onBack: () -> Unit, viewModel: TripsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ui by produceState<DetailUi>(DetailUi.Loading, tripId) {
        value = viewModel.load(tripId)?.let { DetailUi.Loaded(it, viewModel.speeds(it)) } ?: DetailUi.Missing
    }
    val chooserTitle = stringResource(R.string.share_gpx)
    TripDetailScreen(
        ui = ui,
        onBack = onBack,
        onShare = { detail ->
            scope.launch { shareGpx(context, viewModel.gpxFileName(detail), viewModel.gpx(detail), chooserTitle) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDetailScreen(ui: DetailUi, onBack: () -> Unit, onShare: (TripDetail) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((ui as? DetailUi.Loaded)?.let { formatDateTime(it.detail.trip.startedAtMillis) } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (ui is DetailUi.Loaded) {
                        IconButton(onClick = { onShare(ui.detail) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_gpx))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (ui) {
            DetailUi.Loading -> Centered(stringResource(R.string.loading), Modifier.padding(padding))
            DetailUi.Missing -> Centered(stringResource(R.string.trip_missing), Modifier.padding(padding))
            is DetailUi.Loaded -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val trip = ui.detail.trip
                RouteCanvas(ui.detail.points, emptyText = "", height = 300.dp)
                StatRow(
                    stringResource(R.string.distance) to formatDistance(trip.distanceMeters),
                    stringResource(R.string.duration) to formatDuration(trip.durationMillis),
                )
                StatRow(
                    stringResource(R.string.speed_avg) to stringResource(R.string.kmh, formatSpeedKmh(trip.averageSpeedMps)),
                    stringResource(R.string.speed_max) to stringResource(R.string.kmh, formatSpeedKmh(trip.maxSpeedMps)),
                )
                Text(stringResource(R.string.speed_chart), style = MaterialTheme.typography.titleSmall)
                SpeedChart(ui.speeds)
                Text(
                    stringResource(R.string.points_count, trip.pointCount.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Centered(text: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
