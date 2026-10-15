package dev.triplog.data

import dev.triplog.model.TrackPoint
import dev.triplog.model.Trip
import dev.triplog.rules.formatFixed
import kotlin.time.Instant

/**
 * [App 4 · B3] GpxWriter: turns a trip into a GPX 1.1 document (the format Strava, gpx.studio, Garmin… import).
 *
 * Why in commonMain: GPX is only XML text, no platform API needed. Both UIs get byte-identical files and the
 * output is tested once in GpxWriterTest (and parsed back with a real XML parser in GpxParseTest on the JVM).
 *
 * Called by: TripLibrary.gpx(). Calls: rules/formatFixed.
 */
object GpxWriter {

    /** File name both UIs use when sharing. Example: trip id 7 → "trip-7.gpx". */
    fun fileName(trip: Trip): String = "trip-${trip.id}.gpx"

    /**
     * The whole GPX file for [trip]. One `<trkseg>` per recording segment, so a pause shows as a gap on the map.
     *
     * Example (one point):
     * ```
     * <trkpt lat="21.0285000" lon="105.8542000"><time>2026-10-15T01:00:00Z</time></trkpt>
     * ```
     */
    fun write(trip: Trip, points: List<TrackPoint>, name: String = "Trip ${trip.id}"): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine(
            """<gpx version="1.1" creator="Trip Log" xmlns="http://www.topografix.com/GPX/1/1" """ +
                """xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" """ +
                """xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""",
        )
        appendLine("  <metadata>")
        appendLine("    <name>${escape(name)}</name>")
        appendLine("    <time>${isoTime(trip.startedAtMillis)}</time>")
        appendLine("  </metadata>")
        appendLine("  <trk>")
        appendLine("    <name>${escape(name)}</name>")
        points.groupBy { it.segment }.entries.sortedBy { it.key }.forEach { (_, segment) ->
            appendLine("    <trkseg>")
            segment.forEach { p ->
                // TRAP: coordinates with a fixed 7 decimals (≈ 1 cm). Double.toString() can print "1.0E-4".
                appendLine(
                    """      <trkpt lat="${formatFixed(p.latitude, 7)}" lon="${formatFixed(p.longitude, 7)}">""" +
                        "<time>${isoTime(p.timeMillis)}</time></trkpt>",
                )
            }
            appendLine("    </trkseg>")
        }
        appendLine("  </trk>")
        append("</gpx>")
    }

    /** ISO-8601 UTC, e.g. 1_700_000_000_000 → "2023-11-14T22:13:20Z". GPX requires UTC with the trailing Z. */
    private fun isoTime(millis: Long): String = Instant.fromEpochMilliseconds(millis).toString()

    /** The five XML special characters. A trip named `A & B <test>` must not break the file. */
    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
