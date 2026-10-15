package dev.triplog.core

import dev.triplog.data.GpxWriter
import dev.triplog.data.TripRepository
import dev.triplog.model.Trip
import dev.triplog.model.TripDetail
import dev.triplog.rules.speedSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [App 4 · B2] TripLibrary: the saved trips, for the Trips and Trip detail screens.
 *
 * Why separate from TripRecorder: the recorder is a state machine that must stay small and testable;
 * listing, loading and exporting old trips has nothing to do with it.
 *
 * Called by: TripsViewModel / TripDetailViewModel (Android), TripsModel (iOS).
 * Calls: TripRepository, GpxWriter, rules/speedSeries.
 */
class TripLibrary(
    private val repository: TripRepository,
    private val scope: CoroutineScope,
) {
    /** Finished trips, newest first. Updates by itself when the recorder saves or the user deletes. */
    val trips: StateFlow<List<Trip>> = repository.observeTrips()
        .catch { emit(emptyList()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * The trip with all its points, or null when it no longer exists.
     *
     * TRAP: a Kotlin exception thrown from a suspend function called by Swift crashes the app
     * (only CancellationException becomes a Swift error). So every failure is turned into null here.
     */
    suspend fun load(tripId: Long): TripDetail? = try {
        repository.loadTrip(tripId)?.let { TripDetail(it, repository.loadPoints(tripId)) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    /** Deletes in the background; [trips] emits the shorter list when done. */
    fun delete(tripId: Long) {
        scope.launch {
            try {
                repository.deleteTrip(tripId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The row stays in the list; nothing else to do.
            }
        }
    }

    /** GPX 1.1 text for sharing. Example: a 2-point trip → a file with one `<trkseg>` and two `<trkpt>`. */
    fun gpx(detail: TripDetail): String = GpxWriter.write(detail.trip, detail.points)

    /** Share file name, e.g. "trip-7.gpx". */
    fun gpxFileName(detail: TripDetail): String = GpxWriter.fileName(detail.trip)

    /** Up to 60 speeds in m/s for the detail chart. */
    fun speeds(detail: TripDetail): List<Double> = speedSeries(detail.points)
}
