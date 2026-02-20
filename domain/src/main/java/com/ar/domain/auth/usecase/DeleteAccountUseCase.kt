package com.ar.domain.auth.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.auth.repository.GoogleAuthRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val googleAuthRepository: GoogleAuthRepository
) : BaseUseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit): Result<Unit> {
        googleAuthRepository.deleteAccount()
        return Result.Success(Unit)
    }
}
