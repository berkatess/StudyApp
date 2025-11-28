package com.ar.domain.note.model

import java.time.Instant

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val categoryId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
