package com.ar.data.category.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ar.data.sync.SyncState

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val colorHex: String?,
    val order: Int,
    val syncState: SyncState = SyncState.PENDING,
    val isDeleted: Boolean = false
)