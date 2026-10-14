package dev.devicepulse.android.ui

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
import dev.devicepulse.model.SensorKind
import org.koin.compose.viewmodel.koinViewModel

private val Brand = Color(0xFF3D5AFE)

private object Routes {
    const val DASHBOARD = "dashboard"
    const val DETAIL = "detail/{sensor}"
    const val HISTORY = "history"
    fun detail(sensor: SensorKind) = "detail/${sensor.name}"
}

@Composable
fun App() {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF8C9EFF)) else lightColorScheme(primary = Brand)
    MaterialTheme(colorScheme = colors) {
        // One ViewModel for the whole app, created above the NavHost so every screen shares the same live readings.
        val viewModel: PulseViewModel = koinViewModel()
        val monitor = viewModel.monitor
        val state by monitor.state.collectAsStateWithLifecycle()
        val snapshots by monitor.snapshots.collectAsStateWithLifecycle()

        val permission = rememberLocationPermission()
        LaunchedEffect(Unit) { if (!permission.granted) permission.request() }
        LaunchedEffect(permission.granted) { if (permission.granted) monitor.refreshLocation() }

        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    state = state,
                    onOpenDetail = { nav.navigate(Routes.detail(it)) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                    onMeasureLatency = monitor::measureLatency,
                    onSave = monitor::saveSnapshot,
                    onGrantLocation = permission::request,
                )
            }
            composable(Routes.DETAIL) { entry ->
                DetailScreen(
                    sensor = SensorKind.valueOf(entry.arguments?.getString("sensor") ?: SensorKind.Battery.name),
                    state = state,
                    onBack = { nav.popBackStack() },
                    onMeasureLatency = monitor::measureLatency,
                    onGrantLocation = permission::request,
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    snapshots = snapshots,
                    onBack = { nav.popBackStack() },
                    onClear = monitor::clearHistory,
                )
            }
        }
    }
}
