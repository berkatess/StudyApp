package com.ar.domain.auth.usecase

import com.ar.core.result.Result
import com.ar.core.sync.CategorySyncScheduler
import com.ar.core.sync.NoteSyncScheduler
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.auth.repository.GoogleAuthRepository
import javax.inject.Inject

class SignInWithGoogleIdTokenUseCase @Inject constructor(
    private val googleAuthRepository: GoogleAuthRepository,
    private val noteSyncScheduler: NoteSyncScheduler,
    private val categorySyncScheduler: CategorySyncScheduler
) : BaseUseCase<String, Unit>() {

    override suspend fun execute(params: String): Result<Unit> {
        val token = params.trim()
        if (token.isEmpty()) {
            return Result.Error("Google ID token cannot be empty")
        }

        googleAuthRepository.signInWithGoogleIdToken(token)

        // After a successful sign-in, push pending local changes to remote.
        // Schedulers are gated by auth state, so they won't enqueue work in local-only mode.
        noteSyncScheduler.schedule()
        categorySyncScheduler.schedule()

        return Result.Success(Unit)
    }
}
