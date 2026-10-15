package dev.pocketnotes.android.ui

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
import org.koin.compose.viewmodel.koinViewModel

private val Brand = Color(0xFFF2A100)

/** [App 2 · B5] Route strings in one place. `edit/0` means "new note". */
private object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{id}"
    fun edit(id: Long) = "edit/$id"
}

/**
 * [App 2 · B5] Theme + Navigation Compose: `list` → `edit/{id}`.
 *
 * Why one ViewModel above the NavHost: both screens must talk to the SAME NotesCore. A koinViewModel() inside each
 * destination would create one core per screen, and the list would not see the editor's "deleted, undo?" state.
 */
@Composable
fun App() {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFFFC94D)) else lightColorScheme(primary = Brand)
    MaterialTheme(colorScheme = colors) {
        val viewModel: NotesViewModel = koinViewModel()
        val nav = rememberNavController()
        // popBackStack(LIST) instead of popBackStack(): a double tap on Back must not pop the list too (blank screen).
        val backToList = { nav.popBackStack(Routes.LIST, inclusive = false); Unit }

        NavHost(navController = nav, startDestination = Routes.LIST) {
            composable(Routes.LIST) {
                NoteListRoute(
                    viewModel = viewModel,
                    onOpen = { id -> nav.navigate(Routes.edit(id)) },
                    onNew = { nav.navigate(Routes.edit(0)) },
                )
            }
            composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                NoteEditRoute(
                    viewModel = viewModel,
                    id = entry.arguments?.getLong("id") ?: 0L,
                    onDone = backToList,
                )
            }
        }
    }
}
