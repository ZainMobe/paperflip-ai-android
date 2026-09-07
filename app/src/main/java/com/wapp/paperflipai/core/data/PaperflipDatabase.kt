package com.wapp.paperflipai.core.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The local store — the Android counterpart of `PersistenceController.swift`
 * plus the SwiftData context the screens read from.
 *
 * Everything lives in one immutable [PaperflipSnapshot] behind a StateFlow,
 * persisted to a single JSON file with a short debounce. That keeps the whole
 * data layer synchronous and allocation-cheap for a library of this size,
 * makes every mutation an atomic `copy()`, and means Compose recomposes off a
 * single observable source — no ORM, no annotation processor, no DAO
 * boilerplate.
 *
 * Swapping in Room later is a contained change: keep this class's public
 * surface and back it with DAOs.
 */
@Serializable
data class PaperflipSnapshot(
    val profile: PFProfile? = null,
    val folders: List<PFFolder> = emptyList(),
    val decks: List<PFDeck> = emptyList(),
    val cards: List<PFFlashcard> = emptyList(),
    val sessions: List<PFStudySession> = emptyList(),
    val projects: List<PFProject> = emptyList(),
    val members: List<PFProjectMember> = emptyList(),
    val invites: List<PFProjectInvite> = emptyList(),
)

class PaperflipDatabase(
    private val context: Context,
    private val scope: CoroutineScope,
    private val fileName: String = "paperflip_store.json",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val writeMutex = Mutex()
    private val file: File by lazy { File(context.filesDir, fileName) }

    private val _snapshot = MutableStateFlow(PaperflipSnapshot())
    val snapshot: StateFlow<PaperflipSnapshot> = _snapshot.asStateFlow()

    // ── Derived streams the UI collects ───────────────────────────────

    val profile: StateFlow<PFProfile?> = derive { it.profile }
    val folders: StateFlow<List<PFFolder>> = derive { snap -> snap.folders.sortedBy { it.name.lowercase() } }
    val decks: StateFlow<List<PFDeck>> = derive { snap -> snap.decks.sortedByDescending { it.updatedAt } }
    val cards: StateFlow<List<PFFlashcard>> = derive { it.cards }
    val sessions: StateFlow<List<PFStudySession>> = derive { snap -> snap.sessions.sortedByDescending { it.startedAt } }
    val projects: StateFlow<List<PFProject>> = derive { snap -> snap.projects.sortedBy { it.name.lowercase() } }
    val members: StateFlow<List<PFProjectMember>> = derive { it.members }
    val invites: StateFlow<List<PFProjectInvite>> = derive { it.invites }

    private fun <T> derive(transform: (PaperflipSnapshot) -> T): StateFlow<T> =
        _snapshot.map(transform)
            .stateIn(scope, SharingStarted.Eagerly, transform(_snapshot.value))

    // ── Lifecycle ─────────────────────────────────────────────────────

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            load()
            _snapshot.drop(1).debounce(350L).collect { persist(it) }
        }
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching
            val text = file.readText()
            if (text.isBlank()) return@runCatching
            _snapshot.value = json.decodeFromString(PaperflipSnapshot.serializer(), text)
        }.onFailure { Log.w(TAG, "Local store unreadable, starting fresh", it) }
        Unit
    }

    private suspend fun persist(value: PaperflipSnapshot) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runCatching {
                val tmp = File(file.parentFile, "$fileName.tmp")
                tmp.writeText(json.encodeToString(PaperflipSnapshot.serializer(), value))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            }.onFailure { Log.e(TAG, "Failed to persist local store", it) }
        }
    }

    /** Wipes everything. Used on sign-out and account deletion. */
    fun clear() = update { PaperflipSnapshot() }

    /** Atomic read-modify-write on the whole snapshot. */
    fun update(transform: (PaperflipSnapshot) -> PaperflipSnapshot) {
        _snapshot.value = transform(_snapshot.value)
    }

    // ── Convenience queries (pure reads off the current snapshot) ─────

    fun deck(id: String?): PFDeck? = id?.let { d -> _snapshot.value.decks.firstOrNull { it.id == d } }

    fun cards(deckId: String): List<PFFlashcard> =
        _snapshot.value.cards.filter { it.deckId == deckId }.sortedBy { it.orderIndex }

    fun cardCount(deckId: String): Int = _snapshot.value.cards.count { it.deckId == deckId }

    fun folder(id: String?): PFFolder? = id?.let { f -> _snapshot.value.folders.firstOrNull { it.id == f } }

    fun project(id: String?): PFProject? = id?.let { p -> _snapshot.value.projects.firstOrNull { it.id == p } }

    fun members(projectId: String): List<PFProjectMember> =
        _snapshot.value.members.filter { it.projectId == projectId }

    fun role(projectId: String, userId: String?): PFProjectRole? {
        if (userId == null) return null
        val project = project(projectId) ?: return null
        if (project.ownerUserId == userId) return PFProjectRole.Owner
        return _snapshot.value.members
            .firstOrNull { it.projectId == projectId && it.userId == userId }?.role
    }

    // ── Mutations ─────────────────────────────────────────────────────

    fun upsertProfile(profile: PFProfile?) = update { it.copy(profile = profile) }

    fun upsertFolder(folder: PFFolder) = update { snap ->
        snap.copy(folders = snap.folders.upsert(folder) { it.id == folder.id })
    }

    fun deleteFolder(id: String) = update { snap ->
        snap.copy(
            folders = snap.folders.filterNot { it.id == id },
            decks = snap.decks.map { if (it.folderId == id) it.copy(folderId = null) else it },
        )
    }

    fun upsertDeck(deck: PFDeck) = update { snap ->
        snap.copy(decks = snap.decks.upsert(deck) { it.id == deck.id })
    }

    fun upsertDeckWithCards(deck: PFDeck, cards: List<PFFlashcard>) = update { snap ->
        snap.copy(
            decks = snap.decks.upsert(deck) { it.id == deck.id },
            cards = snap.cards.filterNot { it.deckId == deck.id } + cards,
        )
    }

    fun deleteDeck(id: String) = update { snap ->
        snap.copy(
            decks = snap.decks.filterNot { it.id == id },
            cards = snap.cards.filterNot { it.deckId == id },
            sessions = snap.sessions.filterNot { it.deckId == id },
        )
    }

    fun upsertCard(card: PFFlashcard) = update { snap ->
        snap.copy(cards = snap.cards.upsert(card) { it.id == card.id })
    }

    fun deleteCard(id: String) = update { snap ->
        snap.copy(cards = snap.cards.filterNot { it.id == id })
    }

    fun addSession(session: PFStudySession) = update { snap ->
        snap.copy(sessions = snap.sessions.upsert(session) { it.id == session.id })
    }

    fun upsertProject(project: PFProject) = update { snap ->
        snap.copy(projects = snap.projects.upsert(project) { it.id == project.id })
    }

    fun deleteProject(id: String) = update { snap ->
        snap.copy(
            projects = snap.projects.filterNot { it.id == id },
            members = snap.members.filterNot { it.projectId == id },
            decks = snap.decks.map { if (it.projectId == id) it.copy(projectId = null) else it },
        )
    }

    fun replaceMembers(projectId: String, members: List<PFProjectMember>) = update { snap ->
        snap.copy(members = snap.members.filterNot { it.projectId == projectId } + members)
    }

    fun removeMember(projectId: String, userId: String) = update { snap ->
        snap.copy(members = snap.members.filterNot { it.projectId == projectId && it.userId == userId })
    }

    fun replaceInvites(invites: List<PFProjectInvite>) = update { it.copy(invites = invites) }

    private companion object {
        const val TAG = "PaperflipDatabase"
    }
}

/** Replaces the first element matching [match], or appends when absent. */
internal fun <T> List<T>.upsert(value: T, match: (T) -> Boolean): List<T> {
    val index = indexOfFirst(match)
    return if (index >= 0) toMutableList().also { it[index] = value } else this + value
}
