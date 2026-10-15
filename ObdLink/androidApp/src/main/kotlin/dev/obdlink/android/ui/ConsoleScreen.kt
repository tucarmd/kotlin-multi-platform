package dev.obdlink.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.obdlink.android.R
import dev.obdlink.model.ConsoleKind
import dev.obdlink.model.ObdState

/** Commands a learner tries first: battery voltage, protocol name, supported PIDs, RPM. */
private val QUICK_COMMANDS = listOf("ATRV", "ATDP", "0100", "010C")

/**
 * [App 5 · B5] Console tab: raw log of every command and answer (`010C` → `41 0C 1A F8`) and a field to type AT / hex.
 * The log is filled by the core (init, codes and console commands; live polling is not logged, it would flood).
 * Its iOS twin is ConsoleView.swift.
 */
@Composable
fun ConsoleScreen(state: ObdState, onSend: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Keep the newest line visible.
    LaunchedEffect(state.console.size) {
        if (state.console.isNotEmpty()) listState.animateScrollToItem(state.console.lastIndex)
    }
    val send = {
        onSend(text)
        text = ""
    }
    Column(modifier.fillMaxSize().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.connection.label(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text(stringResource(R.string.console_clear)) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
            if (state.console.isEmpty()) item { Text(stringResource(R.string.console_empty)) }
            itemsIndexed(state.console) { _, line ->
                Text(
                    line.kind.format(line.text),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (line.kind) {
                        ConsoleKind.Sent -> MaterialTheme.colorScheme.primary
                        ConsoleKind.Received -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.error
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_COMMANDS.forEach { command -> AssistChip(onClick = { onSend(command) }, label = { Text(command) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.console_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = send) { Text(stringResource(R.string.console_send)) }
        }
    }
}
