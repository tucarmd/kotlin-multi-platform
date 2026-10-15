package dev.triplog.platform

import dev.triplog.model.TrackPoint
import kotlinx.coroutines.flow.Flow

/**
 * [App 4 · B3] The only thing the core needs from the phone's GPS: a cold stream of fixes.
 *
 * Why an interface: TripRecorder is tested with a FakeLocationStream that emits scripted points in virtual time.
 * Android implements it with LocationManager (AndroidLocationStream), iOS with CLLocationManager (IosLocationStream).
 *
 * Contract every implementation follows:
 * - Collecting starts the GPS; cancelling the collection MUST stop it (otherwise the battery drains for nothing).
 * - Every emitted point has `segment = 0`; the recorder sets the real segment.
 * - When the platform cannot deliver, the flow fails with LocationUnavailableException(problem).
 *
 * Called by: TripRecorder. Implemented by: AndroidLocationStream, IosLocationStream, FakeLocationStream (tests).
 */
interface LocationStream {
    /** Cold flow of raw fixes, unfiltered. Example: walking outdoors → about one point per second. */
    fun points(): Flow<TrackPoint>
}
