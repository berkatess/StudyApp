package com.ar.domain.auth.repository

/**
 * Domain abstraction for authentication.
 *
 * We keep Firebase details inside the data layer.
 */
interface AuthRepository {
    /**
     * Ensures that there is a signed-in user (anonymous or real).
     *
     * @return the current user's uid.
     */
    suspend fun ensureSignedIn(): String

    /**
     * @return current user's uid if available, null otherwise.
     */
    fun currentUserIdOrNull(): String?
}
