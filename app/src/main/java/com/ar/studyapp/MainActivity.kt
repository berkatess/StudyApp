package com.ar.studyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ar.domain.settings.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import com.ar.studyapp.note.navigation.NoteNavGraph
import com.ar.studyapp.ui.theme.Theme

/**
 * Application entry point.
 *
 * - @AndroidEntryPoint: Required for Hilt to inject ViewModels and dependencies
 *   into this Activity.
 * - setContent: Initializes the Compose UI hierarchy.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Align status bar / navigation bar with the content (optional)
        enableEdgeToEdge()

        setContent {
            // If you have a custom Compose theme (e.g. StudyAppTheme), use it here.
            // For now, this is wrapped with Theme,
            // you can replace it with your own theme implementation.
            Theme(themeMode = ThemeMode.SYSTEM) {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Call the navigation graph of the Note module.
                    // From inside NoteNavGraph, the following composables will be displayed:
                    // - NoteListRoute
                    // - NoteDetailRoute
                    NoteNavGraph(
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
