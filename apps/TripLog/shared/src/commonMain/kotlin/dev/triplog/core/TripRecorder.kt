package dev.triplog.core

import dev.triplog.model.FixVerdict
import dev.triplog.model.LiveTrip
import dev.triplog.model.LocationProblem
import dev.triplog.model.LocationUnavailableException
import dev.triplog.model.RecordState
import dev.triplog.model.TrackPoint
import dev.triplog.platform.LocationStream
import dev.triplog.data.TripRepository
import dev.triplog.rules.distanceMeters
import dev.triplog.rules.judgeFix
import dev.triplog.rules.tripStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * [App 4 · B2] TripRecorder: the one state machine that records a trip, shared by Android and iOS.
 *
 * Idle → Recording ⇄ Paused → Saving → Idle (diagram on RecordState).
 *
 * Why it lives outside any screen: recording must survive the screen. On Android the Record screen can be rotated or
 * closed while TrackingService keeps the process alive; on iOS the app goes to the background with the screen off.
 * So the recorder is a Koin `single` and runs in an app-wide [scope] passed in from outside (tests pass the
 * test scope and control time).
 *
 * Why an actor (one coroutine reading a command channel) instead of locks: start/pause/stop come from the main
 * thread, fixes from the GPS, ticks from a timer. Funnelling them all through [commands] means only one coroutine
 * ever touches the session fields below, on any platform, with no Mutex and no atomics.
 *
 * Called by: TrackingService + RecordViewModel (Android), TrackingModel (iOS).
 * Calls: LocationStream.points(), rules (judgeFix, distanceMeters, tripStats), TripRepository.
 */
class TripRecorder(
    private val location: LocationStream,
    private val repository: TripRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val batchSize: Int = BATCH_SIZE,
    private val tickMillis: Long = 1_000,
) {
    private val _state = MutableStateFlow<RecordState>(RecordState.Idle)

    /** What the Record screen and the notification draw. Android: collectAsStateWithLifecycle(). Swift: `for await`. */
    val state: StateFlow<RecordState> = _state.asStateFlow()

    private sealed class Command {
        data object Start : Command()
        data object Pause : Command()
        data object Resume : Command()
        data object Stop : Command()
        data object Tick : Command()
        data class Fix(val point: TrackPoint, val segment: Int) : Command()
        data class Failed(val problem: LocationProblem, val segment: Int) : Command()
    }

    // TRAP: UNLIMITED so trySend from a GPS callback or a button never fails or suspends.
    private val commands = Channel<Command>(Channel.UNLIMITED)

    // ---- Session fields. Only the actor coroutine reads or writes them. ----
    private var tripId: Long? = null
    private var startedAt = 0L
    private var segment = 0
    private var segmentStartedAt = 0L
    private var activeMillisBefore = 0L // recording time of the segments already closed by a pause
    private var points = mutableListOf<TrackPoint>()
    private val pending = mutableListOf<TrackPoint>() // accepted but not yet written to the database
    private var distance = 0.0
    private var currentSpeed: Double? = null
    private var lastAccuracy: Double? = null
    private var dropped = 0
    private var problem: LocationProblem? = null
    private var gpsJob: Job? = null
    private var tickJob: Job? = null

    init {
        scope.launch { for (command in commands) handle(command) }
    }

    /** Idle → Recording: opens a trip row and starts the GPS. Ignored in any other state. */
    fun start() { commands.trySend(Command.Start) }

    /** Recording → Paused: stops the GPS (saves battery) and freezes the clock. */
    fun pause() { commands.trySend(Command.Pause) }

    /** Paused → Recording: starts a new segment, so the distance moved while paused is never counted. */
    fun resume() { commands.trySend(Command.Resume) }

    /** Recording or Paused → Saving → Idle: flushes the last points and closes the trip. */
    fun stop() { commands.trySend(Command.Stop) }

    private suspend fun handle(command: Command) {
        val current = _state.value
        when (command) {
            Command.Start -> if (current is RecordState.Idle) begin()
            Command.Pause -> if (current is RecordState.Recording) {
                stopJobs()
                activeMillisBefore += clock() - segmentStartedAt
                publish(paused = true)
            }
            Command.Resume -> if (current is RecordState.Paused) {
                segment += 1
                segmentStartedAt = clock()
                problem = null
                startJobs()
                publish(paused = false)
            }
            Command.Stop -> if (current is RecordState.Recording || current is RecordState.Paused) finish(current)
            Command.Tick -> if (current is RecordState.Recording) publish(paused = false)
            // TRAP: a fix can still sit in the channel after pause/stop cancelled the GPS job.
            // The segment tag tells us it belongs to an older segment, so it is ignored.
            is Command.Fix -> if (current is RecordState.Recording && command.segment == segment) onFix(command.point)
            is Command.Failed -> if (current is RecordState.Recording && command.segment == segment) {
                problem = command.problem
                publish(paused = false)
            }
        }
    }

    private suspend fun begin() {
        val now = clock()
        val id = try {
            repository.createTrip(now)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return // database unusable: stay Idle rather than record into nowhere
        }
        tripId = id
        startedAt = now
        segment = 0
        segmentStartedAt = now
        activeMillisBefore = 0
        points = mutableListOf()
        pending.clear()
        distance = 0.0
        currentSpeed = null
        lastAccuracy = null
        dropped = 0
        problem = null
        startJobs()
        publish(paused = false)
    }

    private suspend fun onFix(raw: TrackPoint) {
        val point = raw.copy(segment = segment)
        lastAccuracy = point.accuracyMeters
        val prev = points.lastOrNull()
        // TEST: TripRecorderTest `a 500 m jump in 1 s is dropped`
        if (judgeFix(prev, point) != FixVerdict.Accepted) {
            dropped += 1
            publish(paused = false)
            return
        }
        val sameSegment = prev != null && prev.segment == point.segment
        val step = if (sameSegment) distanceMeters(prev, point) else 0.0
        distance += step
        currentSpeed = point.speedMps ?: if (sameSegment) {
            val dt = (point.timeMillis - prev.timeMillis) / 1000.0
            if (dt > 0) step / dt else null
        } else {
            null
        }
        points.add(point)
        pending.add(point)
        // TRAP: writing every fix on its own = one disk sync per second for hours. Batch them.
        // TEST: TripRecorderTest `points are written in batches of 10`
        if (pending.size >= batchSize) flush()
        problem = null
        publish(paused = false)
    }

    private suspend fun finish(current: RecordState) {
        val activeMillis = if (current is RecordState.Recording) {
            activeMillisBefore + (clock() - segmentStartedAt)
        } else {
            activeMillisBefore
        }
        _state.value = RecordState.Saving
        stopJobs()
        val id = tripId
        if (id != null) {
            safely {
                if (points.size < MIN_POINTS_TO_SAVE) {
                    repository.deleteTrip(id) // nothing worth keeping: GPS never got a usable fix
                } else {
                    flush()
                    repository.finishTrip(id, clock(), activeMillis, tripStats(points), points.size)
                }
            }
        }
        tripId = null
        points = mutableListOf()
        _state.value = RecordState.Idle
    }

    private suspend fun flush() {
        val id = tripId ?: return
        if (pending.isEmpty()) return
        val batch = pending.toList()
        pending.clear()
        safely { repository.appendPoints(id, batch) }
    }

    private fun startJobs() {
        val tag = segment
        gpsJob = scope.launch {
            location.points()
                .catch { e ->
                    val reason = (e as? LocationUnavailableException)?.problem ?: LocationProblem.LocationOff
                    commands.send(Command.Failed(reason, tag))
                }
                .collect { commands.send(Command.Fix(it, tag)) }
        }
        tickJob = scope.launch {
            while (true) {
                delay(tickMillis)
                commands.send(Command.Tick)
            }
        }
    }

    private fun stopJobs() {
        // Cancelling the collection runs the stream's awaitClose { } → the platform GPS listener is removed.
        gpsJob?.cancel()
        tickJob?.cancel()
        gpsJob = null
        tickJob = null
    }

    private fun publish(paused: Boolean) {
        val elapsed = if (paused) activeMillisBefore else activeMillisBefore + (clock() - segmentStartedAt)
        val live = LiveTrip(
            startedAtMillis = startedAt,
            elapsedMillis = elapsed,
            distanceMeters = distance,
            currentSpeedMps = currentSpeed,
            lastAccuracyMeters = lastAccuracy,
            points = points.toList(),
            droppedFixes = dropped,
            problem = problem,
        )
        _state.value = if (paused) RecordState.Paused(live) else RecordState.Recording(live)
    }

    /** Database errors must not kill the actor (it would stop answering every button). */
    private suspend fun safely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The trip may lose its last batch; the recorder itself keeps working.
        }
    }

    companion object {
        /** Points per database write. */
        const val BATCH_SIZE = 10

        /** A trip with fewer accepted points has no route to draw, so it is not kept. */
        const val MIN_POINTS_TO_SAVE = 2
    }
}
