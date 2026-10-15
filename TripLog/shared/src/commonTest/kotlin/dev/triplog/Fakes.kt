package dev.triplog

import dev.triplog.data.TripRepository
import dev.triplog.model.LocationProblem
import dev.triplog.model.LocationUnavailableException
import dev.triplog.model.TrackPoint
import dev.triplog.model.Trip
import dev.triplog.model.TripStats
import dev.triplog.platform.LocationStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * [App 4 · B7] Test doubles for the two things TripRecorder talks to.
 *
 * FakeLocationStream: the test pushes scripted fixes with [send]; [collectors] shows whether the "GPS" is on.
 * FakeTripRepository: keeps trips and points in memory and records every batch written.
 */
class FakeLocationStream : LocationStream {
    private val fixes = MutableSharedFlow<TrackPoint>(extraBufferCapacity = 100)

    /** How many collectors are active right now. 0 = GPS off. */
    var collectors = 0
        private set

    /** Total number of times points() was collected. */
    var subscriptions = 0
        private set

    /** When set, the next collection fails at once with this problem. */
    var failWith: LocationProblem? = null

    override fun points(): Flow<TrackPoint> = flow {
        subscriptions++
        failWith?.let { throw LocationUnavailableException(it) }
        collectors++
        try {
            emitAll(fixes)
        } finally {
            collectors--
        }
    }

    /** Delivers one fix to whoever is collecting. Call runCurrent() afterwards to let the recorder see it. */
    fun send(point: TrackPoint) {
        check(fixes.tryEmit(point)) { "buffer full" }
    }
}

class FakeTripRepository : TripRepository {
    private var nextId = 1L
    val open = mutableMapOf<Long, Long>() // id → startedAt
    val points = mutableMapOf<Long, MutableList<TrackPoint>>()
    val batches = mutableListOf<Int>() // size of each appendPoints call, in order
    val finished = MutableStateFlow<List<Trip>>(emptyList())

    override suspend fun createTrip(startedAtMillis: Long): Long {
        val id = nextId++
        open[id] = startedAtMillis
        points[id] = mutableListOf()
        return id
    }

    override suspend fun appendPoints(tripId: Long, points: List<TrackPoint>) {
        batches += points.size
        this.points.getValue(tripId).addAll(points)
    }

    override suspend fun finishTrip(tripId: Long, endedAtMillis: Long, durationMillis: Long, stats: TripStats, pointCount: Int) {
        val trip = Trip(
            id = tripId,
            startedAtMillis = open.remove(tripId) ?: error("trip $tripId is not open"),
            endedAtMillis = endedAtMillis,
            distanceMeters = stats.distanceMeters,
            durationMillis = durationMillis,
            maxSpeedMps = stats.maxSpeedMps,
            pointCount = pointCount,
        )
        finished.value = listOf(trip) + finished.value
    }

    override suspend fun deleteTrip(tripId: Long) {
        open.remove(tripId)
        points.remove(tripId)
        finished.value = finished.value.filterNot { it.id == tripId }
    }

    override fun observeTrips(): Flow<List<Trip>> = finished

    override suspend fun loadPoints(tripId: Long): List<TrackPoint> = points[tripId].orEmpty()

    override suspend fun loadTrip(tripId: Long): Trip? = finished.value.firstOrNull { it.id == tripId }
}

/** A fix [northMeters] north of 10°N 106°E, 5 m accurate, at [seconds]. 1° of latitude ≈ 111_195 m. */
fun fix(seconds: Long, northMeters: Double = 0.0, accuracy: Double = 5.0, speed: Double? = null) = TrackPoint(
    latitude = 10.0 + northMeters / 111_195.08,
    longitude = 106.0,
    accuracyMeters = accuracy,
    speedMps = speed,
    timeMillis = seconds * 1000,
)
