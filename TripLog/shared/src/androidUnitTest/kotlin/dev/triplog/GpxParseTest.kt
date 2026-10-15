package dev.triplog

import dev.triplog.data.GpxWriter
import dev.triplog.model.Trip
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [App 4 · B7] The GPX we write can be read back by a real XML parser (JVM only: javax.xml is not in Kotlin/Native).
 * This is the "GPX parses again" case from the plan; gpx.studio uses the same rules.
 */
class GpxParseTest {

    @Test
    fun `written GPX parses back to the same points`() {
        val trip = Trip(7, 1_700_000_000_000, 1_700_000_020_000, 111.0, 20_000, 5.5, 3)
        val points = listOf(fix(1_700_000_000, 0.0), fix(1_700_000_010, 55.0), fix(1_700_000_020, 111.0))
        val gpx = GpxWriter.write(trip, points, name = "Walk & talk")

        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(gpx.byteInputStream())

        assertEquals("1.1", doc.documentElement.getAttribute("version"))
        val parsed = doc.getElementsByTagNameNS("http://www.topografix.com/GPX/1/1", "trkpt")
        assertEquals(3, parsed.length)
        val last = parsed.item(2) as Element
        assertEquals(points[2].latitude, last.getAttribute("lat").toDouble(), 1e-7)
        assertEquals(106.0, last.getAttribute("lon").toDouble(), 1e-7)
        assertEquals("Walk & talk", doc.getElementsByTagNameNS("*", "name").item(0).textContent)
    }
}
