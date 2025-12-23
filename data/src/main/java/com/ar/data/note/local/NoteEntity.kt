package com.ar.data.note.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ar.data.sync.SyncState

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val categoryId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncState: SyncState = SyncState.PENDING,
    val isDeleted: Boolean = false
)

