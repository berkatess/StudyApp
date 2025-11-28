package com.ar.data.note.remote

data class NoteRemoteDto(
    val title: String = "",
    val content: String = "",
    val categoryId: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)