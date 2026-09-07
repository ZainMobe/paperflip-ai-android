package com.wapp.paperflipai.core.auth

import android.content.Context
import android.util.Base64
import com.wapp.paperflipai.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Google sign-in entry point — the Android counterpart of
 * `GoogleSignInCoordinator.swift`.
 *
 * The production implementation is Credential Manager (`androidx.credentials`
 * + `googleid`), which needs a web client id in `GOOGLE_WEB_CLIENT_ID`.
 * Until those dependencies are added this returns a mock result so the
 * "Continue with Google" button is exercisable end-to-end, exactly like the
 * mock auth and mock remote store.
 *
 * To go live:
 *  1. Add `androidx.credentials:credentials`,
 *     `androidx.credentials:credentials-play-services-auth` and
 *     `com.google.android.libraries.identity.googleid:googleid`.
 *  2. Put your OAuth **web** client id in `local.properties` as
 *     `GOOGLE_WEB_CLIENT_ID`.
 *  3. Replace [signIn] with a `CredentialManager.getCredential` call that
 *     builds a `GetSignInWithGoogleOption` using [rawNonce]'s SHA-256 hash,
 *     then map the returned id token into [GoogleSignInResult].
 */
class GoogleSignInCoordinator(private val context: Context) {

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signIn(): GoogleSignInResult {
        val nonce = rawNonce()
        return GoogleSignInResult(
            idToken = "mock-google-id-token",
            accessToken = null,
            rawNonce = nonce,
            userId = UUID.randomUUID().toString(),
            fullName = null,
            email = "google.user@gmail.com",
        )
    }

    /** Random nonce; its SHA-256 hash is what Google embeds in the token. */
    fun rawNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun hashedNonce(rawNonce: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
