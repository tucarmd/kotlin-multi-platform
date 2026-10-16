package dev.devicepulse.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import co.touchlab.kermit.Logger
import dev.devicepulse.model.LocationFix
import dev.devicepulse.model.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Instant

/** Uses the framework LocationManager, so it works without Google Play Services. */
class AndroidLocationSensor(private val context: Context) : LocationSensor {

    @SuppressLint("MissingPermission") // checked by hasPermission() before any LocationManager call
    override fun observe(): Flow<Reading<LocationFix>> = callbackFlow {
        if (!hasPermission()) {
            trySend(Reading.PermissionDenied)
            awaitClose()
            return@callbackFlow
        }
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            trySend(Reading.Error("Location services are off"))
            awaitClose()
            return@callbackFlow
        }

        trySend(Reading.Loading)
        providers.mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { trySend(Reading.Available(it.toFix())) }

        val listener = LocationListener { location -> trySend(Reading.Available(location.toFix())) }
        try {
            providers.forEach { provider ->
                manager.requestLocationUpdates(provider, 2_000L, 0f, listener, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            Logger.w(e) { "Location permission revoked while subscribing" }
            trySend(Reading.PermissionDenied)
        }
        awaitClose { manager.removeUpdates(listener) }
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    private fun Location.toFix() = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.MAX_VALUE,
        speedMps = if (hasSpeed()) speed.toDouble() else null,
        timestamp = Instant.fromEpochMilliseconds(time),
    )
}
