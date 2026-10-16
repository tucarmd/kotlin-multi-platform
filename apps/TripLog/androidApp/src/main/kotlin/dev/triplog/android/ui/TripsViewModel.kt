package dev.triplog.android.ui

import androidx.lifecycle.ViewModel
import dev.triplog.core.TripLibrary
import dev.triplog.model.TripDetail

/**
 * [App 4 · B5] Thin Android wrapper around the shared TripLibrary, for the Trips and Trip detail screens.
 * Its iOS twin is TripsModel.swift.
 */
class TripsViewModel(private val library: TripLibrary) : ViewModel() {
    val trips = library.trips

    fun delete(tripId: Long) = library.delete(tripId)

    /** Null when the trip was deleted meanwhile. */
    suspend fun load(tripId: Long): TripDetail? = library.load(tripId)

    fun gpx(detail: TripDetail): String = library.gpx(detail)
    fun gpxFileName(detail: TripDetail): String = library.gpxFileName(detail)
    fun speeds(detail: TripDetail): List<Double> = library.speeds(detail)
}
