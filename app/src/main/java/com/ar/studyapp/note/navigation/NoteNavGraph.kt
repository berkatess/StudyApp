package com.ar.studyapp.note.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ar.studyapp.category.CategoryManagementRoute
import com.ar.studyapp.note.detail.NoteDetailRoute
import com.ar.studyapp.note.list.NoteListRoute

/**
 * Ekran route sabitleri – stringleri ortada topluyoruz.
 */
object NoteDestinations {
    const val NOTES_LIST = "notes_list"
    const val NOTE_DETAIL = "note_detail"
    const val NOTE_ID_ARG = "noteId"
    const val CATEGORY_MANAGEMENT = "category_management"
}

/**
 * Not modülü için navigation graph.
 * Uygulamanın ana composable'ında setContent içinde çağırabilirsin:
 *
 * setContent {
 *     StudyAppTheme {
 *         NoteNavGraph()
 *     }
 * }
 */
@Composable
fun NoteNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NoteDestinations.NOTES_LIST,
        modifier = modifier
    ) {
        // 📌 NoteListScreen
        composable(route = NoteDestinations.NOTES_LIST) {
            NoteListRoute(
                onNoteClick = { noteId ->
                    // item'a tıklandığında detay ekranına noteId ile geç
                    navController.navigate("${NoteDestinations.NOTE_DETAIL}/$noteId")
                },
                onManageCategoriesClick = {
                    // kategori yönetimi ekranına git
                    navController.navigate(NoteDestinations.CATEGORY_MANAGEMENT)
                }
            )
        }

        // 📌 CategoryManagementScreen
        composable(route = NoteDestinations.CATEGORY_MANAGEMENT) {
            CategoryManagementRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        // 📌 NoteDetailScreen
        composable(
            route = "${NoteDestinations.NOTE_DETAIL}/{${NoteDestinations.NOTE_ID_ARG}}",
            arguments = listOf(
                navArgument(NoteDestinations.NOTE_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString(NoteDestinations.NOTE_ID_ARG)
                ?: return@composable

            NoteDetailRoute(
                noteId = noteId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
