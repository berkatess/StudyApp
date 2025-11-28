package com.ar.data.category.remote

data class CategoryRemoteDto(
    val name: String = "",
    val imageUrl: String? = null,
    val colorHex: String? = null,
    val order: Int = 0
)