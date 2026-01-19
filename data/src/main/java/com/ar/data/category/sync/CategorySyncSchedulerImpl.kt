package com.ar.data.category.sync

import android.content.Context
import androidx.work.*
import com.ar.core.sync.CategorySyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CategorySyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CategorySyncScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<CategorySyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "category_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
