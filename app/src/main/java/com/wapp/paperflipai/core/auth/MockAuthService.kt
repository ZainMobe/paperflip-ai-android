package com.wapp.paperflipai.core.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * In-memory [AuthService] — the Android counterpart of
 * `MockAuthService.swift`. Pretends to talk to a backend with realistic
 * latency and a few baked-in error paths so the failure UI is exercisable.
 *
 * Unlike iOS this one persists through [SessionStore], so a mock session
 * survives process death and the router doesn't bounce you back to sign-in
 * every cold start.
 */
class MockAuthService(
    private val sessionStore: SessionStore? = null,
    initialSession: AuthSession? = null,
) : AuthService {

    private val _session = MutableStateFlow(initialSession)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    /** Simulated latency for every call. */
    var latencyMillis: Long = 700

    /** Set to force the next call to throw a network error. */
    var shouldFailNextCall: Boolean = false

    override suspend fun restoreSession() {
        delay(200)
        if (_session.value == null) {
            _session.value = sessionStore?.load()
        }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        simulate()
        if (!email.contains("@")) throw AuthError.InvalidCredentials
        if (password.length < 8) throw AuthError.InvalidCredentials
        publish(mockSession(email))
    }

    override suspend fun signUpWithEmail(email: String, password: String, fullName: String?) {
        simulate()
        if (!email.contains("@")) throw AuthError.InvalidCredentials
        if (password.length < 8) throw AuthError.WeakPassword
        if (email.equals("taken@example.com", ignoreCase = true)) throw AuthError.EmailAlreadyInUse
        publish(mockSession(email, fullName))
    }

    override suspend fun signInWithGoogle(result: GoogleSignInResult) {
        simulate()
        publish(mockSession(result.email ?: "google.user@gmail.com", result.fullName))
    }

    override suspend fun sendEmailOtp(email: String) {
        simulate()
        if (!email.contains("@")) throw AuthError.InvalidCredentials
    }

    override suspend fun verifyEmailOtp(email: String, token: String) {
        simulate()
        if (token.length != 6) throw AuthError.InvalidCredentials
        publish(mockSession(email))
    }

    override suspend fun sendPasswordReset(email: String) {
        simulate()
        if (!email.contains("@")) throw AuthError.InvalidCredentials
    }

    override suspend fun verifyPasswordResetOtp(email: String, token: String) {
        simulate()
        if (token.length != 6) throw AuthError.InvalidCredentials
        // Recovery verification signs the user in transiently so they can
        // change their password.
        publish(mockSession(email))
    }

    override suspend fun updatePassword(newPassword: String) {
        simulate()
        if (newPassword.length < 8) throw AuthError.WeakPassword
    }

    override suspend fun updateProfileName(fullName: String) {
        simulate()
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) throw AuthError.Unknown("Name can't be empty.")
        _session.value?.let { publish(it.copy(fullName = trimmed)) }
    }

    override suspend fun updateProfileAvatarUrl(url: String?) {
        simulate()
        _session.value?.let { publish(it.copy(avatarUrl = url)) }
    }

    override suspend fun signOut() {
        delay(120)
        _session.value = null
        sessionStore?.clear()
    }

    override suspend fun deleteAccount() {
        simulate()
        _session.value = null
        sessionStore?.clear()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun publish(session: AuthSession) {
        _session.value = session
        sessionStore?.save(session)
    }

    private suspend fun simulate() {
        delay(latencyMillis)
        if (shouldFailNextCall) {
            shouldFailNextCall = false
            throw AuthError.Network("Simulated failure.")
        }
    }

    companion object {
        fun mockSession(
            email: String,
            fullName: String? = null,
            userId: String = UUID.randomUUID().toString(),
        ) = AuthSession(
            userId = userId,
            email = email,
            fullName = fullName,
            avatarUrl = null,
            accessToken = "mock-access-${UUID.randomUUID()}",
            refreshToken = "mock-refresh-${UUID.randomUUID()}",
            expiresAt = System.currentTimeMillis() + 60 * 60 * 1000L,
        )
    }
}
