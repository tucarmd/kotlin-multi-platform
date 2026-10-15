package dev.obdlink.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.obdlink.android.R
import dev.obdlink.model.Reading

/**
 * [App 5 · B5] Round gauge drawn with Canvas: a 240° track and a coloured arc up to the value.
 * Its iOS twin is SwiftUI `Gauge` with `.accessoryCircular` in LiveView.swift.
 *
 * [reading] null = no value yet / NO DATA → empty arc and "—".
 */
@Composable
fun Gauge(title: String, unit: String, reading: Reading?, min: Int, max: Int, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    // TRAP: clamp before drawing. Coolant can be below min (-40 °C sensor fault) and a sweep < 0 draws backwards.
    val fraction = reading?.let { ((it.value - min).toFloat() / (max - min)).coerceIn(0f, 1f) } ?: 0f
    Card(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
                    // 0° is 3 o'clock; start at 150° (bottom-left) and sweep 240° clockwise to bottom-right.
                    drawArc(track, startAngle = 150f, sweepAngle = 240f, useCenter = false, style = stroke)
                    if (fraction > 0f) drawArc(fill, 150f, 240f * fraction, useCenter = false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        reading?.value?.toString() ?: stringResource(R.string.no_value),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(unit, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Shown on Live / Codes / Console while no dongle is connected. */
@Composable
fun ConnectFirst(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.connect_first), style = MaterialTheme.typography.bodyLarge)
    }
}
