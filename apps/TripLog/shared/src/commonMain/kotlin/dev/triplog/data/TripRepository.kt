package dev.triplog.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import dev.triplog.db.TripDb
import dev.triplog.model.TrackPoint
import dev.triplog.model.Trip
import dev.triplog.model.TripStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * [App 4 · B3] TripRepository: the only code that touches the trip and point tables.
 *
 * Why an interface: TripRecorderTest uses an in-memory FakeTripRepository (no SQLite, runs in milliseconds),
 * and swapping SQLDelight for something else later only changes [SqlTripRepository].
 *
 * Called by: TripRecorder (create / append / finish / discard), TripLibrary (list / load / delete).
 * Calls: TripQueries, PointQueries (generated from Trip.sq and Point.sq).
 */
interface TripRepository {
    /** Inserts an open trip row and returns its id. Example: createTrip(1_700_000_000_000) → 7. */
    suspend fun createTrip(startedAtMillis: Long): Long

    /** Appends a batch of points to [tripId] in ONE transaction. */
    suspend fun appendPoints(tripId: Long, points: List<TrackPoint>)

    /** Closes the trip with its final numbers, which makes it appear in [observeTrips]. */
    suspend fun finishTrip(tripId: Long, endedAtMillis: Long, durationMillis: Long, stats: TripStats, pointCount: Int)

    /** Removes the trip and all its points. Also used to drop a trip that recorded nothing. */
    suspend fun deleteTrip(tripId: Long)

    /** Finished trips, newest first. Emits again after every change. */
    fun observeTrips(): Flow<List<Trip>>

    /** All points of a trip in order, or an empty list when the trip does not exist. */
    suspend fun loadPoints(tripId: Long): List<TrackPoint>

    /** One finished trip, or null when it does not exist (deleted, or still recording). */
    suspend fun loadTrip(tripId: Long): Trip?
}

/** [App 4 · B3] SQLDelight implementation. The driver comes from platformModule (Android / native SQLite). */
class SqlTripRepository(driver: SqlDriver) : TripRepository {

    private val db = TripDb(driver)
    private val tripQueries = db.tripQueries
    private val pointQueries = db.pointQueries

    override suspend fun createTrip(startedAtMillis: Long): Long = io {
        // TRAP: insert and last_insert_rowid() in one transaction = one connection (see lastInsertId in Trip.sq).
        tripQueries.transactionWithResult {
            tripQueries.insertTrip(startedAtMillis)
            tripQueries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun appendPoints(tripId: Long, points: List<TrackPoint>): Unit = io {
        // TRAP: without the transaction every INSERT is its own commit + fsync. 10 points = 10 disk syncs;
        // inside one transaction it is a single sync. That is the whole point of writing in batches.
        pointQueries.transaction {
            points.forEach { p ->
                pointQueries.insertPoint(
                    tripId = tripId,
                    segment = p.segment.toLong(),
                    latitude = p.latitude,
                    longitude = p.longitude,
                    accuracyMeters = p.accuracyMeters,
                    speedMps = p.speedMps,
                    timeMillis = p.timeMillis,
                )
            }
        }
    }

    override suspend fun finishTrip(
        tripId: Long,
        endedAtMillis: Long,
        durationMillis: Long,
        stats: TripStats,
        pointCount: Int,
    ): Unit = io {
        tripQueries.finishTrip(
            endedAtMillis = endedAtMillis,
            distanceMeters = stats.distanceMeters,
            durationMillis = durationMillis,
            maxSpeedMps = stats.maxSpeedMps,
            pointCount = pointCount.toLong(),
            id = tripId,
        )
    }

    override suspend fun deleteTrip(tripId: Long): Unit = io {
        tripQueries.transaction {
            pointQueries.deleteByTrip(tripId)
            tripQueries.deleteTrip(tripId)
        }
    }

    override fun observeTrips(): Flow<List<Trip>> =
        tripQueries.selectFinished(::toTrip).asFlow().mapToList(Dispatchers.IO)

    override suspend fun loadPoints(tripId: Long): List<TrackPoint> = io {
        pointQueries.selectByTrip(tripId).executeAsList().map { row ->
            TrackPoint(
                latitude = row.latitude,
                longitude = row.longitude,
                accuracyMeters = row.accuracyMeters,
                speedMps = row.speedMps,
                timeMillis = row.timeMillis,
                segment = row.segment.toInt(),
            )
        }
    }

    override suspend fun loadTrip(tripId: Long): Trip? = io { tripQueries.selectById(tripId, ::toTrip).executeAsOneOrNull() }

    // TRAP: `WHERE endedAtMillis IS NOT NULL` makes SQLDelight generate its own row class (SelectFinished) with a
    // non-null column instead of the table's Trip class. Passing a mapper keeps one conversion for both queries.
    @Suppress("LongParameterList")
    private fun toTrip(
        id: Long,
        startedAtMillis: Long,
        endedAtMillis: Long?,
        distanceMeters: Double,
        durationMillis: Long,
        maxSpeedMps: Double,
        pointCount: Long,
    ) = Trip(
        id = id,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis ?: startedAtMillis,
        distanceMeters = distanceMeters,
        durationMillis = durationMillis,
        maxSpeedMps = maxSpeedMps,
        pointCount = pointCount.toInt(),
    )

    // TRAP: SQLite calls block the thread. Never run them on the main thread (Android throws on strict mode,
    // iOS stutters the map). Dispatchers.IO exists on both platforms since coroutines 1.7.
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
