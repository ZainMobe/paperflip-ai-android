package com.wapp.paperflipai.core.sync

import android.util.Log
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.data.PaperflipDatabase
import com.wapp.paperflipai.core.data.PaperflipSnapshot
import com.wapp.paperflipai.core.network.NetworkMonitor
import com.wapp.paperflipai.core.network.RemoteSnapshot
import com.wapp.paperflipai.core.network.RemoteStore
import com.wapp.paperflipai.core.network.RemoteStoreError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pulls the user's data from [RemoteStore] and reconciles it into the local
 * store — the Android counterpart of `SyncEngine.swift`.
 *
 * Reconciliation (read path):
 *  1. Fetch the snapshot for the user.
 *  2. Upsert folders by id; drop locals that no longer exist remotely.
 *  3. Upsert decks and their cards by id; drop locals removed remotely.
 *  4. Last-write-wins by `updatedAt` on conflict.
 *
 * Per-card SRS state is preserved across a card rebuild: a remote edit to a
 * card's text must not reset the schedule the user has built up on this
 * device.
 */
class SyncEngine(
    private val remote: RemoteStore,
    private val database: PaperflipDatabase,
    private val networkMonitor: NetworkMonitor,
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val gate = Mutex()

    /** Full pull-down. Called on session restore and on explicit refresh. */
    suspend fun pullAll(userId: String) {
        if (_isSyncing.value) return
        if (!networkMonitor.isOnline.value) {
            _lastError.value = "Offline — using cached data."
            return
        }
        gate.withLock {
            _isSyncing.value = true
            _lastError.value = null
            try {
                val snapshot = remote.snapshot(userId)
                reconcile(snapshot)
                _lastSyncedAt.value = System.currentTimeMillis()
            } catch (error: RemoteStoreError) {
                _lastError.value = error.message
            } catch (error: Exception) {
                _lastError.value = error.message ?: "Sync failed."
                Log.w(TAG, "pullAll failed", error)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Pulls just the project list. Cheap enough to call whenever Library
     * appears — does not fetch members or invites.
     */
    suspend fun pullProjects(userId: String) {
        if (!networkMonitor.isOnline.value) return
        runCatching {
            val dtos = remote.fetchProjects(userId)
            val remoteIds = dtos.map { it.id }.toSet()
            database.update { local ->
                val incoming = dtos.map { dto ->
                    val existing = local.projects.firstOrNull { it.id == dto.id }
                    if (existing != null && existing.updatedAt >= dto.updatedAt) existing else dto.toModel()
                }
                local.copy(
                    projects = incoming,
                    members = local.members.filter { it.projectId in remoteIds },
                )
            }
        }.onFailure { Log.w(TAG, "pullProjects failed", it) }
    }

    /** Pulls the invites pending for this user's email. */
    suspend fun pullPendingInvites(email: String) {
        if (!networkMonitor.isOnline.value) return
        runCatching {
            val invites = remote.fetchPendingInvites(email).map { it.toModel() }
            database.replaceInvites(invites)
        }.onFailure { Log.w(TAG, "pullPendingInvites failed", it) }
    }

    // ── Reconciliation ────────────────────────────────────────────────

    private fun reconcile(snapshot: RemoteSnapshot) {
        database.update { local -> merge(local, snapshot) }
    }

    internal fun merge(local: PaperflipSnapshot, snapshot: RemoteSnapshot): PaperflipSnapshot {
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
    private fun preserveSrs(incoming: List<PFFlashcard>, existing: List<PFFlashcard>): List<PFFlashcard> {
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

    private companion object {
        const val TAG = "SyncEngine"
    }
}

