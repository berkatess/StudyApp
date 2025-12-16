package com.ar.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ar.data.category.local.CategoryDao
import com.ar.data.note.local.NoteEntity
import com.ar.data.category.local.CategoryEntity
import com.ar.data.note.local.NoteDao


@Database(
    entities = [
        NoteEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
}