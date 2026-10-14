package dev.devicepulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.devicepulse.model.Reading
import dev.devicepulse.resources.Res
import dev.devicepulse.resources.grant_permission
import dev.devicepulse.resources.state_error
import dev.devicepulse.resources.state_loading
import dev.devicepulse.resources.state_permission_denied
import dev.devicepulse.resources.state_unsupported
import org.jetbrains.compose.resources.stringResource

/** Draws the four non-value states of a [Reading] the same way everywhere; [content] handles Available. */
@Composable
fun <T> ReadingContent(
    reading: Reading<T>,
    onGrantPermission: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (reading) {
        Reading.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.state_loading), style = MaterialTheme.typography.bodyMedium)
        }
        Reading.PermissionDenied -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.state_permission_denied), color = MaterialTheme.colorScheme.error)
            if (onGrantPermission != null) {
                OutlinedButton(onClick = onGrantPermission) { Text(stringResource(Res.string.grant_permission)) }
            }
        }
        Reading.Unsupported -> Text(
            stringResource(Res.string.state_unsupported),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is Reading.Error -> Text(
            stringResource(Res.string.state_error, reading.message),
            color = MaterialTheme.colorScheme.error,
        )
        is Reading.Available -> content(reading.value)
    }
}

@Composable
fun GradeChip(grade: Grade) {
    Surface(
        shape = RoundedCornerShape(50),
        color = grade.tone.color().copy(alpha = 0.14f),
        contentColor = grade.tone.color(),
    ) {
        Text(
            stringResource(grade.label),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SensorCard(
    emoji: String,
    title: String,
    onClick: () -> Unit,
    grade: Grade?,
    body: @Composable () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (grade != null) GradeChip(grade)
            }
            body()
        }
    }
}

@Composable
fun FieldRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}
