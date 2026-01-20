package com.ar.studyapp.note.detail

sealed interface NoteDetailEvent {
    data class TitleChanged(val value: String) : NoteDetailEvent
    data class ContentChanged(val value: String) : NoteDetailEvent
    data class CategoryChanged(val categoryId: String?) : NoteDetailEvent
}