package com.ar.studyapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.ar.core.coroutines.DispatcherProvider
import com.ar.domain.auth.usecase.EnsureSignedInUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch



@HiltAndroidApp
class StudyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var dispatchers: DispatcherProvider
    @Inject lateinit var ensureSignedInUseCase: EnsureSignedInUseCase


    override fun onCreate() {
        super.onCreate()
        android.util.Log.e("StudyApp", "workerFactory = $workerFactory")

        // Firestore security rules commonly require an authenticated user.
        // We sign in anonymously at app start to avoid PERMISSION_DENIED.
        CoroutineScope(SupervisorJob() + dispatchers.io).launch {
            runCatching { ensureSignedInUseCase() }
                .onFailure { e -> android.util.Log.e("StudyApp", "Auth init failed", e) }
        }

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

