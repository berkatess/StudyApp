package com.ar.domain.auth.repository

import com.ar.domain.auth.model.UserInfo
import kotlinx.coroutines.flow.Flow

interface GoogleAuthRepository {
    val user: Flow<UserInfo?>
    suspend fun signInWithGoogleIdToken(idToken: String)
    suspend fun signOut()
    suspend fun deleteAccount()
}
