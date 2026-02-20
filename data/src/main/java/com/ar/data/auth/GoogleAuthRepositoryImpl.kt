package com.ar.data.auth

import com.ar.domain.auth.model.UserInfo
import com.ar.domain.auth.repository.GoogleAuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
        trySend(firebaseAuth.currentUser?.toUserInfo())

        // IdTokenListener is triggered when the user's token changes.
        // Linking a Google credential to an anonymous user refreshes the token,
        // so this listener is reliable for the "upgrade anonymous -> Google" flow.
        val listener = FirebaseAuth.IdTokenListener { auth ->
            trySend(auth.currentUser?.toUserInfo())
        }

        firebaseAuth.addIdTokenListener(listener)
        awaitClose { firebaseAuth.removeIdTokenListener(listener) }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = firebaseAuth.currentUser

        // If the current user is anonymous, link the Google credential.
        // This keeps the same uid so device-only data remains accessible.
        if (current != null && current.isAnonymous) {
            try {
                current.linkWithCredential(credential).await()
            } catch (e: FirebaseAuthUserCollisionException) {
                // The Google account is already linked to a different Firebase user.
                throw e
            }
        } else {
            // Non-anonymous case: normal sign-in.
            firebaseAuth.signInWithCredential(credential).await()
        }

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

private fun FirebaseUser.toUserInfo(): UserInfo {
    // Email can be null right after linking; fallback to provider data.
    val emailValue = email
        ?: providerData.firstOrNull { !it.email.isNullOrBlank() }?.email

    return UserInfo(
        uid = uid,
        email = emailValue,
        isAnonymous = isAnonymous
    )
}
