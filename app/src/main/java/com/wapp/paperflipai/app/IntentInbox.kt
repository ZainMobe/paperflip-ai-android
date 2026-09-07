package com.wapp.paperflipai.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny mailbox between the outside world and the running UI — the Android
 * counterpart of `IntentInbox.swift`.
 *
 * Deep links, notification taps, widget taps and share-sheet hand-offs all
 * arrive outside the composition, so they drop a request here and the root
 * navigation reacts on the next frame.
 */
sealed interface PFAction {
    data object StartStudy : PFAction
    /** Launcher shortcut / deep link straight to the import hub. */
    data object OpenImport : PFAction
    data class OpenProject(val projectId: String) : PFAction
    data class OpenDeck(val deckId: String) : PFAction
    data class JoinProject(val token: String) : PFAction
    data class ImportSharedDeck(val link: String) : PFAction
    /** A URL or block of text shared into the app from another app. */
    data class ImportSharedText(val text: String) : PFAction
    /** A PDF shared into the app; [uri] is a content:// URI we hold a grant for. */
    data class ImportSharedPdf(val uri: String) : PFAction
}

object IntentInbox {
    private val _pending = MutableStateFlow<PFAction?>(null)
    val pending: StateFlow<PFAction?> = _pending.asStateFlow()

    fun post(action: PFAction) {
        _pending.value = action
    }

    /** Reads and clears the pending action in one step. */
    fun take(): PFAction? {
        val value = _pending.value
        _pending.value = null
        return value
    }

    fun clear() {
        _pending.value = null
    }

    /**
     * Parses a deep link or a notification's `link` field into an action.
     *
     * Recognised shapes:
     * ```
     * paperflip://study
     * paperflip://import
     * paperflip://deck/{id}          /deck/{id}    /decks/{id}
     * paperflip://projects/{id}      /project/{id} /projects/{id}
     * paperflip://projects/join/{token}
     * https://www.paperflip.ai/share/{id}
     * ```
     */
    fun actionFor(link: String?): PFAction? {
        if (link.isNullOrBlank()) return null

        val withoutScheme = link
            .substringAfter("://", link)
            .substringBefore('?')
            .substringBefore('#')
        // Drop the host for absolute https links; paperflip:// puts the first
        // meaningful segment in the host slot, so keep it there.
        val segments = if (link.startsWith("http", ignoreCase = true)) {
            withoutScheme.split('/').drop(1)
        } else {
            withoutScheme.split('/')
        }.filter { it.isNotBlank() }

        if (segments.isEmpty()) return null

        if (segments.size == 1 && segments[0].equals("study", ignoreCase = true)) {
            return PFAction.StartStudy
        }

        if (segments.size == 1 && segments[0].equals("import", ignoreCase = true)) {
            return PFAction.OpenImport
        }

        if (segments.size >= 3 &&
            segments[0].lowercase() in setOf("project", "projects") &&
            segments[1].equals("join", ignoreCase = true)
        ) {
            return PFAction.JoinProject(segments[2])
        }

        if (segments.size >= 2 && segments[0].equals("share", ignoreCase = true)) {
            return PFAction.ImportSharedDeck(link)
        }

        if (segments.size >= 2) {
            val id = segments[1]
            return when (segments[0].lowercase()) {
                "project", "projects" -> PFAction.OpenProject(id)
                "deck", "decks" -> PFAction.OpenDeck(id)
                else -> null
            }
        }
        return null
    }
}
