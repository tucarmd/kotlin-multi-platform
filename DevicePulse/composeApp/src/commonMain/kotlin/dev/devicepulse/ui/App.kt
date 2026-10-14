package dev.devicepulse.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.devicepulse.sensor.rememberLocationPermission
import org.koin.compose.viewmodel.koinViewModel

private val Brand = Color(0xFF3D5AFE)

@Composable
fun App() {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF8C9EFF)) else lightColorScheme(primary = Brand)
    MaterialTheme(colorScheme = colors) {
        // One ViewModel for the whole app, created above the NavHost so every screen shares the same live readings.
        val viewModel: DashboardViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()

        val permission = rememberLocationPermission()
        LaunchedEffect(Unit) { if (!permission.granted && permission.canPrompt) permission.request() }
        LaunchedEffect(permission.granted) { if (permission.granted) viewModel.refreshLocation() }

        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = DashboardRoute) {
            composable<DashboardRoute> {
                DashboardScreen(
                    state = state,
                    onOpenDetail = { nav.navigate(DetailRoute(it)) },
                    onOpenHistory = { nav.navigate(HistoryRoute) },
                    onMeasureLatency = viewModel::measureLatency,
                    onSave = viewModel::saveSnapshot,
                    onGrantLocation = permission::request,
                )
            }
            composable<DetailRoute> { entry ->
                DetailScreen(
                    sensor = entry.toRoute<DetailRoute>().sensor,
                    state = state,
                    onBack = { nav.popBackStack() },
                    onMeasureLatency = viewModel::measureLatency,
                    onGrantLocation = permission::request,
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    snapshots = snapshots,
                    onBack = { nav.popBackStack() },
                    onClear = viewModel::clearHistory,
                )
            }
        }
    }
}
