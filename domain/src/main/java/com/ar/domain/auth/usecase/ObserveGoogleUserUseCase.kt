package com.ar.domain.auth.usecase

import com.ar.domain.auth.model.UserInfo
import com.ar.domain.auth.repository.GoogleAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGoogleUserUseCase @Inject constructor(
    private val googleAuthRepository: GoogleAuthRepository
) {
    operator fun invoke(): Flow<UserInfo?> = googleAuthRepository.user
}
