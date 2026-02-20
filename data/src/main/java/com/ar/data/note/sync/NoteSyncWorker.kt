package com.ar.data.note.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ar.data.note.local.NoteLocalDataSource
import com.ar.data.note.mapper.toDomain
import com.ar.data.note.mapper.toRemoteDto
import com.ar.data.note.remote.NoteRemoteDataSource
import com.ar.domain.auth.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NoteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val local: NoteLocalDataSource,
    private val remote: NoteRemoteDataSource,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("NoteSyncWorker", "WORKER STARTED")

        val uid = runCatching { authRepository.ensureSignedIn() }
            .getOrElse { return Result.retry() }

        val pendingDeletes = local.getPendingDeletes()
        for (entity in pendingDeletes) {
            try {
                remote.deleteNote(uid, entity.id)
                local.hardDelete(entity.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        val pendingNotes = local.getPendingNotes()
        for (entity in pendingNotes) {
            try {
                val dto = entity.toDomain().toRemoteDto()
                remote.updateNote(uid, entity.id, dto)
                local.markAsSynced(entity.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        return Result.success()
    }
}
