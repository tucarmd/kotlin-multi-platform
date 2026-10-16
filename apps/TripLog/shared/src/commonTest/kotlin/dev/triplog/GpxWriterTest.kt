package dev.triplog

import dev.triplog.data.GpxWriter
import dev.triplog.model.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [App 4 · B7] GpxWriter output, checked as text on every platform.
 * The full XML parse lives in androidUnitTest/GpxParseTest (javax.xml exists only on the JVM).
 */
class GpxWriterTest {

    private val trip = Trip(
        id = 7, startedAtMillis = 1_700_000_000_000, endedAtMillis = 1_700_000_100_000,
        distanceMeters = 200.0, durationMillis = 20_000, maxSpeedMps = 10.0, pointCount = 4,
    )
    private val points = listOf(
        fix(1_700_000_000, 0.0),
        fix(1_700_000_010, 100.0),
        fix(1_700_000_610, 5_000.0).copy(segment = 1),
        fix(1_700_000_620, 5_100.0).copy(segment = 1),
    )

    @Test
    fun `declares GPX 1 point 1 with the topografix namespace`() {
        val gpx = GpxWriter.write(trip, points)
        assertTrue(gpx.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue("""<gpx version="1.1" creator="Trip Log" xmlns="http://www.topografix.com/GPX/1/1"""" in gpx)
        assertTrue(gpx.endsWith("</gpx>"))
    }

    @Test
    fun `one trkseg per segment and one trkpt per point`() {
        val gpx = GpxWriter.write(trip, points)
        assertEquals(2, Regex("<trkseg>").findAll(gpx).count())
        assertEquals(4, Regex("<trkpt ").findAll(gpx).count())
    }

    @Test
    fun `coordinates have 7 decimals and times are UTC`() {
        val gpx = GpxWriter.write(trip, points)
        assertTrue("""<trkpt lat="10.0000000" lon="106.0000000"><time>2023-11-14T22:13:20Z</time></trkpt>""" in gpx, gpx)
    }

    @Test
    fun `special characters in the name are escaped`() {
        val gpx = GpxWriter.write(trip, points, name = "A & B <test>")
        assertTrue("<name>A &amp; B &lt;test&gt;</name>" in gpx)
    }

    @Test
    fun `file name uses the trip id`() {
        assertEquals("trip-7.gpx", GpxWriter.fileName(trip))
    }
}
