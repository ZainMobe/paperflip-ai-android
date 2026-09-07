package com.wapp.paperflipai.core.sync

import com.wapp.paperflipai.core.card
import com.wapp.paperflipai.core.data.CardDto
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.PaperflipSnapshot
import com.wapp.paperflipai.core.deck
import com.wapp.paperflipai.core.network.RemoteSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The most consequential logic in the app. A bug here does not crash — it
 * silently resets every user's review schedule on their next sync, which is
 * unrecoverable and invisible until they notice their whole library is due
 * again. Hence the disproportionate number of tests.
 */
class SnapshotMergerTest {

    private fun remoteDeck(
        id: String = "deck-1",
        title: String = "Deck",
        updatedAt: Long = 0L,
        cards: List<CardDto> = emptyList(),
    ) = DeckDto(
        id = id,
        userId = "user-1",
        title = title,
        sourceType = "article",
        cards = cards,
        createdAt = 0L,
        updatedAt = updatedAt,
    )

    // ── SRS preservation ──────────────────────────────────────────────

    @Test fun `a remote text edit does not reset the card's schedule`() {
        val local = PaperflipSnapshot(
            decks = listOf(deck(updatedAt = 100)),
            cards = listOf(
                card(id = "c1", front = "capital of France", interval = 40, repetitions = 6, dueDate = 999, ease = 2.9)
            ),
        )
        val remote = RemoteSnapshot(
            decks = listOf(
                remoteDeck(
                    updatedAt = 200, // server is newer, so the deck is rebuilt
                    cards = listOf(CardDto(id = "c1", front = "Capital of France?", back = "Paris")),
                )
            )
        )

        val synced = SnapshotMerger.merge(local, remote).cards.single()

        assertEquals("Capital of France?", synced.front) // text updated
        assertEquals(40, synced.srsInterval)             // schedule intact
        assertEquals(6, synced.srsRepetitions)
        assertEquals(999L, synced.srsDueDate)
        assertEquals(2.9, synced.srsEase, 1e-9)
    }

    @Test fun `schedule survives the server reassigning card ids, matched on front text`() {
        val local = PaperflipSnapshot(
            decks = listOf(deck(updatedAt = 100)),
            cards = listOf(card(id = "old-id", front = "  Mitochondria ", interval = 21, repetitions = 4)),
        )
        val remote = RemoteSnapshot(
            decks = listOf(
                remoteDeck(
                    updatedAt = 200,
                    cards = listOf(CardDto(id = "server-generated", front = "mitochondria", back = "powerhouse")),
                )
            )
        )

        val rebuilt = SnapshotMerger.merge(local, remote).cards.single()
        assertEquals("server-generated", rebuilt.id)
        assertEquals(21, rebuilt.srsInterval)
        assertEquals(4, rebuilt.srsRepetitions)
    }

    @Test fun `a genuinely new remote card starts unscheduled`() {
        val local = PaperflipSnapshot(decks = listOf(deck(updatedAt = 100)), cards = emptyList())
        val remote = RemoteSnapshot(
            decks = listOf(
                remoteDeck(updatedAt = 200, cards = listOf(CardDto(id = "c-new", front = "new", back = "card")))
            )
        )

        val added = SnapshotMerger.merge(local, remote).cards.single()
        assertEquals(0, added.srsRepetitions)
        assertEquals(0, added.srsInterval)
        assertNull(added.srsLastReviewedAt)
    }

    // ── Last-write-wins ───────────────────────────────────────────────

    @Test fun `a locally newer deck is not overwritten by the server`() {
        val local = PaperflipSnapshot(
            decks = listOf(deck(title = "My better title", updatedAt = 500)),
            cards = listOf(card(id = "c1", interval = 12)),
        )
        val remote = RemoteSnapshot(
            decks = listOf(remoteDeck(title = "Stale server title", updatedAt = 100, cards = emptyList()))
        )

        val merged = SnapshotMerger.merge(local, remote)
        assertEquals("My better title", merged.decks.single().title)
        assertEquals(1, merged.cards.size) // local cards kept, not wiped by the empty remote deck
        assertEquals(12, merged.cards.single().srsInterval)
    }

    @Test fun `equal timestamps favour the local copy`() {
        val local = PaperflipSnapshot(decks = listOf(deck(title = "Local", updatedAt = 300)))
        val remote = RemoteSnapshot(decks = listOf(remoteDeck(title = "Remote", updatedAt = 300)))
        assertEquals("Local", SnapshotMerger.merge(local, remote).decks.single().title)
    }

    // ── Local-only state ──────────────────────────────────────────────

    @Test fun `lastStudiedAt is local-only and survives a server-driven rebuild`() {
        val local = PaperflipSnapshot(decks = listOf(deck(updatedAt = 100, lastStudiedAt = 4242)))
        val remote = RemoteSnapshot(decks = listOf(remoteDeck(updatedAt = 200)))

        assertEquals(4242L, SnapshotMerger.merge(local, remote).decks.single().lastStudiedAt)
    }

    @Test fun `a deck created offline is not deleted by a sync that has never seen it`() {
        val local = PaperflipSnapshot(
            decks = listOf(deck(id = "local-only", updatedAt = 100)),
            cards = listOf(card(id = "c1", deckId = "local-only")),
        )
        val remote = RemoteSnapshot(decks = listOf(remoteDeck(id = "deck-1", updatedAt = 200)))

        val merged = SnapshotMerger.merge(local, remote)
        assertNotNull(merged.decks.firstOrNull { it.id == "local-only" })
        assertTrue(merged.cards.any { it.deckId == "local-only" })
        assertEquals(2, merged.decks.size)
    }

    @Test fun `cards belonging to a deck deleted on the server are dropped`() {
        val local = PaperflipSnapshot(
            decks = listOf(deck(id = "deck-1", updatedAt = 100)),
            cards = listOf(card(id = "c1", deckId = "deck-1"), card(id = "c2", deckId = "ghost-deck")),
        )
        val remote = RemoteSnapshot(decks = listOf(remoteDeck(id = "deck-1", updatedAt = 200)))

        val merged = SnapshotMerger.merge(local, remote)
        assertTrue(merged.cards.none { it.deckId == "ghost-deck" })
    }

    // ── Empty payloads ────────────────────────────────────────────────

    @Test fun `an empty projects list means 'not synced', not 'delete everything'`() {
        val local = PaperflipSnapshot(decks = emptyList())
        // A snapshot with no project data must leave local projects untouched;
        // the endpoint is paginated separately and an empty list is ambiguous.
        val merged = SnapshotMerger.merge(local, RemoteSnapshot())
        assertEquals(local.projects, merged.projects)
        assertEquals(local.members, merged.members)
        assertEquals(local.invites, merged.invites)
    }
}
