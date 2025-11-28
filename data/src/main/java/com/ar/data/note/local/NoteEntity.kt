package com.ar.data.note.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val categoryId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)