package com.wapp.paperflipai.core.sync

import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.data.PaperflipSnapshot
import com.wapp.paperflipai.core.network.RemoteSnapshot

/**
 * The pure half of [SyncEngine]: reconciling a remote snapshot into the local
 * one. Extracted deliberately — it touches no Android API, no coroutines and
 * no I/O, so it can be unit-tested on the plain JVM.
 *
 * This is the highest-consequence logic in the app. Getting it wrong does not
 * crash; it quietly resets everyone's review schedule on the next sync.
 */
internal object SnapshotMerger {

    fun merge(local: PaperflipSnapshot, snapshot: RemoteSnapshot): PaperflipSnapshot {
        // ── Folders ───────────────────────────────────────────────────
        val remoteFolderIds = snapshot.folders.map { it.id }.toSet()
        val folders = snapshot.folders.map { dto ->
            val existing = local.folders.firstOrNull { it.id == dto.id }
            if (existing != null && existing.updatedAt >= dto.updatedAt) existing else dto.toModel()
        } + local.folders.filter { it.id !in remoteFolderIds }

        // ── Decks + cards ─────────────────────────────────────────────
        val remoteDeckIds = snapshot.decks.map { it.id }.toSet()
        val cardsByDeck = local.cards.groupBy { it.deckId }

        val decks = ArrayList<PFDeck>(snapshot.decks.size)
        val cards = ArrayList<PFFlashcard>()

        snapshot.decks.forEach { dto ->
            val existing = local.decks.firstOrNull { it.id == dto.id }
            if (existing != null && existing.updatedAt >= dto.updatedAt) {
                decks.add(existing)
                cards.addAll(cardsByDeck[existing.id].orEmpty())
            } else {
                // lastStudiedAt is local-only — the server never sends it.
                decks.add(dto.toDeck().copy(lastStudiedAt = existing?.lastStudiedAt))
                cards.addAll(preserveSrs(dto.toCards(), cardsByDeck[dto.id].orEmpty()))
            }
        }

        // Decks created on this device that the server hasn't seen yet stay put.
        local.decks.filter { it.id !in remoteDeckIds }.forEach { orphan ->
            decks.add(orphan)
            cards.addAll(cardsByDeck[orphan.id].orEmpty())
        }

        val liveDeckIds = decks.map { it.id }.toSet()

        return local.copy(
            folders = folders,
            decks = decks,
            cards = cards,
            sessions = local.sessions.filter { it.deckId == null || it.deckId in liveDeckIds },
            projects = if (snapshot.projects.isEmpty()) local.projects
            else snapshot.projects.map { it.toModel() },
            members = if (snapshot.projectMembers.isEmpty()) local.members
            else snapshot.projectMembers.map { dto ->
                dto.toModel(snapshot.memberProfiles[dto.userId])
            },
            invites = if (snapshot.pendingInvites.isEmpty()) local.invites
            else snapshot.pendingInvites.map { it.toModel() },
        )
    }

    /**
     * Carries per-card SRS scheduling across a remote-driven rebuild so a
     * text edit upstream never wipes the user's review history.
     */
    internal fun preserveSrs(incoming: List<PFFlashcard>, existing: List<PFFlashcard>): List<PFFlashcard> {
        if (existing.isEmpty()) return incoming
        val byId = existing.associateBy { it.id }
        val byFront = existing.associateBy { it.front.trim().lowercase() }
        return incoming.map { card ->
            val match = byId[card.id] ?: byFront[card.front.trim().lowercase()] ?: return@map card
            card.copy(
                srsEase = match.srsEase,
                srsInterval = match.srsInterval,
                srsRepetitions = match.srsRepetitions,
                srsDueDate = match.srsDueDate,
                srsLastReviewedAt = match.srsLastReviewedAt,
            )
        }
    }
}
