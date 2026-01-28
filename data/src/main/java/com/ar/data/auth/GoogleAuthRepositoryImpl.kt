package com.ar.data.auth

import com.ar.domain.auth.model.UserInfo
import com.ar.domain.auth.repository.GoogleAuthRepository
import com.google.firebase.auth.FirebaseAuth
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
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val u = auth.currentUser
            trySend(
                u?.let {
                    UserInfo(
                        uid = it.uid,
                        email = it.email,
                        isAnonymous = it.isAnonymous
                    )
                }
            )
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser ?: return
        user.delete().await()
    }
}
