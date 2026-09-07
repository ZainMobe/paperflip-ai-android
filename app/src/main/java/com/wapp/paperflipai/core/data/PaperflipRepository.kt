package com.wapp.paperflipai.core.data

import android.util.Log
import com.wapp.paperflipai.core.network.RemoteStore
import com.wapp.paperflipai.core.srs.SM2
import com.wapp.paperflipai.core.srs.SRSResponse

/**
 * Write path for everything the user does to their library.
 *
 * Every mutation is optimistic: the local store changes immediately so the
 * UI is instant, then the change is pushed to the [RemoteStore]. If the push
 * fails the local copy stands and the next sync reconciles — the same
 * "local first, server catches up" contract the iOS screens rely on.
 *
 * This layer exists on Android but not on iOS, where each SwiftUI view
 * mutates its SwiftData objects directly. Pulling it out keeps the Compose
 * screens declarative and makes the mutations testable.
 */
class PaperflipRepository(
    private val database: PaperflipDatabase,
    private val remote: RemoteStore,
) {

    // ── Folders ───────────────────────────────────────────────────────

    suspend fun createFolder(userId: String, name: String, colorHex: String): PFFolder {
        val folder = PFFolder(userId = userId, name = name, colorHex = colorHex)
        database.upsertFolder(folder)
        push { remote.createFolder(folder.toDto()) }
        return folder
    }

    suspend fun updateFolder(folder: PFFolder, name: String, colorHex: String) {
        val updated = folder.copy(
            name = name,
            colorHex = colorHex,
            updatedAt = System.currentTimeMillis(),
        )
        database.upsertFolder(updated)
        push { remote.updateFolder(updated.toDto()) }
    }

    suspend fun deleteFolder(folderId: String) {
        database.deleteFolder(folderId)
        push { remote.deleteFolder(folderId) }
    }

    // ── Decks ─────────────────────────────────────────────────────────

    suspend fun renameDeck(deck: PFDeck, title: String) {
        val updated = deck.copy(title = title, updatedAt = System.currentTimeMillis())
        database.upsertDeck(updated)
        pushDeck(updated)
    }

    suspend fun moveDeckToFolder(deck: PFDeck, folderId: String?) {
        val updated = deck.copy(folderId = folderId, updatedAt = System.currentTimeMillis())
        database.upsertDeck(updated)
        pushDeck(updated)
    }

    /** Returns false when the server rejected the move; the local copy is reverted. */
    suspend fun moveDeckToProject(deck: PFDeck, projectId: String?): Boolean {
        val updated = deck.copy(projectId = projectId, updatedAt = System.currentTimeMillis())
        database.upsertDeck(updated)
        return try {
            remote.setDeckProject(deck.id, projectId)
            true
        } catch (error: Exception) {
            Log.w(TAG, "setDeckProject failed, reverting", error)
            database.upsertDeck(deck)
            false
        }
    }

    suspend fun setDeckPublic(deck: PFDeck, isPublic: Boolean): Boolean {
        val updated = deck.copy(isPublic = isPublic, updatedAt = System.currentTimeMillis())
        database.upsertDeck(updated)
        return try {
            remote.setDeckPublic(deck.id, isPublic)
            true
        } catch (error: Exception) {
            Log.w(TAG, "setDeckPublic failed, reverting", error)
            database.upsertDeck(deck)
            false
        }
    }

    suspend fun deleteDeck(deck: PFDeck) {
        database.deleteDeck(deck.id)
        push { remote.deleteDeck(deck.id) }
    }

    /** Persists a freshly generated (or imported) deck and its cards. */
    fun saveDeck(dto: DeckDto, folderId: String? = null, projectId: String? = null): PFDeck {
        val deck = dto.toDeck().copy(folderId = folderId ?: dto.folderId, projectId = projectId ?: dto.projectId)
        database.upsertDeckWithCards(deck, dto.toCards())
        return deck
    }

    // ── Cards ─────────────────────────────────────────────────────────

    suspend fun addCard(deck: PFDeck, front: String, back: String, hint: String? = null) {
        val order = database.cards(deck.id).maxOfOrNull { it.orderIndex }?.plus(1) ?: 0
        val card = PFFlashcard(
            deckId = deck.id,
            front = front,
            back = back,
            hint = hint,
            orderIndex = order,
        )
        database.upsertCard(card)
        touchAndPush(deck)
    }

    suspend fun updateCard(deck: PFDeck, card: PFFlashcard, front: String, back: String, hint: String?) {
        database.upsertCard(
            card.copy(front = front, back = back, hint = hint, updatedAt = System.currentTimeMillis())
        )
        touchAndPush(deck)
    }

    suspend fun deleteCard(deck: PFDeck, cardId: String) {
        database.deleteCard(cardId)
        touchAndPush(deck)
    }

    // ── Study ─────────────────────────────────────────────────────────

    /** Applies an SM-2 response to a card and stores the new schedule. */
    fun review(card: PFFlashcard, response: SRSResponse) {
        database.upsertCard(SM2.apply(response, card))
    }

    fun finishSession(session: PFStudySession, deckIds: Collection<String>) {
        database.addSession(session.copy(endedAt = System.currentTimeMillis()))
        val now = System.currentTimeMillis()
        deckIds.forEach { deckId ->
            database.deck(deckId)?.let { database.upsertDeck(it.copy(lastStudiedAt = now)) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Bumps updatedAt and pushes the deck (used after card edits/reorders). */
    suspend fun touchDeck(deck: PFDeck) = touchAndPush(deck)

    private suspend fun touchAndPush(deck: PFDeck) {
        val updated = deck.copy(updatedAt = System.currentTimeMillis())
        database.upsertDeck(updated)
        pushDeck(updated)
    }

    private suspend fun pushDeck(deck: PFDeck) {
        val cards = database.cards(deck.id)
        push { remote.updateDeck(deck.toDto(cards)) }
    }

    /**
     * Fire-and-log: a failed push is not a user-facing error because the
     * local write already succeeded and sync will reconcile later.
     */
    private suspend fun push(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            Log.w(TAG, "Remote push failed; local change stands", error)
        }
    }

    private companion object {
        const val TAG = "PaperflipRepository"
    }
}
