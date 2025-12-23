package com.ar.data.category.mapper

import com.ar.data.category.local.CategoryEntity
import com.ar.data.category.remote.CategoryRemoteDto
import com.ar.data.sync.SyncState
import com.ar.domain.category.model.Category

fun Category.toEntity(
    syncState: SyncState = SyncState.SYNCED,
    isDeleted: Boolean = false
): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order,
        syncState = syncState,
        isDeleted = isDeleted
    )

fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )

fun Category.toRemoteDto(): CategoryRemoteDto =
    CategoryRemoteDto(
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )

fun CategoryRemoteDto.toDomain(id: String): Category =
    Category(
        id = id,
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )
