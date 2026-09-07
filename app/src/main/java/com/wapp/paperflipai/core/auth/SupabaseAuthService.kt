package com.wapp.paperflipai.core.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Live [AuthService] against Supabase GoTrue — the Android counterpart of
 * `SupabaseAuthService.swift`.
 *
 * Talks to `/auth/v1` directly rather than through [SupabaseClient], and that
 * is deliberate: the client resolves its bearer token *from this class*, so
 * routing auth through it would be a cycle. The two calls that touch
 * `profiles` are done here for the same reason.
 *
 * Token lifetime is this class's job. [validAccessToken] is what the REST
 * client calls before every request; it refreshes anything inside
 * [REFRESH_WINDOW_MS] of expiry, under a mutex so a burst of parallel
 * requests produces one refresh rather than a thundering herd — each of which
 * would rotate the refresh token and invalidate the others.
 */
class SupabaseAuthService(
    baseUrl: String,
    private val anonKey: String,
    private val sessionStore: SessionStore,
) : AuthService {

    private val base = baseUrl.trimEnd('/')
    private val authUrl = "$base/auth/v1"
    private val restUrl = "$base/rest/v1"
    private val functionsUrl = "$base/functions/v1"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMedia = "application/json".toMediaType()
    private val refreshGate = Mutex()

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    // ── Session lifecycle ─────────────────────────────────────────────

    override suspend fun restoreSession() {
        val stored = sessionStore.load() ?: return
        _session.value = stored
        // Don't block cold-start on the network: an expired token is
        // refreshed lazily by the first request that needs one.
        if (stored.isExpired) runCatching { validAccessToken() }
    }

    /**
     * The bearer token for outgoing requests, refreshed if it is expired or
     * about to be. Returns null when signed out, which makes the REST client
     * fall back to the anon key.
     */
    suspend fun validAccessToken(): String? {
        val current = _session.value ?: return null
        if (System.currentTimeMillis() < current.expiresAt - REFRESH_WINDOW_MS) {
            return current.accessToken
        }
        return refreshGate.withLock {
            // Re-check inside the lock: another caller may have refreshed
            // while this one was waiting.
            val latest = _session.value ?: return@withLock null
            if (System.currentTimeMillis() < latest.expiresAt - REFRESH_WINDOW_MS) {
                return@withLock latest.accessToken
            }
            runCatching {
                val body = post(
                    "$authUrl/token?grant_type=refresh_token",
                    buildJsonObject { put("refresh_token", latest.refreshToken) }.toString(),
                )
                store(parseSession(body, fallbackEmail = latest.email)).accessToken
            }.getOrElse {
                // A rejected refresh token means the session is gone for
                // good — sign out locally so the UI routes to the welcome
                // screen instead of retrying forever.
                clearLocal()
                null
            }
        }
    }

    // ── Email + password ──────────────────────────────────────────────

    override suspend fun signInWithEmail(email: String, password: String) {
        val body = post(
            "$authUrl/token?grant_type=password",
            buildJsonObject {
                put("email", email.trim().lowercase())
                put("password", password)
            }.toString(),
        )
        store(parseSession(body, fallbackEmail = email))
    }

    override suspend fun signUpWithEmail(email: String, password: String, fullName: String?) {
        val body = post(
            "$authUrl/signup",
            buildJsonObject {
                put("email", email.trim().lowercase())
                put("password", password)
                if (!fullName.isNullOrBlank()) {
                    putJsonObject("data") { put("full_name", fullName.trim()) }
                }
            }.toString(),
        )
        // With email confirmation on, signup returns a user but no tokens.
        // That is not a failure: the UI moves to the OTP screen, and
        // verifyEmailOtp completes the session.
        runCatching { parseSession(body, fallbackEmail = email) }.getOrNull()?.let { store(it) }
    }

    override suspend fun signInWithGoogle(result: GoogleSignInResult) {
        val body = post(
            "$authUrl/token?grant_type=id_token",
            buildJsonObject {
                put("provider", "google")
                put("id_token", result.idToken)
                result.rawNonce?.let { put("nonce", it) }
            }.toString(),
        )
        store(parseSession(body, fallbackEmail = result.email.orEmpty()))
    }

    // ── Passwordless OTP ──────────────────────────────────────────────

    override suspend fun sendEmailOtp(email: String) {
        post(
            "$authUrl/otp",
            buildJsonObject {
                put("email", email.trim().lowercase())
                put("create_user", true)
            }.toString(),
        )
    }

    override suspend fun verifyEmailOtp(email: String, token: String) {
        val body = post(
            "$authUrl/verify",
            buildJsonObject {
                put("email", email.trim().lowercase())
                put("token", token.trim())
                put("type", "email")
            }.toString(),
        )
        store(parseSession(body, fallbackEmail = email))
    }

    // ── Recovery ──────────────────────────────────────────────────────

    override suspend fun sendPasswordReset(email: String) {
        post(
            "$authUrl/recover",
            buildJsonObject { put("email", email.trim().lowercase()) }.toString(),
        )
    }

    override suspend fun verifyPasswordResetOtp(email: String, token: String) {
        val body = post(
            "$authUrl/verify",
            buildJsonObject {
                put("email", email.trim().lowercase())
                put("token", token.trim())
                put("type", "recovery")
            }.toString(),
        )
        // Recovery hands back a real session; the user is signed in and can
        // now set a new password.
        store(parseSession(body, fallbackEmail = email))
    }

    override suspend fun updatePassword(newPassword: String) {
        val body = send(
            "PUT", "$authUrl/user",
            buildJsonObject { put("password", newPassword) }.toString(),
            authorized = true,
        )
        // GoTrue returns the user, not a session — keep the one we have and
        // just refresh the profile fields off it.
        mergeUserInto(body)
    }

    // ── Profile ───────────────────────────────────────────────────────

    override suspend fun updateProfileName(fullName: String) {
        val current = _session.value ?: throw AuthError.Unknown("You need to sign in first.")
        send(
            "PATCH", "$restUrl/profiles?id=eq.${current.userId}",
            buildJsonObject { put("full_name", fullName.trim()) }.toString(),
            authorized = true,
        )
        store(current.copy(fullName = fullName.trim()))
    }

    override suspend fun updateProfileAvatarUrl(url: String?) {
        val current = _session.value ?: throw AuthError.Unknown("You need to sign in first.")
        send(
            "PATCH", "$restUrl/profiles?id=eq.${current.userId}",
            buildJsonObject {
                if (url == null) put("avatar_url", null as String?) else put("avatar_url", url)
            }.toString(),
            authorized = true,
        )
        store(current.copy(avatarUrl = url))
    }

    // ── Sign out / delete ─────────────────────────────────────────────

    override suspend fun signOut() {
        // Best effort: a failed server-side logout must not strand the user
        // in a signed-in UI they can't leave.
        runCatching { send("POST", "$authUrl/logout", "{}", authorized = true) }
        clearLocal()
    }

    override suspend fun deleteAccount() {
        // The Edge Function owns the cascade — auth user, profile, decks,
        // storage objects — because none of it is deletable with the user's
        // own token.
        send("POST", "$functionsUrl/delete-account", "{}", authorized = true)
        clearLocal()
    }

    // ── Plumbing ──────────────────────────────────────────────────────

    private fun store(next: AuthSession): AuthSession {
        _session.value = next
        sessionStore.save(next)
        return next
    }

    private fun clearLocal() {
        _session.value = null
        sessionStore.clear()
    }

    private suspend fun post(url: String, body: String) = send("POST", url, body, authorized = false)

    private suspend fun send(
        method: String,
        url: String,
        body: String,
        authorized: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val bearer = if (authorized) _session.value?.accessToken ?: anonKey else anonKey
        val request = Request.Builder()
            .url(url)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $bearer")
            .header("Content-Type", "application/json")
            .method(method, body.toRequestBody(jsonMedia))
            .build()

        val response = try {
            http.newCall(request).execute()
        } catch (io: IOException) {
            throw AuthError.Network(io.message ?: "connection failed")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (it.isSuccessful) return@withContext text
            throw authErrorFor(it.code, text)
        }
    }

    /** GoTrue reports failures as prose, so the mapping is by message shape. */
    private fun authErrorFor(code: Int, body: String): AuthError {
        val message = runCatching {
            val obj = json.parseToJsonElement(body).jsonObject
            listOf("error_description", "msg", "message", "error")
                .firstNotNullOfOrNull { obj[it]?.jsonPrimitive?.content }
        }.getOrNull() ?: "HTTP $code"
        val lower = message.lowercase()
        return when {
            lower.contains("invalid login") || lower.contains("invalid credentials") ||
                lower.contains("invalid email or password") -> AuthError.InvalidCredentials
            lower.contains("already registered") || lower.contains("already been registered") ||
                lower.contains("user already exists") -> AuthError.EmailAlreadyInUse
            lower.contains("password should be") || lower.contains("password is too short") ||
                lower.contains("weak password") -> AuthError.WeakPassword
            else -> AuthError.Unknown(message)
        }
    }

    private fun parseSession(body: String, fallbackEmail: String): AuthSession {
        val payload = json.decodeFromString(TokenResponse.serializer(), body)
        val token = payload.accessToken
            ?: throw AuthError.Unknown("No session was returned.")
        val user = payload.user ?: throw AuthError.Unknown("No user was returned.")
        val meta = user.metadata
        return AuthSession(
            userId = user.id,
            email = user.email ?: fallbackEmail,
            fullName = meta?.fullName ?: meta?.name,
            avatarUrl = meta?.avatarUrl ?: meta?.picture,
            accessToken = token,
            refreshToken = payload.refreshToken.orEmpty(),
            // expires_in is seconds from now; expires_at (when present) is a
            // unix timestamp in seconds. Prefer the former — it does not care
            // whether the device clock agrees with the server's.
            expiresAt = payload.expiresIn
                ?.let { System.currentTimeMillis() + it * 1_000L }
                ?: payload.expiresAt?.let { it * 1_000L }
                ?: (System.currentTimeMillis() + DEFAULT_TTL_MS),
        )
    }

    /** Applies a bare `/user` response onto the session we already hold. */
    private fun mergeUserInto(body: String) {
        val current = _session.value ?: return
        val user = runCatching { json.decodeFromString(GoTrueUser.serializer(), body) }.getOrNull()
            ?: return
        store(
            current.copy(
                email = user.email ?: current.email,
                fullName = user.metadata?.fullName ?: user.metadata?.name ?: current.fullName,
                avatarUrl = user.metadata?.avatarUrl ?: user.metadata?.picture ?: current.avatarUrl,
            )
        )
    }

    // ── Wire format ───────────────────────────────────────────────────

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        @SerialName("expires_at") val expiresAt: Long? = null,
        val user: GoTrueUser? = null,
    )

    @Serializable
    private data class GoTrueUser(
        val id: String,
        val email: String? = null,
        @SerialName("user_metadata") val metadata: UserMetadata? = null,
    )

    @Serializable
    private data class UserMetadata(
        @SerialName("full_name") val fullName: String? = null,
        val name: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        val picture: String? = null,
    )

    private companion object {
        /** Refresh this far ahead of expiry so a request never races it. */
        const val REFRESH_WINDOW_MS = 60_000L
        const val DEFAULT_TTL_MS = 3_600_000L
    }
}
