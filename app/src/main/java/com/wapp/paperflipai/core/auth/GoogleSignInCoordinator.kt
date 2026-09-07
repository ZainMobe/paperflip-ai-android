package com.wapp.paperflipai.core.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.wapp.paperflipai.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Google sign-in via Credential Manager — the Android counterpart of
 * `GoogleSignInCoordinator.swift` (which wraps Sign in with Apple; Google is
 * the equivalent affordance here).
 *
 * ### The nonce, which is the part that is easy to get wrong
 * Two different values are in play and swapping them fails with an opaque
 * "bad id token":
 *  - Google is handed the **SHA-256 hash** of the nonce, and embeds that
 *    hash in the issued token.
 *  - Supabase is handed the **raw** nonce, hashes it itself, and compares.
 *
 * So [signIn] generates a raw nonce, gives Google [hashedNonce], and returns
 * the raw one in [GoogleSignInResult.rawNonce] for `SupabaseAuthService` to
 * forward untouched.
 *
 * ### Setup this depends on
 *  1. `GOOGLE_WEB_CLIENT_ID` in `local.properties` — the **Web** client id
 *     from Google Cloud, not the Android one. The Android client exists only
 *     so Google can verify the app signature; the token is *issued for* the
 *     web client, which is what Supabase validates against.
 *  2. Both debug and release SHA-1 fingerprints registered on the Android
 *     OAuth client.
 *  3. That same web client id set as the Client ID in
 *     Supabase → Authentication → Providers → Google.
 *
 * With [isConfigured] false the button should not reach Supabase at all —
 * see `WelcomeAuthScreen`, which fails fast with a readable message rather
 * than posting a placeholder and getting "bad id token" back.
 */
class GoogleSignInCoordinator(private val context: Context) {

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * @param activity Credential Manager renders a bottom sheet, so it needs
     *   an Activity context — the application context throws at runtime.
     */
    suspend fun signIn(activity: Activity): GoogleSignInResult {
        if (!isConfigured) {
            throw AuthError.Unknown(
                "Google sign-in isn't configured in this build. Use email instead."
            )
        }

        val raw = rawNonce()
        val option = GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(hashedNonce(raw))
            .build()

        val response = try {
            CredentialManager.create(context).getCredential(
                context = activity,
                request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build(),
            )
        } catch (cancelled: GetCredentialCancellationException) {
            throw AuthError.UserCancelled
        } catch (none: NoCredentialException) {
            throw AuthError.Unknown(
                "No Google account is available on this device. Add one in " +
                    "Settings, or continue with email."
            )
        } catch (failure: GetCredentialException) {
            // Almost always a configuration problem rather than a user one:
            // an unregistered SHA-1, or a client id that doesn't match.
            throw AuthError.Unknown(failure.message ?: "Google sign-in failed.")
        }

        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthError.Unknown("Unexpected credential type from Google.")
        }

        val googleId = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (invalid: Exception) {
            throw AuthError.Unknown("Couldn't read the Google credential.")
        }

        return GoogleSignInResult(
            idToken = googleId.idToken,
            accessToken = null,
            rawNonce = raw,
            // Supabase derives the real user id from the token; this is only
            // for logging and the optimistic UI.
            userId = googleId.id,
            fullName = googleId.displayName,
            email = googleId.id,
        )
    }

    /** Random nonce. Its SHA-256 hash is what Google embeds in the token. */
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
