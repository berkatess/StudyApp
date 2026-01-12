package com.ar.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {

    fun currentUserIdOrNull(): String? = auth.currentUser?.uid

    /**
     * Ensures we have a signed-in user. Uses anonymous sign-in by default.
     */
    suspend fun ensureSignedIn(): String {
        val existing = auth.currentUser?.uid
        if (existing != null) return existing

        val result = auth.signInAnonymously().await()
        return requireNotNull(result.user?.uid) { "FirebaseAuth returned null user" }
    }
}
