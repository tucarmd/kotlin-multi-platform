package dev.ratewatch.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ratewatch.android.R
import org.koin.compose.viewmodel.koinViewModel

private object Routes {
    const val RATES = "rates"
    const val CONVERT = "convert"
}

/**
 * [App 3 · B5] Root composable: theme, one ViewModel for both tabs, bottom navigation, NavHost.
 *
 * Why the ViewModel is created above the NavHost: both tabs read the same RatesCore, so a refresh on Rates
 * is visible on Convert immediately. Its iOS twin is RootView.swift (TabView).
 */
@Composable
fun App(viewModel: RatesViewModel = koinViewModel()) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF7FD8BE)) else lightColorScheme(primary = Color(0xFF00796B))
    MaterialTheme(colorScheme = colors) {
        val rates by viewModel.state.collectAsStateWithLifecycle()
        val convert by viewModel.convert.collectAsStateWithLifecycle()

        // Every time the app comes back to the foreground: update "… ago" and auto-refresh if older than 1 h.
        // PLATFORM: iOS does the same with .onChange(of: scenePhase) in RootView.swift.
        LifecycleResumeEffect(Unit) {
            viewModel.start()
            onPauseOrDispose { }
        }

        val nav = rememberNavController()
        val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: Routes.RATES
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab(Routes.RATES, current, R.string.tab_rates, { Icon(Icons.AutoMirrored.Filled.List, null) }) {
                        nav.switchTo(Routes.RATES)
                    }
                    Tab(Routes.CONVERT, current, R.string.tab_convert, { Icon(Icons.Filled.Edit, null) }) {
                        nav.switchTo(Routes.CONVERT)
                    }
                }
            },
        ) { padding ->
            // Only the bottom padding: each screen's own TopAppBar already handles the status bar.
            NavHost(nav, startDestination = Routes.RATES, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
                composable(Routes.RATES) {
                    RatesScreen(
                        state = rates,
                        onRefresh = viewModel::refresh,
                        onSelectBase = viewModel::selectBase,
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                }
                composable(Routes.CONVERT) {
                    ConvertScreen(
                        state = convert,
                        onAmountChanged = viewModel::onAmountChanged,
                        onFromSelected = viewModel::onFromSelected,
                        onToSelected = viewModel::onToSelected,
                        onSwap = viewModel::swapCurrencies,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Tab(
    route: String,
    current: String,
    @StringRes label: Int,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    NavigationBarItem(selected = route == current, onClick = onClick, icon = icon, label = { Text(stringResource(label)) })
}

/** Standard bottom-tab navigation: one copy of each tab on the back stack, tab state kept when switching. */
private fun NavController.switchTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
