package dev.triplog.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val RECORD = "record"
    const val TRIPS = "trips"
    const val DETAIL = "trip/{id}"
    fun detail(id: Long) = "trip/$id"
}

/**
 * [App 4 · B5] Theme + navigation: Record → Trips → Trip detail.
 * Each route gets its ViewModel from Koin; the TripRecorder behind RecordViewModel is the app-wide single.
 */
@Composable
fun App() {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF80CBC4))
    } else {
        lightColorScheme(primary = Color(0xFF00796B))
    }
    MaterialTheme(colorScheme = colors) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.RECORD) {
            composable(Routes.RECORD) {
                RecordRoute(onOpenTrips = { nav.navigate(Routes.TRIPS) })
            }
            composable(Routes.TRIPS) {
                TripsRoute(onBack = { nav.popBackStack() }, onOpenTrip = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                TripDetailRoute(tripId = entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
        }
    }
}
