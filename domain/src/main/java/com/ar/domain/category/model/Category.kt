package com.ar.domain.category.model

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val colorHex: String? = null,
    val order: Int = 0
)
