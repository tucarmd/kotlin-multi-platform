package dev.devicepulse.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Fake readings standing in for DevicePulse's sensors; state survives every hot reload.
data class Reading(val label: String, val value: Int, val unit: String)

@Composable
fun PulseScreen() {
    var running by remember { mutableStateOf(true) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(running) {
        while (running) {
            delay(1_000)
            tick++
        }
    }
    val readings = listOf(
        Reading("Battery", 80 - tick % 20, "%"),
        Reading("CPU load", Random(tick).nextInt(10, 90), "%"),
        Reading("Latency", Random(tick + 7).nextInt(20, 180), "ms"),
    )

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("DevicePulse", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("● LIVE · uptime ${tick}s", color = Color(0xFF30A46C), fontWeight = FontWeight.Medium)
            readings.forEach { ReadingCard(it) }
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "Pause monitor" else "Resume monitor")
            }
        }
    }
}

@Composable
fun ReadingCard(reading: Reading) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(10.dp).background(statusColor(reading.value), CircleShape))
                Spacer(Modifier.size(8.dp))
                Text(reading.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${reading.value} ${reading.unit}", fontSize = 20.sp)
            }
            Spacer(Modifier.size(10.dp))
            LinearProgressIndicator(
                progress = { reading.value.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = statusColor(reading.value),
            )
        }
    }
}

fun statusColor(value: Int): Color = when {
    value > 70 -> Color(0xFFE5484D)
    value > 40 -> Color(0xFFF5A524)
    else -> Color(0xFF30A46C)
}
