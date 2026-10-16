package dev.tipsplit.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tipsplit.android.R
import dev.tipsplit.model.Split
import dev.tipsplit.model.SplitInput
import dev.tipsplit.model.SplitResult
import dev.tipsplit.model.SplitState
import dev.tipsplit.rules.calculate
import dev.tipsplit.rules.formatMoney
import dev.tipsplit.ui.UiIds
import org.koin.compose.viewmodel.koinViewModel

/**
 * [App 1 · B5] Entry composable: gets the ViewModel from Koin and turns its StateFlow into Compose state.
 *
 * Why split Route / Screen: SplitScreen takes plain values and lambdas, so @Preview can draw it without Koin.
 *
 * [Bài 8 · ViewModel] koinViewModel() asks Koin for SplitViewModel, scoped to this screen, so rotation gets the
 * same instance back. collectAsStateWithLifecycle() turns the StateFlow into Compose State: a new value redraws.
 */
@Composable
fun SplitRoute(viewModel: SplitViewModel = koinViewModel()) {
    // collectAsStateWithLifecycle stops collecting while the app is in the background.
    val state by viewModel.state.collectAsStateWithLifecycle()
    SplitScreen(
        state = state,
        tipOptions = viewModel.tipOptions,
        onBillChanged = viewModel::onBillChanged,
        onTipSelected = viewModel::onTipSelected,
        onAddPerson = viewModel::addPerson,
        onRemovePerson = viewModel::removePerson,
        onRoundUpToggled = viewModel::onRoundUpToggled,
        onReset = viewModel::reset,
    )
}

/**
 * [App 1 · B5] The whole screen. Draws [state] and fires actions; computes nothing itself.
 *
 * [Bài 6 · Compose cơ bản] A @Composable is a function that describes UI for the given input. When `state` changes,
 * Compose calls it again and updates only what differs. Layout = Column / Row; size, spacing and test ids =
 * Modifier, read top to bottom (padding before verticalScroll is not the same as after).
 * [Bài 7] Every visible text comes from stringResource(R.string.x), never a literal, except symbols ("đ", "%").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SplitScreen(
    state: SplitState,
    tipOptions: List<Int>,
    onBillChanged: (String) -> Unit,
    onTipSelected: (Int) -> Unit,
    onAddPerson: () -> Unit,
    onRemovePerson: () -> Unit,
    onRoundUpToggled: () -> Unit,
    onReset: () -> Unit,
) {
    val input = state.input
    MaterialTheme {
        Scaffold(
            // [Phụ lục P2] Expose every testTag as an Android resource-id, so Maestro's `id:` finds the same names
            // as Compose UI test. Without it Maestro only sees text, and the shared .maestro flow breaks on Android.
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                    actions = {
                        TextButton(onClick = onReset, modifier = Modifier.testTag(UiIds.RESET)) {
                            Text(stringResource(R.string.reset))
                        }
                    },
                )
            },
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
                val error = (state.result as? SplitResult.Invalid)?.error
                OutlinedTextField(
                    value = input.billText,
                    onValueChange = onBillChanged,
                    label = { Text(stringResource(R.string.bill)) },
                    suffix = { Text("đ") },
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it.label()), Modifier.testTag(UiIds.BILL_ERROR)) } },
                    singleLine = true,
                    // PLATFORM: Number shows a digits-only pad; iOS uses .numberPad for the same thing.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag(UiIds.BILL_INPUT),
                )

                Text(stringResource(R.string.tip), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    tipOptions.forEachIndexed { index, percent ->
                        SegmentedButton(
                            selected = input.tipPercent == percent,
                            onClick = { onTipSelected(percent) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tipOptions.size),
                            modifier = Modifier.testTag(UiIds.tip(percent)),
                        ) { Text("$percent%") }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.people),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = onRemovePerson,
                        enabled = state.canRemovePerson,
                        modifier = Modifier.testTag(UiIds.PEOPLE_MINUS),
                    ) { Text("−") }
                    Text(
                        "${input.people}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp).testTag(UiIds.PEOPLE_COUNT),
                    )
                    FilledTonalIconButton(
                        onClick = onAddPerson,
                        enabled = state.canAddPerson,
                        modifier = Modifier.testTag(UiIds.PEOPLE_PLUS),
                    ) { Text("+") }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.round_up), modifier = Modifier.weight(1f))
                    Switch(
                        checked = input.roundUp,
                        onCheckedChange = { onRoundUpToggled() },
                        modifier = Modifier.testTag(UiIds.ROUND_UP),
                    )
                }

                ResultCard(state.result)
            }
        }
    }
}

/** [App 1 · B5] One branch per SplitResult case. `when` without `else` stops compiling if a case is added later. */
@Composable
private fun ResultCard(result: SplitResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (result) {
                SplitResult.Waiting -> Text(stringResource(R.string.waiting), Modifier.testTag(UiIds.RESULT_MESSAGE))
                is SplitResult.Invalid -> Text(
                    stringResource(R.string.fix_input),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(UiIds.RESULT_MESSAGE),
                )
                is SplitResult.Ready -> ReadyResult(result.split)
            }
        }
    }
}

@Composable
private fun ReadyResult(split: Split) {
    Text(stringResource(R.string.per_person), style = MaterialTheme.typography.labelLarge)
    Text(
        "${formatMoney(split.perPerson)} đ",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag(UiIds.RESULT_PER_PERSON),
    )
    AmountRow(stringResource(R.string.tip_amount), split.tip, UiIds.RESULT_TIP)
    AmountRow(stringResource(R.string.total), split.total, UiIds.RESULT_TOTAL)
    if (split.extra > 0) {
        Text(stringResource(R.string.extra, formatMoney(split.extra)), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AmountRow(label: String, amount: Long, tag: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text("${formatMoney(amount)} đ", Modifier.testTag(tag))
    }
}

/**
 * Preview with a result computed by the real shared rule, so it matches what the app shows.
 *
 * [Bài 6 · @Preview] Android Studio draws this in the Split / Design pane without running the app.
 * It only works on a @Composable with no parameters (or default ones), which is why SplitScreen takes plain values.
 */
@Preview(showBackground = true)
@Composable
private fun SplitScreenPreview() {
    val input = SplitInput(billText = "100.000", tipPercent = 10, people = 3)
    SplitScreen(
        state = SplitState(input = input, result = calculate(input)),
        tipOptions = listOf(0, 5, 10, 15),
        onBillChanged = {},
        onTipSelected = {},
        onAddPerson = {},
        onRemovePerson = {},
        onRoundUpToggled = {},
        onReset = {},
    )
}
