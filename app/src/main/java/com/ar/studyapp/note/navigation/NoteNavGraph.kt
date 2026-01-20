package com.ar.studyapp.note.navigation

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
import com.ar.studyapp.note.edit.NoteEditRoute
import com.ar.studyapp.note.list.NoteListRoute
import com.ar.studyapp.note.list.NoteListViewModel

object NoteDestinations {
    const val NOTE_GRAPH = "note_graph"

    const val NOTES_LIST = "notes_list"
    const val NOTE_DETAIL = "note_detail"
    const val CATEGORY_MANAGEMENT = "category_management"

    const val NOTE_EDIT = "note_edit"

    const val ARG_NOTE_ID = "noteId"
}

private fun noteEditRoute(noteId: String? = null): String =
    if (noteId.isNullOrBlank()) NoteDestinations.NOTE_EDIT
    else "${NoteDestinations.NOTE_EDIT}?${NoteDestinations.ARG_NOTE_ID}=$noteId"

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
                        navController.navigate("${NoteDestinations.NOTE_DETAIL}/$noteId")
                    },
                    onAddNoteClick = {
                        navController.navigate(noteEditRoute())
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
                route = "${NoteDestinations.NOTE_DETAIL}/{${NoteDestinations.ARG_NOTE_ID}}",
                arguments = listOf(
                    navArgument(NoteDestinations.ARG_NOTE_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString(NoteDestinations.ARG_NOTE_ID)
                    ?: return@composable

                // Graph-scoped shared VM (safe for restore/deeplinks vs getBackStackEntry(NOTES_LIST))
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(NoteDestinations.NOTE_GRAPH)
                }
                val noteListViewModel: NoteListViewModel = hiltViewModel(graphEntry)

                // Collect from SSOT owner (shared VM) with lifecycle awareness
                val categories by noteListViewModel.categories.collectAsStateWithLifecycle()

                NoteDetailRoute(
                    noteId = noteId,
                    noteListViewModel = noteListViewModel,
                    categories = categories,
                    onBackClick = { navController.navigateUp() }
                )
            }

            // Single NOTE_EDIT destination (new note when noteId is blank)
            composable(
                route = "${NoteDestinations.NOTE_EDIT}?${NoteDestinations.ARG_NOTE_ID}={${NoteDestinations.ARG_NOTE_ID}}",
                arguments = listOf(
                    navArgument(NoteDestinations.ARG_NOTE_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getString(NoteDestinations.ARG_NOTE_ID)
                val noteId = rawId?.takeIf { it.isNotBlank() }

                NoteEditRoute(
                    noteId = noteId,
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }
}
