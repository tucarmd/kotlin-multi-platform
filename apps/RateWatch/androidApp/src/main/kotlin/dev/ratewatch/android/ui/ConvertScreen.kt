package dev.ratewatch.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ratewatch.android.R
import dev.ratewatch.model.ConvertInput
import dev.ratewatch.model.ConvertResult
import dev.ratewatch.model.ConvertState
import dev.ratewatch.model.RateTable
import dev.ratewatch.rules.convertInput
import dev.ratewatch.rules.currencyCodes

/**
 * [App 3 · B5] Convert tab: amount field, two currency buttons, swap, result card. Computes nothing itself:
 * the result (including its formatted text) comes from RatesCore.convert, so both phones print the same digits.
 * Its iOS twin is ConvertView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    state: ConvertState,
    onAmountChanged: (String) -> Unit,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit,
) {
    val input = state.input
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_convert), fontWeight = FontWeight.SemiBold) }) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // keeps the result card above the keyboard
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val error = (state.result as? ConvertResult.Invalid)?.error
            OutlinedTextField(
                value = input.amountText,
                onValueChange = onAmountChanged,
                label = { Text(stringResource(R.string.amount)) },
                suffix = { Text(input.from) },
                isError = error != null,
                supportingText = error?.let { { Text(stringResource(it.label())) } },
                singleLine = true,
                // PLATFORM: Decimal shows digits plus the separator of the phone's language; iOS uses .decimalPad.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.from), style = MaterialTheme.typography.labelMedium)
                    CurrencyButton(input.from, state.currencies, stringResource(R.string.pick_currency), onFromSelected, Modifier.fillMaxWidth())
                }
                val swap = stringResource(R.string.swap)
                FilledTonalIconButton(onClick = onSwap, modifier = Modifier.semantics { contentDescription = swap }) {
                    Text("⇄", style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.to), style = MaterialTheme.typography.labelMedium)
                    CurrencyButton(input.to, state.currencies, stringResource(R.string.pick_currency), onToSelected, Modifier.fillMaxWidth())
                }
            }

            ResultCard(state)
        }
    }
}

/** One branch per ConvertResult case. `when` without `else` stops compiling if a case is added later. */
@Composable
private fun ResultCard(state: ConvertState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (val result = state.result) {
                ConvertResult.Waiting -> Text(stringResource(R.string.convert_waiting))
                is ConvertResult.Invalid -> Text(stringResource(result.error.label()), color = MaterialTheme.colorScheme.error)
                ConvertResult.NoRates -> Text(stringResource(R.string.convert_no_rates))
                is ConvertResult.Ready -> {
                    Text(
                        "${result.amountText} ${state.input.to}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.rate_line, state.input.from, result.rateText, state.input.to),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Preview with a result computed by the real shared rule, so it matches what the app shows. */
@Preview(showBackground = true)
@Composable
private fun ConvertScreenPreview() {
    val table = RateTable("USD", mapOf("USD" to 1.0, "EUR" to 0.894425, "VND" to 26_312.5), 0)
    val input = ConvertInput(amountText = "100", from = "EUR", to = "VND")
    MaterialTheme {
        ConvertScreen(
            state = ConvertState(input, convertInput(input, table), currencyCodes(table)),
            onAmountChanged = {},
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
        )
    }
}
