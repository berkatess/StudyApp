package com.ar.data.di

import com.ar.core.sync.CategorySyncScheduler
import com.ar.core.sync.NoteSyncScheduler
import com.ar.data.category.sync.CategorySyncSchedulerImpl
import com.ar.data.note.sync.NoteSyncSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindNoteSyncScheduler(
        impl: NoteSyncSchedulerImpl
    ): NoteSyncScheduler

    @Binds
    @Singleton
    abstract fun bindCategorySyncScheduler(
        impl: CategorySyncSchedulerImpl
    ): CategorySyncScheduler
}
