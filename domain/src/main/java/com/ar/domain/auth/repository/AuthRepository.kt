package com.ar.domain.auth.repository

/**
 * Domain abstraction for authentication.
 *
 * We keep Firebase details inside the data layer.
 */
interface AuthRepository {
    /**
     * Ensures that there is a signed-in **non-anonymous** user.
     *
     * This project supports a "local-only" mode.
     * In local-only mode, Firebase must not be used.
     * Therefore, this method MUST NOT create an anonymous user.
     *
     * @return the current signed-in user's uid.
     * @throws IllegalStateException if no non-anonymous user exists.
     */
    suspend fun ensureSignedIn(): String

    /**
     * @return current user's uid if available, null otherwise.
     */
    fun currentUserIdOrNull(): String?

    /**
     * @return current user's uid only if the user is non-anonymous; null otherwise.
     */
    fun currentNonAnonymousUserIdOrNull(): String?
}