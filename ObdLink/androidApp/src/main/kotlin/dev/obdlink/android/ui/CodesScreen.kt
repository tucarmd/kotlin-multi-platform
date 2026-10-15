package dev.obdlink.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.obdlink.android.R
import dev.obdlink.model.CodesState
import dev.obdlink.model.ObdState
import dev.obdlink.obd.model.Dtc

/** [App 5 · B5] Codes tab actions. Clearing is two steps (request → confirm) and the core enforces it. */
data class CodesActions(
    val onRead: () -> Unit,
    val onRequestClear: () -> Unit,
    val onConfirmClear: () -> Unit,
    val onCancelClear: () -> Unit,
)

/** [App 5 · B5] Codes tab: read / clear buttons, the code list, and the clear confirmation. Twin: CodesView.swift. */
@Composable
fun CodesScreen(state: ObdState, actions: CodesActions, modifier: Modifier = Modifier) {
    if (!state.isConnected) {
        ConnectFirst(modifier)
        return
    }
    val codes = state.codes
    val busy = codes == CodesState.Reading || codes == CodesState.Clearing
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.onRead, enabled = !busy) { Text(stringResource(R.string.codes_read)) }
            OutlinedButton(onClick = actions.onRequestClear, enabled = !busy) { Text(stringResource(R.string.codes_clear)) }
        }
        when (codes) {
            CodesState.NotRead -> Text(stringResource(R.string.codes_not_read))
            CodesState.Reading -> Busy(stringResource(R.string.codes_reading))
            CodesState.Clearing -> Busy(stringResource(R.string.codes_clearing))
            is CodesState.Failed -> Text(stringResource(codes.reason.label()), color = MaterialTheme.colorScheme.error)
            is CodesState.Loaded -> CodeList(codes.codes)
            is CodesState.ConfirmClear -> {
                CodeList(codes.codes)
                // TRAP: mode 04 also resets the readiness monitors. The dialog text says so; never clear on one tap.
                AlertDialog(
                    onDismissRequest = actions.onCancelClear,
                    title = { Text(stringResource(R.string.clear_title)) },
                    text = { Text(stringResource(R.string.clear_message)) },
                    confirmButton = {
                        TextButton(onClick = actions.onConfirmClear) {
                            Text(stringResource(R.string.clear_confirm), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = { TextButton(onClick = actions.onCancelClear) { Text(stringResource(R.string.cancel)) } },
                )
            }
        }
    }
}

@Composable
private fun Busy(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.padding(2.dp))
        Text(text)
    }
}

@Composable
private fun CodeList(codes: List<Dtc>) {
    if (codes.isEmpty()) {
        Text(stringResource(R.string.codes_none))
        return
    }
    LazyColumn(Modifier.fillMaxWidth()) {
        items(codes, key = { it.code }) { dtc ->
            ListItem(
                headlineContent = { Text(dtc.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(dtc.summary ?: stringResource(R.string.codes_unknown)) },
                trailingContent = { Text(stringResource(dtc.system.label()), style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
