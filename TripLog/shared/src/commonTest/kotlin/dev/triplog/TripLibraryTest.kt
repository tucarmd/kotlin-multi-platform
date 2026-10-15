package dev.triplog

import app.cash.turbine.test
import dev.triplog.core.TripLibrary
import dev.triplog.model.TripStats
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [App 4 · B7] TripLibrary: list updates, load and delete, against the in-memory FakeTripRepository. */
class TripLibraryTest {

    private val db = FakeTripRepository()

    private suspend fun saveTrip(): Long {
        val id = db.createTrip(0)
        db.appendPoints(id, listOf(fix(0, 0.0), fix(10, 100.0)))
        db.finishTrip(id, 10_000, 10_000, TripStats(100.0, 10_000, 10.0, 10.0), 2)
        return id
    }

    @Test
    fun `the list follows saves and deletes`() = runTest {
        val library = TripLibrary(db, backgroundScope)
        library.trips.test {
            assertEquals(emptyList(), awaitItem())
            val id = saveTrip()
            assertEquals(id, awaitItem().single().id)
            library.delete(id)
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `load returns the trip with its points and null when it is gone`() = runTest {
        val library = TripLibrary(db, backgroundScope)
        val id = saveTrip()
        val detail = library.load(id)!!
        assertEquals(2, detail.points.size)
        assertTrue("<trkpt" in library.gpx(detail))
        assertEquals("trip-$id.gpx", library.gpxFileName(detail))

        library.delete(id)
        runCurrent()
        assertNull(library.load(id))
    }
}
