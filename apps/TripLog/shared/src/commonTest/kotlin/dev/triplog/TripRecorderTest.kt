package dev.triplog

import dev.triplog.core.TripRecorder
import dev.triplog.model.LocationProblem
import dev.triplog.model.RecordState
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [App 4 · B7] TripRecorder driven by a FakeLocationStream in virtual time.
 *
 * The recorder runs in `backgroundScope` (it never finishes on its own, like the real app scope) and reads the
 * test scheduler's clock, so `advanceTimeBy(60_000)` is one minute for the recorder and takes no real time.
 */
class TripRecorderTest {

    private val gps = FakeLocationStream()
    private val db = FakeTripRepository()

    private fun TestScope.recorder() = TripRecorder(
        location = gps,
        repository = db,
        scope = backgroundScope,
        clock = { testScheduler.currentTime },
    )

    /** Sends a fix and lets the recorder process it. */
    private fun TestScope.walk(point: dev.triplog.model.TrackPoint) {
        gps.send(point)
        runCurrent()
    }

    private val RecordState.live get() = when (this) {
        is RecordState.Recording -> trip
        is RecordState.Paused -> trip
        else -> error("not recording: $this")
    }

    @Test
    fun `start turns the GPS on and moves to Recording`() = runTest {
        val recorder = recorder()
        recorder.start()
        runCurrent()
        assertIs<RecordState.Recording>(recorder.state.value)
        assertEquals(1, gps.collectors)
        assertEquals(1, db.open.size)
    }

    @Test
    fun `accepted fixes add up the distance`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        walk(fix(0, 0.0))
        walk(fix(10, 50.0))
        walk(fix(20, 100.0))
        val live = recorder.state.value.live
        assertEquals(100.0, live.distanceMeters, 0.5)
        assertEquals(3, live.points.size)
        assertEquals(5.0, live.currentSpeedMps!!, 0.05)
    }

    @Test
    fun `a 500 m jump in 1 s is dropped`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        walk(fix(0, 0.0))
        walk(fix(1, 500.0)) // noisy jump
        walk(fix(2, 2.0))
        val live = recorder.state.value.live
        assertEquals(1, live.droppedFixes)
        assertEquals(2, live.points.size)
        assertEquals(2.0, live.distanceMeters, 0.1)
    }

    @Test
    fun `an inaccurate fix is dropped but its accuracy is shown`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        walk(fix(0, accuracy = 80.0))
        val live = recorder.state.value.live
        assertEquals(0, live.points.size)
        assertEquals(80.0, live.lastAccuracyMeters)
    }

    @Test
    fun `pause does not count time`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        walk(fix(0, 0.0))
        advanceTimeBy(10_000); runCurrent()
        walk(fix(10, 30.0))
        recorder.pause(); runCurrent()
        assertEquals(10_000, recorder.state.value.live.elapsedMillis)

        advanceTimeBy(60_000); runCurrent() // one paused minute
        assertEquals(10_000, recorder.state.value.live.elapsedMillis)

        recorder.resume(); runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(15_000, recorder.state.value.live.elapsedMillis)

        recorder.stop(); runCurrent()
        assertEquals(RecordState.Idle, recorder.state.value)
        assertEquals(15_000, db.finished.value.single().durationMillis)
    }

    @Test
    fun `pause turns the GPS off and resume turns it back on`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        recorder.pause(); runCurrent()
        assertEquals(0, gps.collectors)
        recorder.resume(); runCurrent()
        assertEquals(1, gps.collectors)
        assertEquals(2, gps.subscriptions)
    }

    @Test
    fun `distance moved while paused is not counted`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        walk(fix(0, 0.0))
        walk(fix(10, 100.0))
        recorder.pause(); runCurrent()
        recorder.resume(); runCurrent()
        walk(fix(600, 3_000.0)) // took the bus while paused
        walk(fix(610, 3_100.0))
        val live = recorder.state.value.live
        assertEquals(200.0, live.distanceMeters, 0.5)
        assertEquals(listOf(0, 0, 1, 1), live.points.map { it.segment })
    }

    @Test
    fun `points are written in batches of 10`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        for (second in 0L until 25L) walk(fix(second, second * 2.0))
        assertEquals(listOf(10, 10), db.batches)

        recorder.stop(); runCurrent()
        assertEquals(listOf(10, 10, 5), db.batches)
        val trip = db.finished.value.single()
        assertEquals(25, trip.pointCount)
        assertEquals(48.0, trip.distanceMeters, 0.5)
    }

    @Test
    fun `a trip with fewer than 2 points is discarded`() = runTest {
        val recorder = recorder().apply { start() }
        runCurrent()
        walk(fix(0))
        recorder.stop(); runCurrent()
        assertEquals(RecordState.Idle, recorder.state.value)
        assertTrue(db.finished.value.isEmpty())
        assertTrue(db.open.isEmpty())
        assertEquals(0, gps.collectors)
    }

    @Test
    fun `denied permission shows a problem instead of crashing`() = runTest {
        gps.failWith = LocationProblem.PermissionDenied
        val recorder = recorder().apply { start() }
        runCurrent()
        assertEquals(LocationProblem.PermissionDenied, recorder.state.value.live.problem)

        gps.failWith = null
        recorder.pause(); runCurrent()
        recorder.resume(); runCurrent()
        assertNull(recorder.state.value.live.problem)
    }

    @Test
    fun `buttons that make no sense in the current state are ignored`() = runTest {
        val recorder = recorder()
        recorder.pause(); recorder.resume(); recorder.stop()
        runCurrent()
        assertEquals(RecordState.Idle, recorder.state.value)
        assertEquals(0, gps.subscriptions)

        recorder.start(); recorder.start()
        runCurrent()
        assertEquals(1, db.open.size)
    }
}
