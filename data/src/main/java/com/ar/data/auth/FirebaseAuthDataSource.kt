package com.ar.data.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {

    fun currentUserIdOrNull(): String? = auth.currentUser?.uid

    fun currentNonAnonymousUserIdOrNull(): String? {
        val user = auth.currentUser ?: return null
        return if (user.isAnonymous) null else user.uid
    }

    /**
     * Ensures we have a signed-in **non-anonymous** user.
     *
     * Important: This project supports a local-only mode.
     * We must NOT create an anonymous Firebase user implicitly.
     */
    suspend fun ensureSignedIn(): String {
        val uid = currentNonAnonymousUserIdOrNull()
        if (uid != null) return uid
        throw IllegalStateException("User is not signed in")
    }
}