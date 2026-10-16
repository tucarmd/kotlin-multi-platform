package dev.triplog.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.triplog.model.TrackPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/**
 * [App 4 · B5] Draws a route with Compose Canvas: no Google Maps SDK, no API key, works on every emulator.
 *
 * Why not maps-compose: it needs a Maps API key in the manifest and Play Services on the device. A practice app
 * only needs the SHAPE of the route; iOS gets a real map for free with SwiftUI Map + MapPolyline.
 *
 * Projection: equirectangular, with longitude scaled by cos(latitude) so a square walk stays square.
 * Good enough for a city-sized trip; wrong near the poles, which nobody walks across.
 */
@Composable
fun RouteCanvas(points: List<TrackPoint>, emptyText: String, modifier: Modifier = Modifier, height: Dp = 240.dp) {
    val line = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (points.size < 2) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        Canvas(Modifier.fillMaxSize().padding(20.dp)) {
            val midLat = (points.minOf { it.latitude } + points.maxOf { it.latitude }) / 2
            val lonScale = cos(midLat * PI / 180)
            val xs = points.map { it.longitude * lonScale }
            val ys = points.map { it.latitude }
            val minX = xs.min()
            val minY = ys.min()
            // Same scale on both axes, so the route keeps its real shape; tiny spans get a floor to avoid /0.
            val span = max(max(xs.max() - minX, ys.max() - minY), 1e-6)
            val scale = minOf(size.width, size.height) / span.toFloat()
            val offsetX = (size.width - ((xs.max() - minX) * scale).toFloat()) / 2
            val offsetY = (size.height - ((ys.max() - minY) * scale).toFloat()) / 2
            // TRAP: screen y grows downwards, latitude grows northwards. Flip y or the map is upside down.
            fun at(i: Int) = Offset(
                offsetX + ((xs[i] - minX) * scale).toFloat(),
                size.height - offsetY - ((ys[i] - minY) * scale).toFloat(),
            )

            val path = Path()
            points.indices.forEach { i ->
                // A new segment (after a pause) starts a new sub-path: no straight line across the paused gap.
                if (i == 0 || points[i].segment != points[i - 1].segment) path.moveTo(at(i).x, at(i).y)
                else path.lineTo(at(i).x, at(i).y)
            }
            drawPath(path, line, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Color(0xFF2E7D32), radius = 6.dp.toPx(), center = at(0))
            drawCircle(Color(0xFFC62828), radius = 6.dp.toPx(), center = at(points.lastIndex))
        }
    }
}

/** [App 4 · B5] Speed over the trip as a simple line, scaled to the top speed. Values are m/s. */
@Composable
fun SpeedChart(speeds: List<Double>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.tertiary
    val top = speeds.maxOrNull()?.takeIf { it > 0 } ?: return
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val stepX = if (speeds.size > 1) size.width / (speeds.size - 1) else 0f
        val path = Path()
        speeds.forEachIndexed { i, speed ->
            val point = Offset(i * stepX, size.height - (speed / top).toFloat() * size.height)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** [App 4 · B5] One labelled number, e.g. "Distance / 1.23 km". */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** [App 4 · B5] Two tiles side by side, the grid unit of the Record and Detail screens. */
@Composable
fun StatRow(first: Pair<String, String>, second: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(first.first, first.second, Modifier.weight(1f))
        StatTile(second.first, second.second, Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
private fun RouteCanvasPreview() {
    val square = listOf(0.0 to 0.0, 0.0 to 0.001, 0.001 to 0.001, 0.001 to 0.0, 0.0 to 0.0)
        .mapIndexed { i, (dLat, dLon) -> TrackPoint(21.0285 + dLat, 105.8542 + dLon, 5.0, 1.4, i * 60_000L) }
    MaterialTheme { RouteCanvas(square, emptyText = "") }
}
