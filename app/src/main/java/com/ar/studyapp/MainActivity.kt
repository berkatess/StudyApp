package com.ar.studyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.ar.studyapp.note.NoteListViewModel
import com.ar.studyapp.note.NotesScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 🔹 Hilt ViewModel’i Activity seviyesinde alıyoruz
    private val noteListViewModel: NoteListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NotesScreen(viewModel = noteListViewModel)
        }
    }
}
