package com.wapp.paperflipai.app

import java.net.URLDecoder
import java.net.URLEncoder

/** Every destination in the authenticated part of the app. */
object Route {
    // Top-level tabs
    const val LIBRARY = "library"
    const val STUDY = "study"
    const val IMPORT = "import"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    val topLevel = listOf(LIBRARY, STUDY, IMPORT, STATS, SETTINGS)

    // Library
    const val DECK = "deck/{deckId}"
    fun deck(deckId: String) = "deck/$deckId"

    // Projects
    const val PROJECT = "project/{projectId}"
    fun project(projectId: String) = "project/$projectId"

    // Study
    const val STUDY_SESSION = "studySession?deckId={deckId}&projectId={projectId}"
    fun studySession(deckId: String? = null, projectId: String? = null) =
        "studySession?deckId=${deckId.orEmpty()}&projectId=${projectId.orEmpty()}"

    // Import
    const val IMPORT_SOURCE = "importSource/{sourceType}?shared={shared}"
    fun importSource(sourceType: String, shared: String? = null) =
        "importSource/$sourceType?shared=${shared.orEmpty().encode()}"

    // Settings
    const val SETTINGS_PROFILE = "settings/profile"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_SUBSCRIPTION = "settings/subscription"
    const val SETTINGS_DELETE = "settings/delete"

    // Standalone
    const val PAYWALL = "paywall"
    const val PRO_UNLOCKED = "proUnlocked"
    const val NOTIFICATIONS_INBOX = "notificationsInbox"
    const val SUPPORT = "support"
    const val PENDING_INVITES = "pendingInvites"

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")
    fun decode(value: String?): String = runCatching {
        URLDecoder.decode(value.orEmpty(), "UTF-8")
    }.getOrDefault("")
}
