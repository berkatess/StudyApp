package com.ar.data.note.mapper

import com.ar.data.note.local.NoteEntity
import com.ar.data.note.remote.NoteRemoteDto
import com.ar.domain.note.model.Note
import java.time.Instant

// Domain → Local
fun Note.toEntity(): NoteEntity =
    NoteEntity(
        id = id,
        title = title,
        content = content,
        categoryId = categoryId,
        createdAtMillis = createdAt.toEpochMilli(),
        updatedAtMillis = updatedAt.toEpochMilli()
    )

// Local → Domain
fun NoteEntity.toDomain(): Note =
    Note(
        id = id,
        title = title,
        content = content,
        categoryId = categoryId,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtMillis)
    )

// Domain → Remote
fun Note.toRemoteDto(): NoteRemoteDto =
    NoteRemoteDto(
        title = title,
        content = content,
        categoryId = categoryId,
        createdAtMillis = createdAt.toEpochMilli(),
        updatedAtMillis = updatedAt.toEpochMilli()
    )

// Remote → Domain
fun NoteRemoteDto.toDomain(id: String): Note =
    Note(
        id = id,
        title = title,
        content = content,
        categoryId = categoryId,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtMillis)
    )