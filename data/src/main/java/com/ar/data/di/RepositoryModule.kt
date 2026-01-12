package com.ar.data.di

import com.ar.domain.note.repository.NoteRepository
import com.ar.domain.category.repository.CategoryRepository
import com.ar.data.note.repository.NoteRepositoryImpl
import com.ar.data.category.repository.CategoryRepositoryImpl
import com.ar.domain.auth.repository.AuthRepository
import com.ar.data.auth.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

}
