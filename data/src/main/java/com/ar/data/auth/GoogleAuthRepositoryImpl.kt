package com.ar.data.auth

import com.ar.core.sync.CategorySyncScheduler
import com.ar.core.sync.NoteSyncScheduler
import com.ar.domain.auth.model.UserInfo
import com.ar.domain.auth.repository.GoogleAuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : GoogleAuthRepository {

    override val user: Flow<UserInfo?> = callbackFlow {
        // Emit immediately for the current session.
        trySend(firebaseAuth.currentUser.toUserInfoOrNull())

        // IdTokenListener is triggered when the user's token changes.
        val listener = FirebaseAuth.IdTokenListener { auth ->
            trySend(auth.currentUser.toUserInfoOrNull())
        }

        firebaseAuth.addIdTokenListener(listener)
        awaitClose { firebaseAuth.removeIdTokenListener(listener) }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        require(idToken.isNotBlank()) { "Google ID token must not be blank" }

        // In this project, Firebase is only used when the user explicitly signs in with Google.
        // Therefore we do not keep an anonymous session and we do not link accounts.
        // A normal sign-in is enough and avoids collision errors when re-signing in.
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()

        // Refresh user profile/token so observers receive updated fields (email/isAnonymous).
        firebaseAuth.currentUser?.reload()?.await()
        firebaseAuth.currentUser?.getIdToken(true)?.await()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser ?: return
        user.delete().await()
    }
}

private fun FirebaseUser?.toUserInfoOrNull(): UserInfo? {
    val user = this ?: return null

    // Anonymous Firebase users are treated as "signed out" in this app.
    if (user.isAnonymous) return null

    // Email can be null in some edge cases; fallback to provider data.
    val emailValue = user.email
        ?: user.providerData.firstOrNull { !it.email.isNullOrBlank() }?.email

    return UserInfo(
        uid = user.uid,
        email = emailValue,
        isAnonymous = user.isAnonymous
    )
}
