package com.ar.data.category.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ar.data.category.local.CategoryLocalDataSource
import com.ar.data.category.mapper.toDomain
import com.ar.data.category.mapper.toRemoteDto
import com.ar.data.category.remote.CategoryRemoteDataSource
import com.ar.domain.auth.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CategorySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val local: CategoryLocalDataSource,
    private val remote: CategoryRemoteDataSource,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = authRepository.currentNonAnonymousUserIdOrNull() ?: return Result.success()

        // 1) DELETE
        local.getPendingDeletes().forEach { entity ->
            try {
                remote.deleteCategory(uid, entity.id)
                local.hardDelete(entity.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        // 2) CREATE/UPDATE (upsert)
        local.getPendingCreates().forEach { entity ->
            try {
                remote.createCategory(uid, entity.id, entity.toDomain().toRemoteDto())
                local.markAsSynced(entity.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        return Result.success()
    }
}
