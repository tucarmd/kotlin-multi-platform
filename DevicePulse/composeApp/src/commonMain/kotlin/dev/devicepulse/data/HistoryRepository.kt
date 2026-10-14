package dev.devicepulse.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import dev.devicepulse.db.PulseDb
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.ConnectionType
import dev.devicepulse.model.valueOrNull
import dev.devicepulse.ui.DashboardState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

/** One saved reading. Fields are null when that sensor had no value at save time. */
data class Snapshot(
    val id: Long,
    val takenAt: Instant,
    val batteryPercent: Int?,
    val charging: ChargingState?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Double?,
    val connection: ConnectionType?,
    val online: Boolean?,
    val latencyMs: Long?,
)

interface HistoryRepository {
    fun observe(): Flow<List<Snapshot>>
    suspend fun save(state: DashboardState)
    suspend fun clear()
}

class SqlHistoryRepository(
    driver: SqlDriver,
    private val clock: Clock = Clock.System,
) : HistoryRepository {

    private val queries = PulseDb(driver).snapshotQueries

    override fun observe(): Flow<List<Snapshot>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { row ->
                Snapshot(
                    id = row.id,
                    takenAt = Instant.fromEpochMilliseconds(row.takenAtMillis),
                    batteryPercent = row.batteryPercent?.toInt(),
                    charging = row.charging?.let { name -> ChargingState.entries.firstOrNull { it.name == name } },
                    latitude = row.latitude,
                    longitude = row.longitude,
                    accuracyMeters = row.accuracyMeters,
                    connection = row.connection?.let { name -> ConnectionType.entries.firstOrNull { it.name == name } },
                    online = row.online?.let { it != 0L },
                    latencyMs = row.latencyMs,
                )
            }
        }

    override suspend fun save(state: DashboardState) {
        withContext(Dispatchers.Default) { insert(state) }
    }

    private fun insert(state: DashboardState) {
        val battery = state.battery.valueOrNull()
        val fix = state.location.valueOrNull()
        val network = state.network.valueOrNull()
        queries.insert(
            takenAtMillis = clock.now().toEpochMilliseconds(),
            batteryPercent = battery?.levelPercent?.toLong(),
            charging = battery?.charging?.name,
            latitude = fix?.latitude,
            longitude = fix?.longitude,
            accuracyMeters = fix?.accuracyMeters,
            connection = network?.type?.name,
            online = network?.isOnline?.let { if (it) 1L else 0L },
            latencyMs = state.latencyMs?.valueOrNull(),
        )
    }

    override suspend fun clear() {
        withContext(Dispatchers.Default) { queries.deleteAll() }
    }
}
