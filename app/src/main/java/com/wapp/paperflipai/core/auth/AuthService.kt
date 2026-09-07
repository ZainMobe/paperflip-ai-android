package com.wapp.paperflipai.core.auth

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Authentication abstraction — the Android counterpart of
 * `AuthService.swift`. The whole app talks to this interface, never to
 * Supabase or Google directly, so implementations swap without touching a
 * single screen.
 *
 * Ships with [MockAuthService]; a `SupabaseAuthService` drops in later.
 */
@Serializable
data class AuthSession(
    val userId: String,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAt

    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    val initials: String
        get() = displayName.trim().split(" ", limit = 2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

/**
 * Result handed back by Google sign-in once the user has picked an account.
 * `idToken` plus the un-hashed `rawNonce` are exchanged with Supabase's
 * `signInWithIdToken(provider = google)`.
 */
data class GoogleSignInResult(
    val idToken: String,
    val accessToken: String? = null,
    val rawNonce: String? = null,
    val userId: String,
    val fullName: String? = null,
    val email: String? = null,
)

sealed class AuthError(message: String) : Exception(message) {
    data object InvalidCredentials : AuthError("Email or password is incorrect.")
    data object EmailAlreadyInUse : AuthError("An account with that email already exists.")
    data object WeakPassword : AuthError("Password should be at least 8 characters.")
    data object UserCancelled : AuthError("Sign-in cancelled.")
    class Network(detail: String) : AuthError("Network error: $detail")
    class Unknown(detail: String) : AuthError(detail)
}

interface AuthService {

    /** Current session, null when signed out. Drives the root router. */
    val session: StateFlow<AuthSession?>

    /** Restores any persisted session. Called once on cold-start. */
    suspend fun restoreSession()

    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String, fullName: String?)
    suspend fun signInWithGoogle(result: GoogleSignInResult)

    // Passwordless OTP sign-in.
    suspend fun sendEmailOtp(email: String)
    suspend fun verifyEmailOtp(email: String, token: String)

    // Forgot-password / recovery flow.
    suspend fun sendPasswordReset(email: String)
    suspend fun verifyPasswordResetOtp(email: String, token: String)
    suspend fun updatePassword(newPassword: String)

    // Profile mutations.
    suspend fun updateProfileName(fullName: String)
    suspend fun updateProfileAvatarUrl(url: String?)

    suspend fun signOut()

    /**
     * Permanently deletes the signed-in user's account and all their data.
     * Required by Play Store data-deletion policy (and App Store 5.1.1(v)).
     */
    suspend fun deleteAccount()
}
