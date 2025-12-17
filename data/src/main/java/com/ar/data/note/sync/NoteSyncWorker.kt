package com.ar.data.note.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ar.data.note.local.NoteLocalDataSource
import com.ar.data.note.mapper.toDomain
import com.ar.data.note.mapper.toRemoteDto
import com.ar.data.note.remote.NoteRemoteDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NoteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val local: NoteLocalDataSource,
    private val remote: NoteRemoteDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1) Pending delete
        val pendingDeletes = local.getPendingDeletes()
        pendingDeletes.forEach { entity ->
            try {
                remote.deleteNote(entity.id)
                // iff firebase deleted delete local
                local.deleteNote(entity.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        // 2) Pending create
        val pendingNotes = local.getPendingNotes()
        pendingNotes.forEach { entity ->
            try {
                remote.createNote(
                    entity.id,
                    entity.toDomain().toRemoteDto()
                )
                local.markAsSynced(entity.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        return Result.success()
    }
}
