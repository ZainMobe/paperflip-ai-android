package com.wapp.paperflipai.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Minimal Supabase transport — PostgREST, Edge Functions and Storage over
 * plain HTTP. The Android counterpart of what `supabase-swift` does for the
 * iOS target.
 *
 * Deliberately not the official Kotlin SDK: it pulls Ktor plus a dozen
 * transitive modules, and everything this app needs is REST with two headers.
 * One dependency (OkHttp) against a build that cannot be verified here is a
 * trade worth making; a dependency graph is not.
 *
 * Auth: every request carries the anon key as `apikey`, plus a bearer token —
 * the signed-in user's JWT when there is one, falling back to the anon key so
 * unauthenticated calls (public shared decks) still work.
 *
 * [accessToken] is a *suspend* lambda, resolved per request rather than
 * captured once: `SupabaseAuthService` refreshes an expiring token inside it,
 * so a long session never starts 401-ing mid-use.
 */
class SupabaseClient(
    baseUrl: String,
    private val anonKey: String,
    private val accessToken: suspend () -> String?,
) {
    private val base = baseUrl.trimEnd('/')
    private val restUrl = "$base/rest/v1"
    private val functionsUrl = "$base/functions/v1"
    private val storageUrl = "$base/storage/v1"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // deck generation is slow
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val jsonMedia = "application/json".toMediaType()

    // ── PostgREST ─────────────────────────────────────────────────────

    /**
     * @param query raw PostgREST query string, e.g. `select=*&user_id=eq.$id`.
     * @param prefer `Prefer` header — `return=representation` to get the row
     *        back on write, `resolution=merge-duplicates` for upserts.
     */
    suspend fun rest(
        method: String,
        table: String,
        query: String = "",
        body: String? = null,
        prefer: String? = null,
        single: Boolean = false,
    ): String {
        val url = buildString {
            append(restUrl).append('/').append(table)
            if (query.isNotEmpty()) append('?').append(query)
        }
        val token = accessToken()
        val request = Request.Builder()
            .url(url)
            .apply {
                headers(token)
                prefer?.let { header("Prefer", it) }
                // Asks PostgREST for a bare object instead of a 1-element
                // array, and for a 406 rather than a silent empty array when
                // the row is missing.
                if (single) header("Accept", "application/vnd.pgrst.object+json")
                method(method, body?.toRequestBody(jsonMedia) ?: emptyBodyFor(method))
            }
            .build()
        return execute(request)
    }

    // ── Edge Functions ────────────────────────────────────────────────

    suspend fun invoke(function: String, body: String = "{}"): String {
        val token = accessToken()
        val request = Request.Builder()
            .url("$functionsUrl/$function")
            .apply { headers(token) }
            .post(body.toRequestBody(jsonMedia))
            .build()
        return execute(request)
    }

    // ── Storage ───────────────────────────────────────────────────────

    /**
     * Uploads to [bucket] and returns the object path.
     *
     * The folder is the user's id **lowercased**: Postgres renders
     * `auth.uid()::text` in lowercase and the storage RLS policy compares it
     * as text, so an uppercase UUID fails every upload. Same trap as iOS.
     */
    suspend fun storageUpload(
        bucket: String,
        userId: String,
        bytes: ByteArray,
        contentType: String,
        extension: String,
    ): String {
        val path = "${userId.lowercase()}/${UUID.randomUUID().toString().lowercase()}.$extension"
        val token = accessToken()
        val request = Request.Builder()
            .url("$storageUrl/object/$bucket/$path")
            .apply { headers(token) }
            .header("Content-Type", contentType)
            .header("x-upsert", "false")
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .build()
        execute(request)
        return path
    }

    fun publicUrl(bucket: String, path: String): String =
        "$storageUrl/object/public/$bucket/$path"

    // ── Plumbing ──────────────────────────────────────────────────────

    private fun Request.Builder.headers(token: String?) {
        header("apikey", anonKey)
        header("Authorization", "Bearer ${token ?: anonKey}")
    }

    private fun emptyBodyFor(method: String): RequestBody? =
        if (method == "GET" || method == "HEAD") null else "".toRequestBody(jsonMedia)

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        val response = try {
            http.newCall(request).execute()
        } catch (io: IOException) {
            throw RemoteStoreError.Network(io.message ?: "connection failed")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (it.isSuccessful) return@withContext text
            throw errorFor(it.code, text)
        }
    }

    /**
     * Maps a failed response onto [RemoteStoreError], mirroring the iOS
     * `mapError`. Plan-limit detection is by message shape because PostgREST
     * surfaces the Edge Function's own error text rather than a typed code.
     */
    private fun errorFor(code: Int, body: String): RemoteStoreError {
        val message = messageIn(body) ?: body.take(300).ifBlank { "HTTP $code" }
        val lower = message.lowercase()
        return when {
            code == 401 || code == 403 -> RemoteStoreError.NotAuthenticated
            code == 404 || code == 406 -> RemoteStoreError.NotFound
            code == 429 || lower.contains("rate limit") -> RemoteStoreError.RateLimited
            lower.contains("plan") && (lower.contains("limit") || lower.contains("cap")) ->
                RemoteStoreError.PlanLimitReached(message)
            else -> RemoteStoreError.Unknown(message)
        }
    }

    /** PostgREST, GoTrue and the Edge Functions each name the field differently. */
    private fun messageIn(body: String): String? = runCatching {
        val obj = json.parseToJsonElement(body).jsonObject
        for (key in listOf("message", "error", "error_description", "msg", "hint")) {
            obj[key]?.jsonPrimitive?.contentOrNullSafe()?.let { return@runCatching it }
        }
        null
    }.getOrNull()

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { content }.getOrNull()?.takeIf { it.isNotBlank() && it != "null" }

    companion object {
        /** PostgREST needs values percent-encoded inside filters. */
        fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

        /** Reads a top-level field out of an Edge Function response. */
        fun field(json: Json, body: String, name: String): JsonElement? = runCatching {
            json.parseToJsonElement(body).jsonObject[name]
        }.getOrNull()
    }
}
