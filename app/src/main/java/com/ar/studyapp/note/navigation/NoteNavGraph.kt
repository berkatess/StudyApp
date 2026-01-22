package com.ar.studyapp.note.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ar.studyapp.category.CategoryManagementRoute
import com.ar.studyapp.note.detail.NoteDetailRoute
import com.ar.studyapp.note.list.NoteListRoute
import com.ar.studyapp.note.list.NoteListViewModel

object NoteDestinations {
    const val NOTE_GRAPH = "note_graph"

    const val NOTES_LIST = "notes_list"
    const val NOTE_DETAIL = "note_detail"
    const val NOTE_EDIT = "note_edit"
    const val CATEGORY_MANAGEMENT = "category_management"

    const val ARG_NOTE_ID = "noteId"
    const val ARG_MODE = "mode"

    const val MODE_CREATE = "create"
    const val MODE_EDIT = "edit"
}

/**
 * Builds a single detail route with query args.
 *
 * Examples:
 * - note_detail?mode=create&noteId=
 * - note_detail?mode=edit&noteId=<id>
 */
private fun noteDetailRoute(mode: String, noteId: String?): String {
    val safeMode = Uri.encode(mode)
    val safeId = Uri.encode(noteId.orEmpty())
    return "${NoteDestinations.NOTE_DETAIL}?${NoteDestinations.ARG_MODE}=$safeMode&${NoteDestinations.ARG_NOTE_ID}=$safeId"
}

@Composable
fun NoteNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NoteDestinations.NOTE_GRAPH,
        modifier = modifier
    ) {
        navigation(
            route = NoteDestinations.NOTE_GRAPH,
            startDestination = NoteDestinations.NOTES_LIST
        ) {
            composable(route = NoteDestinations.NOTES_LIST) {
                NoteListRoute(
                    onNoteClick = { noteId ->
                        // Open existing note detail.
                        navController.navigate(noteDetailRoute(NoteDestinations.MODE_EDIT, noteId))
                    },
                    onAddNoteClick = {
                        // Open create mode instantly (no waiting list state).
                        navController.navigate(noteDetailRoute(NoteDestinations.MODE_CREATE, null))
                    },
                    onManageCategoriesClick = {
                        navController.navigate(NoteDestinations.CATEGORY_MANAGEMENT)
                    }
                )
            }

            composable(route = NoteDestinations.CATEGORY_MANAGEMENT) {
                CategoryManagementRoute(
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable(
                route = "${NoteDestinations.NOTE_DETAIL}?${NoteDestinations.ARG_MODE}={${NoteDestinations.ARG_MODE}}&${NoteDestinations.ARG_NOTE_ID}={${NoteDestinations.ARG_NOTE_ID}}",
                arguments = listOf(
                    navArgument(NoteDestinations.ARG_MODE) {
                        type = NavType.StringType
                        defaultValue = NoteDestinations.MODE_EDIT
                    },
                    navArgument(NoteDestinations.ARG_NOTE_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString(NoteDestinations.ARG_MODE)
                    ?: NoteDestinations.MODE_EDIT

                val rawId = backStackEntry.arguments?.getString(NoteDestinations.ARG_NOTE_ID)
                val noteId = rawId?.takeIf { it.isNotBlank() }

                // Use a graph-scoped ViewModel so list & detail share the same instance/state.
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(NoteDestinations.NOTE_GRAPH)
                }
                val noteListViewModel: NoteListViewModel = hiltViewModel(graphEntry)
                val categories by noteListViewModel.categories.collectAsStateWithLifecycle()

                NoteDetailRoute(
                    mode = mode,
                    noteId = noteId,
                    noteListViewModel = noteListViewModel,
                    categories = categories,
                    onBackClick = { navController.navigateUp() },
                    onCreated = { createdId ->
                        // After creation, switch the screen into edit mode with the real id.
                        // This keeps back stack clean and makes the route represent the persisted entity.
                        val createRoute = noteDetailRoute(NoteDestinations.MODE_CREATE, null)
                        navController.navigate(noteDetailRoute(NoteDestinations.MODE_EDIT, createdId)) {
                            popUpTo(createRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
