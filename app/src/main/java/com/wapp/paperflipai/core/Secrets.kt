package com.wapp.paperflipai.core

import android.util.Log
import com.wapp.paperflipai.BuildConfig

/**
 * Runtime configuration — the Android counterpart of `Secrets.swift`.
 *
 * Values come from `local.properties` (gitignored) via `buildConfigField`,
 * so nothing sensitive is committed. Anything missing, blank, or still
 * holding a "your-…" placeholder is treated as absent, and the app falls
 * back to the corresponding Mock service — it keeps running while you stand
 * up the backend at your own pace.
 *
 * ─── One-time setup ──────────────────────────────────────────────────
 * Add to `local.properties`:
 *
 *     SUPABASE_URL=https://xxxx.supabase.co
 *     SUPABASE_ANON_KEY=eyJhbGci...
 *     REVENUECAT_API_KEY=goog_...
 *     GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com
 */
object Secrets {

    val supabaseUrl: String? get() = clean(BuildConfig.SUPABASE_URL)
    val supabaseAnonKey: String? get() = clean(BuildConfig.SUPABASE_ANON_KEY)
    val revenueCatApiKey: String? get() = clean(BuildConfig.REVENUECAT_API_KEY)
    val googleWebClientId: String? get() = clean(BuildConfig.GOOGLE_WEB_CLIENT_ID)

    val hasSupabaseConfig: Boolean get() = supabaseUrl != null && supabaseAnonKey != null
    val hasRevenueCatConfig: Boolean get() = revenueCatApiKey != null

    private fun clean(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.lowercase().contains("your-")) return null
        return trimmed
    }

    /** One-shot diagnostic so the log says which services are live vs mocked. */
    fun logConfigStatus() {
        if (!BuildConfig.DEBUG) return
        Log.i(TAG, "─── PaperFlip secrets ─────────────────────────────")
        Log.i(TAG, "  Supabase:    ${if (hasSupabaseConfig) "✓ live" else "× mock"}")
        Log.i(TAG, "  RevenueCat:  ${if (hasRevenueCatConfig) "✓ live" else "× mock"}")
        Log.i(TAG, "───────────────────────────────────────────────────")
    }

    private const val TAG = "PaperflipSecrets"
}
