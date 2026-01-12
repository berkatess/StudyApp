package com.ar.data.auth

import com.ar.domain.auth.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun ensureSignedIn(): String = firebaseAuthDataSource.ensureSignedIn()

    override fun currentUserIdOrNull(): String? = firebaseAuthDataSource.currentUserIdOrNull()
}
