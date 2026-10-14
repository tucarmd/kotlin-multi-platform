package dev.devicepulse.sensor

import dev.devicepulse.model.LocationFix
import dev.devicepulse.model.Reading
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.time.Instant

@OptIn(ExperimentalForeignApi::class)
class IosLocationSensor : LocationSensor {

    override fun observe(): Flow<Reading<LocationFix>> = callbackFlow {
        val manager = CLLocationManager()
        manager.desiredAccuracy = kCLLocationAccuracyBest

        fun onAuthorization(m: CLLocationManager) {
            when (m.authorizationStatus) {
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> {
                    m.startUpdatingLocation()
                }
                // Not determined yet counts as "no permission": the UI shows the grant button,
                // and the prompt answer arrives here through locationManagerDidChangeAuthorization.
                kCLAuthorizationStatusNotDetermined -> trySend(Reading.PermissionDenied)
                else -> {
                    m.stopUpdatingLocation()
                    trySend(Reading.PermissionDenied)
                }
            }
        }

        // CLLocationManager holds its delegate weakly; awaitClose below keeps this object alive.
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                onAuthorization(manager)
            }

            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                (didUpdateLocations.lastOrNull() as? CLLocation)?.let { trySend(Reading.Available(it.toFix())) }
            }

            @ObjCSignatureOverride
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                when (didFailWithError.code) {
                    kCLErrorLocationUnknown -> Unit // transient; Core Location keeps trying
                    kCLErrorDenied -> trySend(Reading.PermissionDenied)
                    else -> trySend(Reading.Error(didFailWithError.localizedDescription))
                }
            }
        }
        manager.delegate = delegate
        onAuthorization(manager)

        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
            delegate.hashCode() // keep a strong reference until the flow closes
        }
    }

    private fun CLLocation.toFix(): LocationFix {
        val (lat, lon) = coordinate.useContents { latitude to longitude }
        return LocationFix(
            latitude = lat,
            longitude = lon,
            // Negative accuracy means the coordinate is invalid.
            accuracyMeters = if (horizontalAccuracy >= 0) horizontalAccuracy else Double.MAX_VALUE,
            speedMps = speed.takeIf { it >= 0 },
            timestamp = Instant.fromEpochMilliseconds((timestamp.timeIntervalSince1970 * 1000).toLong()),
        )
    }
}
