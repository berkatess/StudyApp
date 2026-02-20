package com.ar.domain.auth.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.auth.repository.GoogleAuthRepository
import javax.inject.Inject

class SignInWithGoogleIdTokenUseCase @Inject constructor(
    private val googleAuthRepository: GoogleAuthRepository
) : BaseUseCase<String, Unit>() {

    override suspend fun execute(params: String): Result<Unit> {
        val token = params.trim()
        if (token.isEmpty()) {
            return Result.Error("Google ID token cannot be empty")
        }

        googleAuthRepository.signInWithGoogleIdToken(token)
        return Result.Success(Unit)
    }
}
