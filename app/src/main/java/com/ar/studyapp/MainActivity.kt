package com.ar.studyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.ar.studyapp.note.navigation.NoteNavGraph
import com.ar.studyapp.ui.theme.Theme

/**
 * Uygulamanın giriş noktası.
 * - @AndroidEntryPoint: Hilt'in bu Activity'ye ViewModel ve Dependency enjekte edebilmesi için gerekli.
 * - setContent: Compose UI hiyerarşisini başlatır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar / navigation bar'ı içerikle hizalamak için (isteğe bağlı)
        enableEdgeToEdge()

        setContent {
            // Eğer kendi Compose temanız varsa (ör: StudyAppTheme) onu kullan.
            // Burada basitçe MaterialTheme üzerinden gidiyorum, sen kendi temana wrap edebilirsin.
            Theme {   // yoksa aşağıda basit versiyonunu da veriyorum
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Bütün Note modülünün navigation grafiğini çağırıyoruz.
                    // Artık NoteNavGraph içinden:
                    // - NoteListRoute
                    // - NoteDetailRoute
                    // composable'ları çalışacak.
                    NoteNavGraph(
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
