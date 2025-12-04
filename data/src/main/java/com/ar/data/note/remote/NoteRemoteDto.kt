package com.ar.data.note.remote

import com.google.firebase.Timestamp

data class NoteRemoteDto(
    val title: String = "",
    val content: String = "",
    val categoryId: String? = null,
    val createdAtMillis: Timestamp? = null,
    val updatedAtMillis: Timestamp? = null
)