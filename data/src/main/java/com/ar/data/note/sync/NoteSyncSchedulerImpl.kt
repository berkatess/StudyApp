package com.ar.data.note.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ar.core.sync.NoteSyncScheduler
import com.ar.domain.auth.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NoteSyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : NoteSyncScheduler {

    override fun schedule() {
        // Local-only mode: do not enqueue sync work if the user is not signed in.
        if (authRepository.currentUserIdOrNull() == null) return

        val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "note_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
