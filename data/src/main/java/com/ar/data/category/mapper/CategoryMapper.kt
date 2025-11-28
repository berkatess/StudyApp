package com.ar.data.category.mapper

import com.ar.data.category.local.CategoryEntity
import com.ar.data.category.remote.CategoryRemoteDto
import com.ar.domain.category.model.Category

// Domain → Local
fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )

// Local → Domain
fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )

// Domain → Remote
fun Category.toRemoteDto(): CategoryRemoteDto =
    CategoryRemoteDto(
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )

// Remote → Domain
fun CategoryRemoteDto.toDomain(id: String): Category =
    Category(
        id = id,
        name = name,
        imageUrl = imageUrl,
        colorHex = colorHex,
        order = order
    )