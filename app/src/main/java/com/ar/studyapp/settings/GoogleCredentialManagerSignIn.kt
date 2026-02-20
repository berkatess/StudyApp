package com.ar.studyapp.settings

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleCredentialManagerSignIn(
    private val credentialManager: CredentialManager,
    private val webClientId: String
) {

    suspend fun getIdToken(activity: Activity): String {
        val clientId = webClientId.trim()
        if (clientId.isBlank()) return ""

        // Best practice: first try previously authorized accounts. If none exist, fall back to all accounts.
        val tokenFromAuthorized = try {
            getIdTokenInternal(activity, clientId, filterByAuthorizedAccounts = true)
        } catch (_: NoCredentialException) {
            ""
        }

        if (tokenFromAuthorized.isNotBlank()) return tokenFromAuthorized

        return try {
            getIdTokenInternal(activity, clientId, filterByAuthorizedAccounts = false)
        } catch (_: GetCredentialCancellationException) {
            // The user dismissed the bottom sheet or canceled the flow.
            ""
        } catch (e: GetCredentialCustomException) {
            // Custom error returned from the framework/provider. Logging the type helps troubleshooting.
            android.util.Log.e("AUTH", "CredentialManager custom error type=${e.type}", e)
            ""
        } catch (e: GetCredentialException) {
            // Surface the error in logs; caller can show UI message if needed.
            android.util.Log.e("AUTH", "CredentialManager getCredential failed", e)
            ""
        }
    }

    private suspend fun getIdTokenInternal(
        activity: Activity,
        clientId: String,
        filterByAuthorizedAccounts: Boolean
    ): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activity
        )

        val credential = result.credential

        // Credential Manager returns a CustomCredential for Google ID tokens.
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }

        // Not a Google ID token credential.
        return ""
    }
}
