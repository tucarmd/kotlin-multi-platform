package dev.devicepulse.ui

import dev.devicepulse.model.SensorKind
import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

/**
 * Carries the enum's name, not the enum: on iOS, navigation has no reflection to derive a NavType for
 * custom enums and crashes building the graph ("could not find any NavType for argument sensor").
 */
@Serializable
data class DetailRoute(val sensorName: String) {
    constructor(sensor: SensorKind) : this(sensor.name)

    val sensor: SensorKind get() = SensorKind.valueOf(sensorName)
}

@Serializable
object HistoryRoute
