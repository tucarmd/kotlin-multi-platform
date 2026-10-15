package dev.triplog.rules

import dev.triplog.model.FixVerdict
import dev.triplog.model.TrackPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * [App 4 · B1] Geometry rules: distance between two fixes and the GPS noise filter. Pure functions, no I/O.
 *
 * Why our own haversine instead of `Location.distanceTo` / `CLLocation.distance(from:)`: those live in platform code,
 * so they could not be unit-tested in commonTest, and the two phones would not be guaranteed to print the same number.
 *
 * Called by: TripRecorder (live distance, filter), rules/Stats.kt. Tested by: RulesTest.
 */

/** Mean Earth radius (IUGG), in metres. */
const val EARTH_RADIUS_METERS = 6_371_008.8

/** Fixes with a wider accuracy circle than this are dropped. */
const val MAX_ACCURACY_METERS = 30.0

/** 50 m/s = 180 km/h. Anything faster between two fixes is a GPS jump, not a trip. */
const val MAX_SPEED_MPS = 50.0

private const val DEG_TO_RAD = kotlin.math.PI / 180.0

/**
 * Great-circle distance in metres between two coordinates (haversine formula).
 *
 * Example: Hà Nội (21.0285, 105.8542) → TP.HCM (10.7769, 106.7009) ≈ 1_137_000 m.
 * Example: the same point twice → 0.0.
 */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = (lat2 - lat1) * DEG_TO_RAD
    val dLon = (lon2 - lon1) * DEG_TO_RAD
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * DEG_TO_RAD) * cos(lat2 * DEG_TO_RAD) * sin(dLon / 2) * sin(dLon / 2)
    // TRAP: rounding can push h a hair above 1.0 for antipodal points; asin(>1) is NaN. Clamp it.
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(min(1.0, h)))
}

/** Same as [distanceMeters] for two track points. Example: two fixes 0.001° of latitude apart → ≈ 111 m. */
fun distanceMeters(a: TrackPoint, b: TrackPoint): Double = distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)

/**
 * Decides whether [next] is a usable fix, given the last accepted one ([prev], null for the first fix).
 *
 * Examples:
 * - accuracy 45 m → TooInaccurate (whatever prev is)
 * - prev at 10:00:00, next 500 m away at 10:00:01 → TooFast (500 m/s)
 * - prev at 10:00:00, next 10 m away at 10:00:05 → Accepted (2 m/s)
 * - next.time == prev.time → OutOfOrder
 */
fun judgeFix(prev: TrackPoint?, next: TrackPoint): FixVerdict {
    // TRAP: a negative accuracy (iOS) means "invalid", not "perfect". Platform code maps it to a huge number.
    if (next.accuracyMeters > MAX_ACCURACY_METERS) return FixVerdict.TooInaccurate
    if (prev == null) return FixVerdict.Accepted
    val dtSeconds = (next.timeMillis - prev.timeMillis) / 1000.0
    if (dtSeconds <= 0) return FixVerdict.OutOfOrder
    // TRAP: compare the IMPLIED speed (distance / time), not the distance alone. 500 m in 1 s is noise,
    // 500 m in 60 s is a car on a highway.
    val impliedSpeed = distanceMeters(prev, next) / dtSeconds
    return if (impliedSpeed > MAX_SPEED_MPS) FixVerdict.TooFast else FixVerdict.Accepted
}

/** True when [judgeFix] accepts [next]. Example: prev = null, accuracy 5 m → true. */
fun acceptFix(prev: TrackPoint?, next: TrackPoint): Boolean = judgeFix(prev, next) == FixVerdict.Accepted
