package dev.triplog.model

/**
 * [App 4 · B1] Plain data the whole app passes around. No logic lives here: rules/ computes, core/ stores.
 *
 * Why times are epoch milliseconds (Long) and not Instant: the same number goes into SQLite, into GPX and across
 * to Swift (Int64) without conversion, and tests can build points with simple numbers.
 *
 * Used by: rules/, core/TripRecorder, data/TripRepository, data/GpxWriter, both UIs.
 */

/**
 * One GPS fix, exactly as the platform delivered it.
 *
 * [segment] starts at 0 and goes up by one after every pause/resume. Distance and moving time are only summed
 * inside a segment, so the gap while the trip was paused never counts.
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    /** Radius of the 68 % confidence circle, in metres. Bigger = worse. */
    val accuracyMeters: Double,
    /** Speed reported by the GPS chip in m/s, or null when the platform has none (first fix, network provider). */
    val speedMps: Double?,
    val timeMillis: Long,
    val segment: Int = 0,
)

/** Why a fix was thrown away by `judgeFix`. The recorder counts the rejects so the UI can show them. */
enum class FixVerdict {
    Accepted,

    /** Accuracy circle wider than the limit: typical indoors or right after GPS starts. */
    TooInaccurate,

    /** Implied speed between the previous fix and this one is impossible for a person or a car. */
    TooFast,

    /** Same or older timestamp than the previous fix (duplicate delivery from two providers). */
    OutOfOrder,
}

/**
 * Totals for a list of points. Computed by `tripStats`, never stored half-way.
 *
 * [movingMillis] only counts time inside segments, so pauses are excluded.
 */
data class TripStats(
    val distanceMeters: Double,
    val movingMillis: Long,
    val averageSpeedMps: Double,
    val maxSpeedMps: Double,
)

/** One saved trip, as shown in the list. Points are loaded separately (see [TripDetail]). */
data class Trip(
    val id: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val distanceMeters: Double,
    /** Recording time without pauses. */
    val durationMillis: Long,
    val maxSpeedMps: Double,
    val pointCount: Int,
) {
    val averageSpeedMps: Double get() = if (durationMillis > 0) distanceMeters / (durationMillis / 1000.0) else 0.0
}

/** A trip together with all of its points, for the detail screen and GPX export. */
data class TripDetail(val trip: Trip, val points: List<TrackPoint>)

/**
 * Why the location stream could not deliver. The UI turns each case into a message; the core never holds UI text.
 *
 * TRAP: avoid single capital letters inside names. SKIE lower-cases runs of capitals for Swift
 * (`GPSOff` would become `.gpsoff`). `LocationOff` stays `.locationOff`.
 */
enum class LocationProblem { PermissionDenied, LocationOff }

/** Thrown by a LocationStream; caught by TripRecorder and turned into [LiveTrip.problem]. Never reaches the UI. */
class LocationUnavailableException(val problem: LocationProblem) : Exception(problem.name)

/** Everything the Record screen draws while a trip is running or paused. */
data class LiveTrip(
    val startedAtMillis: Long,
    /** Recording time so far, pauses excluded. Ticks once per second. */
    val elapsedMillis: Long,
    val distanceMeters: Double,
    /** Speed of the last accepted fix in m/s, null before the first one. */
    val currentSpeedMps: Double?,
    /** Accuracy of the last fix received (accepted or not), so the user sees when GPS is weak. */
    val lastAccuracyMeters: Double?,
    /** Accepted points so far, in order. Drawn as the live route. */
    val points: List<TrackPoint>,
    /** How many fixes the noise filter dropped. */
    val droppedFixes: Int,
    /** Set when the platform stopped delivering (permission revoked, location switched off). */
    val problem: LocationProblem?,
) {
    val averageSpeedMps: Double get() = if (elapsedMillis > 0) distanceMeters / (elapsedMillis / 1000.0) else 0.0
}

/**
 * The recorder's state machine. Exactly one case at a time, so the UI cannot forget one (`when` / `switch`).
 *
 * ```
 * Idle ──start──▶ Recording ──pause──▶ Paused ──resume──▶ Recording
 *                    │                   │
 *                    └──────stop─────────┴──▶ Saving ──▶ Idle
 * ```
 *
 * TRAP: sealed **class**, not sealed interface. Objective-C keeps subclass info only for classes,
 * so Swift (via SKIE `onEnum(of:)`) sees a proper enum.
 */
sealed class RecordState {
    data object Idle : RecordState()
    data class Recording(val trip: LiveTrip) : RecordState()
    data class Paused(val trip: LiveTrip) : RecordState()

    /** Last points are being flushed and the trip row closed. Buttons are disabled. */
    data object Saving : RecordState()
}
