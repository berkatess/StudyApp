package com.ar.data.note.sync

import android.content.Context
import androidx.work.*
import com.ar.core.sync.NoteSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NoteSyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NoteSyncScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "note_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
