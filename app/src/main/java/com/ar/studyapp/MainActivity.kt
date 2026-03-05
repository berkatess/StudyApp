package com.ar.studyapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.domain.settings.model.ThemeMode
import com.ar.studyapp.note.navigation.NoteNavGraph
import com.ar.studyapp.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Application entry point.
 *
 * Language is resolved by Android automatically from the device locale
 * using string resources (values/, values-tr/, ...). No in-app language override.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: AppViewModel = hiltViewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()

            val isDark = themeMode == ThemeMode.DARK

//            WindowCompat.setDecorFitsSystemWindows(window, false)
//
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                window.isNavigationBarContrastEnforced = false
//            }


            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightNavigationBars = !isDark
            }

            Theme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NoteNavGraph()
                }
            }
        }
    }
}