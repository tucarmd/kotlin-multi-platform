package dev.triplog.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import dev.triplog.model.LocationProblem
import dev.triplog.model.LocationUnavailableException
import dev.triplog.model.TrackPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [App 4 · B3] LocationStream on Android, using the framework LocationManager (no Google Play Services needed,
 * same choice as DevicePulse's AndroidLocationSensor, and it works on every emulator image).
 *
 * Why `callbackFlow`: LocationManager pushes fixes into a listener. callbackFlow turns that callback API into a
 * cold Flow, and `awaitClose { removeUpdates }` is the one place that turns the GPS off again.
 *
 * Called by: TripRecorder (through the LocationStream interface). Calls: LocationManager.
 */
class AndroidLocationStream(private val context: Context) : LocationStream {

    /**
     * GPS fixes about once per second. Fails with PermissionDenied when no location permission is granted,
     * LocationOff when the GPS provider is disabled (or gets disabled while recording).
     */
    @SuppressLint("MissingPermission") // checked by hasPermission() right before requestLocationUpdates
    override fun points(): Flow<TrackPoint> = callbackFlow {
        if (!hasPermission()) {
            close(LocationUnavailableException(LocationProblem.PermissionDenied))
            return@callbackFlow
        }
        val manager = context.getSystemService(LocationManager::class.java)
        // Only the GPS provider: network fixes are 50–2000 m wide and would be dropped by the filter anyway,
        // and mixing two providers delivers out-of-order fixes. `adb emu geo fix` also feeds this provider.
        if (manager == null || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            close(LocationUnavailableException(LocationProblem.LocationOff))
            return@callbackFlow
        }

        // TRAP: a full object, not a `LocationListener { }` lambda. Before API 30 the other three methods were
        // abstract, so the system calling onStatusChanged on a lambda crashes with AbstractMethodError on old phones.
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toPoint())
            }

            override fun onProviderDisabled(provider: String) {
                close(LocationUnavailableException(LocationProblem.LocationOff))
            }

            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Deprecated in API 29, still called on older versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            // PLATFORM: callbacks arrive on the main Looper. iOS needs the same: a CLLocationManager created on a
            // thread without a run loop never calls its delegate (see IosLocationStream).
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            close(LocationUnavailableException(LocationProblem.PermissionDenied)) // revoked between check and call
        }
        // TRAP: forgetting removeUpdates leaves the GPS on after the trip ends, until the process dies.
        awaitClose { manager.removeUpdates(listener) }
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    private fun Location.toPoint() = TrackPoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.MAX_VALUE,
        speedMps = if (hasSpeed()) speed.toDouble() else null,
        timeMillis = time,
    )
}
