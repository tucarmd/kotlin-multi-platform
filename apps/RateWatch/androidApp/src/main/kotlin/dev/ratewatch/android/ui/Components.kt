package dev.ratewatch.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ratewatch.android.R

/**
 * [App 3 · B5] Small pieces shared by both tabs: a button that shows a currency code and opens a picker.
 * Its iOS twin is the `Menu`/`Picker` in RatesView.swift and ConvertView.swift.
 */
@Composable
fun CurrencyButton(
    code: String,
    currencies: List<String>,
    title: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable: the dialog stays open across rotation.
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text(code, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
    }
    if (open) {
        CurrencyPickerDialog(
            title = title,
            currencies = currencies,
            selected = code,
            onPick = { open = false; onPick(it) },
            onDismiss = { open = false },
        )
    }
}

/** A scrollable list of codes in a dialog. The list comes from the core (codes in the saved table). */
@Composable
fun CurrencyPickerDialog(
    title: String,
    currencies: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(currencies, key = { it }) { code ->
                    Text(
                        code,
                        color = if (code == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (code == selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(code) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
