package com.ar.domain.auth.usecase

import com.ar.domain.auth.repository.AuthRepository
import javax.inject.Inject

class EnsureSignedInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): String = authRepository.ensureSignedIn()
}
