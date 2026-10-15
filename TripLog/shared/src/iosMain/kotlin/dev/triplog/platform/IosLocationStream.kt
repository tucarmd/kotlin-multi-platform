package dev.triplog.platform

import dev.triplog.model.LocationProblem
import dev.triplog.model.LocationUnavailableException
import dev.triplog.model.TrackPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLActivityTypeFitness
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/**
 * [App 4 · B3] LocationStream on iOS: a Kotlin CLLocationManagerDelegate wrapped in `callbackFlow`.
 *
 * Why it keeps running with the screen off: `allowsBackgroundLocationUpdates = true` plus the `location`
 * background mode in Info.plist (iosApp/project.yml). iOS then shows the blue location pill instead of
 * suspending the app. When-In-Use permission is enough as long as recording starts while the app is open.
 *
 * Called by: TripRecorder (through the LocationStream interface). Calls: CLLocationManager.
 */
@OptIn(ExperimentalForeignApi::class)
class IosLocationStream : LocationStream {

    /** Fixes as fast as Core Location delivers them (about 1 per second while moving). */
    override fun points(): Flow<TrackPoint> = callbackFlow {
        val manager = CLLocationManager()
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = kCLDistanceFilterNone
        manager.activityType = CLActivityTypeFitness
        // PLATFORM: crashes with NSInternalInconsistencyException if Info.plist lacks UIBackgroundModes = location.
        manager.allowsBackgroundLocationUpdates = true
        // TRAP: the default (true) lets iOS silently stop updates when you stand still at a red light,
        // and they do not come back until the app is opened again.
        manager.pausesLocationUpdatesAutomatically = false
        manager.showsBackgroundLocationIndicator = true

        fun onAuthorization(m: CLLocationManager) {
            when (m.authorizationStatus) {
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways ->
                    m.startUpdatingLocation()
                kCLAuthorizationStatusNotDetermined -> m.requestWhenInUseAuthorization()
                else -> close(LocationUnavailableException(LocationProblem.PermissionDenied))
            }
        }

        // CLLocationManager holds its delegate weakly; awaitClose below keeps this object alive.
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                onAuthorization(manager)
            }

            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                // In the background iOS may deliver several fixes at once: send them all, in order.
                didUpdateLocations.forEach { (it as? CLLocation)?.let { location -> trySend(location.toPoint()) } }
            }

            @ObjCSignatureOverride
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                if (didFailWithError.code == kCLErrorDenied) {
                    close(LocationUnavailableException(LocationProblem.PermissionDenied))
                }
                // kCLErrorLocationUnknown and friends are transient: Core Location keeps trying.
            }
        }
        manager.delegate = delegate
        onAuthorization(manager)

        awaitClose {
            manager.stopUpdatingLocation()
            manager.allowsBackgroundLocationUpdates = false
            manager.delegate = null
            delegate.hashCode() // keep a strong reference until the flow closes
        }
    }
        // TRAP: CLLocationManager only calls its delegate on a thread with a run loop. The recorder collects on
        // Dispatchers.Default (no run loop), so the manager must be created and used on the main thread.
        .flowOn(Dispatchers.Main)

    private fun CLLocation.toPoint(): TrackPoint {
        val (lat, lon) = coordinate.useContents { latitude to longitude }
        return TrackPoint(
            latitude = lat,
            longitude = lon,
            // PLATFORM: negative accuracy means "invalid" on iOS; Android uses hasAccuracy() instead.
            accuracyMeters = if (horizontalAccuracy >= 0) horizontalAccuracy else Double.MAX_VALUE,
            speedMps = speed.takeIf { it >= 0 },
            timeMillis = (timestamp.timeIntervalSince1970 * 1000).toLong(),
        )
    }
}
