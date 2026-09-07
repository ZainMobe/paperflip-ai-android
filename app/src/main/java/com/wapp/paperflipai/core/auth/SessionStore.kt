package com.wapp.paperflipai.core.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

/**
 * Persists the auth session across launches — the Android counterpart of
 * `KeychainStore.swift`.
 *
 * Kept in its own SharedPreferences file that is excluded from cloud backup
 * and device transfer (see `res/xml/data_extraction_rules.xml`), so tokens
 * never leave the device. When you wire the real Supabase client, swap the
 * body for `EncryptedSharedPreferences` — the surface here does not change.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AuthSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString(AuthSession.serializer(), raw) }.getOrNull()
    }

    fun save(session: AuthSession) {
        prefs.edit()
            .putString(KEY_SESSION, json.encodeToString(AuthSession.serializer(), session))
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private companion object {
        const val PREFS = "paperflip_session"
        const val KEY_SESSION = "session.v1"
    }
}
