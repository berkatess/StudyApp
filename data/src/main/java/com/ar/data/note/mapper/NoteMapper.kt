package com.ar.data.note.mapper

import com.ar.data.note.local.NoteEntity
import com.ar.data.note.remote.NoteRemoteDto
import com.ar.domain.note.model.Note
import com.google.firebase.Timestamp
import java.time.Instant
import java.util.Date

// ---------------------
// Domain → Local (Room)
// ---------------------
fun Note.toEntity(): NoteEntity =
    NoteEntity(
        id = id,
        title = title,
        content = content,
        categoryId = categoryId,
        createdAtMillis = createdAt.toEpochMilli(),
        updatedAtMillis = updatedAt.toEpochMilli()
    )

// ---------------------
// Local → Domain
// ---------------------
fun NoteEntity.toDomain(): Note =
    Note(
        id = id,
        title = title,
        content = content,
        categoryId = categoryId,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtMillis)
    )

// ---------------------
// Domain → Remote (Firestore)
// Instant → Timestamp
// ---------------------
fun Note.toRemoteDto(): NoteRemoteDto =
    NoteRemoteDto(
        title = title,
        content = content,
        categoryId = categoryId,
        createdAtMillis = Timestamp(Date.from(createdAt)),
        updatedAtMillis = Timestamp(Date.from(updatedAt))
    )

// ---------------------
// Remote → Domain (Firestore → Domain)
// Timestamp → Instant
// ---------------------
fun NoteRemoteDto.toDomain(id: String): Note {
    val created = createdAtMillis?.toDate()?.toInstant() ?: Instant.EPOCH
    val updated = updatedAtMillis?.toDate()?.toInstant() ?: created

    return Note(
        id = id,
        title = title,
        content = content,
        categoryId = categoryId,
        createdAt = created,
        updatedAt = updated
    )
}
