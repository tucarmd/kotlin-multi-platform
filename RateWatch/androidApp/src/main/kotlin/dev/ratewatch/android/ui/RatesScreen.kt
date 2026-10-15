package dev.ratewatch.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ratewatch.android.R
import dev.ratewatch.model.Freshness
import dev.ratewatch.model.RateRow
import dev.ratewatch.model.RateTable
import dev.ratewatch.model.RatesState
import dev.ratewatch.model.RefreshError
import dev.ratewatch.rules.ageOf
import dev.ratewatch.rules.currencyCodes
import dev.ratewatch.rules.rateRows

/**
 * [App 3 · B5] Rates tab. Draws [state] and fires actions; computes nothing itself.
 *
 * Why it takes plain values and lambdas (no ViewModel): @Preview can draw it without Koin, and App() decides
 * where the state comes from. Its iOS twin is RatesView.swift, with the same sections in the same order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(
    state: RatesState,
    onRefresh: () -> Unit,
    onSelectBase: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                        Text(
                            updatedText(state.age),
                            style = MaterialTheme.typography.labelMedium,
                            // Red when older than 1 h: the user should not trust these numbers for a big payment.
                            color = if (state.freshness == Freshness.Stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    CurrencyButton(state.base, state.currencies, stringResource(R.string.pick_base), onSelectBase)
                    IconButton(onClick = onRefresh, enabled = !state.refreshing) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
        // The outer Scaffold in App() already pads for the bottom bar; this one only needs the top bar's height.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OfflineBanner(state)
            // PullToRefreshBox needs scrollable content (the LazyColumn) to detect the pull gesture.
            PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize()) {
                    when {
                        state.table != null -> rateList(state, onToggleFavorite)
                        state.showEmptyError -> item { EmptyError(state.error ?: RefreshError.Unknown, onRefresh) }
                        else -> item { Message(stringResource(R.string.loading)) }
                    }
                }
            }
        }
    }
}

/** Slides in when a refresh failed but saved rates are on screen. */
@Composable
private fun OfflineBanner(state: RatesState) {
    // Keep the last error so the text does not vanish while the banner is still sliding out.
    val last = remember { object { var error: RefreshError = RefreshError.Offline } }
    state.error?.let { last.error = it }
    AnimatedVisibility(visible = state.showOfflineBanner, enter = expandVertically(), exit = shrinkVertically()) {
        Text(
            stringResource(R.string.offline_banner, stringResource(last.error.label())),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Header "1 USD equals", then favorites, then all other currencies. Rows are already sorted by the core. */
private fun LazyListScope.rateList(state: RatesState, onToggleFavorite: (String) -> Unit) {
    item(key = "base") {
        Text(
            stringResource(R.string.base_label, state.base),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
    val favorites = state.rows.filter { it.favorite }
    val others = state.rows.filterNot { it.favorite }
    if (favorites.isNotEmpty()) {
        item(key = "h-fav") { SectionHeader(stringResource(R.string.favorites)) }
        items(favorites, key = { "f-" + it.code }) { RateLine(it, onToggleFavorite) }
    }
    item(key = "h-all") { SectionHeader(stringResource(R.string.all_currencies)) }
    items(others, key = { "a-" + it.code }) { RateLine(it, onToggleFavorite) }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun RateLine(row: RateRow, onToggleFavorite: (String) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(row.code, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(row.rateText, fontFamily = FontFamily.Monospace)
            IconButton(onClick = { onToggleFavorite(row.code) }) {
                if (row.favorite) {
                    Icon(Icons.Filled.Star, stringResource(R.string.unstar), tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Outlined.Star, stringResource(R.string.star), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))
    }
}

/** Nothing saved AND the download failed: say why and offer a retry. */
@Composable
private fun EmptyError(error: RefreshError, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.empty_error_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(error.label()), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
}

/** Preview with rows built by the real shared rules, offline banner on. */
@Preview(showBackground = true)
@Composable
private fun RatesScreenPreview() {
    val now = 10 * 3_600_000L
    val table = RateTable("USD", mapOf("USD" to 1.0, "EUR" to 0.894425, "JPY" to 150.12, "VND" to 26_312.5), now - 2 * 3_600_000L)
    MaterialTheme {
        RatesScreen(
            state = RatesState(
                base = "USD",
                table = table,
                rows = rateRows(table, setOf("VND")),
                freshness = Freshness.Stale,
                age = ageOf(table.fetchedAt, now),
                error = RefreshError.Offline,
                currencies = currencyCodes(table),
            ),
            onRefresh = {},
            onSelectBase = {},
            onToggleFavorite = {},
        )
    }
}
