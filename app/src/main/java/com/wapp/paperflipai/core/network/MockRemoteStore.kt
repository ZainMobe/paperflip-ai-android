package com.wapp.paperflipai.core.network

import com.wapp.paperflipai.core.data.CardDto
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.FolderDto
import com.wapp.paperflipai.core.data.GenerationRequest
import com.wapp.paperflipai.core.data.NotificationDto
import com.wapp.paperflipai.core.data.PFProjectInviteStatus
import com.wapp.paperflipai.core.data.PFProjectJoinRequestStatus
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.core.data.ProfileDto
import com.wapp.paperflipai.core.data.ProjectDto
import com.wapp.paperflipai.core.data.ProjectInviteDto
import com.wapp.paperflipai.core.data.ProjectJoinPreviewDto
import com.wapp.paperflipai.core.data.ProjectJoinRequestDto
import com.wapp.paperflipai.core.data.ProjectMemberDto
import com.wapp.paperflipai.core.data.newId
import com.wapp.paperflipai.designsystem.theme.PFGenerationLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [RemoteStore] — the Android counterpart of
 * `MockRemoteStore.swift`. Returns realistic seed data so Library, Study and
 * Import work end-to-end with no backend configured. Swap in a
 * `SupabaseRemoteStore` and every screen keeps working unchanged.
 */
class MockRemoteStore(private val seed: Boolean = true) : RemoteStore {

    /** Simulated network latency. */
    var latencyMillis: Long = 450

    /** Set to make the next call throw a network error (for testing). */
    var shouldFailNextCall: Boolean = false

    private val mutex = Mutex()
    private val folders = LinkedHashMap<String, FolderDto>()
    private val decks = LinkedHashMap<String, DeckDto>()
    private val projects = LinkedHashMap<String, ProjectDto>()
    private val members = ArrayList<ProjectMemberDto>()
    private val invites = LinkedHashMap<String, ProjectInviteDto>()
    private val notifications = LinkedHashMap<String, NotificationDto>()
    private var seeded = false

    // ── Pull ──────────────────────────────────────────────────────────

    override suspend fun snapshot(userId: String): RemoteSnapshot {
        simulate()
        mutex.withLock {
            if (seed && !seeded) {
                populate(userId)
                seeded = true
            }
            return RemoteSnapshot(
                profile = null, // auth owns the profile; the sync engine tolerates null
                folders = folders.values.toList(),
                decks = decks.values.toList(),
                projects = projects.values.toList(),
                projectMembers = members.toList(),
                pendingInvites = invites.values.filter { it.status == PFProjectInviteStatus.Pending.raw },
            )
        }
    }

    // ── Decks ─────────────────────────────────────────────────────────

    override suspend fun createDeck(deck: DeckDto): DeckDto {
        simulate()
        mutex.withLock { decks[deck.id] = deck }
        return deck
    }

    override suspend fun updateDeck(deck: DeckDto): DeckDto {
        simulate()
        mutex.withLock {
            if (!decks.containsKey(deck.id)) throw RemoteStoreError.NotFound
            decks[deck.id] = deck
        }
        return deck
    }

    override suspend fun deleteDeck(id: String) {
        simulate()
        mutex.withLock { decks.remove(id) }
    }

    override suspend fun setDeckPublic(deckId: String, isPublic: Boolean) {
        simulate()
        mutex.withLock {
            val existing = decks[deckId] ?: throw RemoteStoreError.NotFound
            decks[deckId] = existing.copy(isPublic = isPublic, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun importSharedDeck(link: String): DeckDto {
        simulate()
        return DeckDto(
            id = newId(),
            userId = newId(),
            title = "Imported sample deck",
            summary = "From a shared link (mock).",
            sourceType = PFSourceType.Article.raw,
            sourceRef = link,
            cards = listOf(CardDto(id = newId(), front = "Sample shared question?", back = "Sample shared answer.")),
        )
    }

    // ── Folders ───────────────────────────────────────────────────────

    override suspend fun createFolder(folder: FolderDto): FolderDto {
        simulate()
        mutex.withLock { folders[folder.id] = folder }
        return folder
    }

    override suspend fun updateFolder(folder: FolderDto): FolderDto {
        simulate()
        mutex.withLock {
            if (!folders.containsKey(folder.id)) throw RemoteStoreError.NotFound
            folders[folder.id] = folder
        }
        return folder
    }

    override suspend fun deleteFolder(id: String) {
        simulate()
        mutex.withLock { folders.remove(id) }
    }

    // ── AI generation ─────────────────────────────────────────────────

    override suspend fun generateDeck(request: GenerationRequest, userId: String): DeckDto {
        // Deliberately slow — generation is where the user expects a wait,
        // and the Import screen's progress ring is built around it.
        delay(2400)
        if (shouldFailNextCall) {
            shouldFailNextCall = false
            throw RemoteStoreError.Network("Couldn't reach the AI right now.")
        }
        val sourceType = PFSourceType.from(request.sourceType)
        val language = PFGenerationLanguage.from(request.language) ?: PFGenerationLanguage.En
        val deck = DeckDto(
            id = newId(),
            userId = userId,
            title = guessTitle(sourceType),
            summary = "Generated from ${sourceType.raw} in ${language.displayName}.",
            sourceType = sourceType.raw,
            sourceRef = request.sourceRef,
            cards = fakeCards(request.targetCardCount.coerceIn(6, 20)),
        )
        mutex.withLock { decks[deck.id] = deck }
        return deck
    }

    // ── Storage ───────────────────────────────────────────────────────

    override suspend fun uploadPdf(bytes: ByteArray, fileName: String, userId: String): String {
        simulate()
        return "$userId/${newId()}.pdf"
    }

    override suspend fun uploadAvatar(bytes: ByteArray, contentType: String, userId: String): String {
        simulate()
        return "https://example.com/avatars/$userId/${newId()}.jpg"
    }

    // ── Support chat ──────────────────────────────────────────────────

    override suspend fun supportChat(messages: List<SupportChatMessage>): String {
        delay(1200)
        if (shouldFailNextCall) {
            shouldFailNextCall = false
            throw RemoteStoreError.Network("Mock support failure.")
        }
        return "Thanks for reaching out! (This is a mock reply — set SUPABASE_URL and " +
            "SUPABASE_ANON_KEY in local.properties to hit the real support-chat Edge Function.)"
    }

    override suspend fun flagSupportMessage(content: String, reason: String?, userId: String) {
        simulate()
    }

    // ── Projects ──────────────────────────────────────────────────────

    override suspend fun fetchProjects(userId: String): List<ProjectDto> {
        simulate()
        return mutex.withLock { projects.values.toList() }
    }

    override suspend fun createProject(name: String, description: String?, ownerUserId: String): ProjectDto {
        simulate()
        val project = ProjectDto(
            id = newId(),
            ownerUserId = ownerUserId,
            name = name,
            description = description,
            joinLinkToken = newId(),
            joinLinkEnabled = false,
        )
        mutex.withLock {
            projects[project.id] = project
            members.add(
                ProjectMemberDto(
                    projectId = project.id,
                    userId = ownerUserId,
                    role = PFProjectRole.Owner.raw,
                )
            )
        }
        return project
    }

    override suspend fun updateProject(project: ProjectDto): ProjectDto {
        simulate()
        mutex.withLock { projects[project.id] = project }
        return project
    }

    override suspend fun deleteProject(id: String) {
        simulate()
        mutex.withLock {
            projects.remove(id)
            members.removeAll { it.projectId == id }
        }
    }

    override suspend fun fetchProjectMembers(projectId: String): ProjectMembersResult {
        simulate()
        return mutex.withLock {
            ProjectMembersResult(
                members = members.filter { it.projectId == projectId },
                profiles = emptyMap(),
            )
        }
    }

    override suspend fun inviteProjectMember(
        projectId: String,
        email: String,
        role: PFProjectRole,
    ): ProjectInviteDto {
        simulate()
        val invite = ProjectInviteDto(
            id = newId(),
            projectId = projectId,
            invitedEmail = email,
            role = role.raw,
            status = PFProjectInviteStatus.Pending.raw,
            token = newId(),
            invitedByUserId = projects[projectId]?.ownerUserId ?: newId(),
            expiresAt = System.currentTimeMillis() + 7 * 86_400_000L,
        )
        mutex.withLock { invites[invite.id] = invite }
        return invite
    }

    override suspend fun revokeProjectInvite(inviteId: String) {
        simulate()
        mutex.withLock {
            invites[inviteId]?.let { invites[it.id] = it.copy(status = PFProjectInviteStatus.Revoked.raw) }
        }
    }

    override suspend fun resendProjectInvite(inviteId: String) = simulate()

    override suspend fun fetchProjectInvites(projectId: String): List<ProjectInviteDto> {
        simulate()
        return mutex.withLock { invites.values.filter { it.projectId == projectId } }
    }

    override suspend fun acceptProjectInvite(inviteId: String) {
        simulate()
        mutex.withLock {
            invites[inviteId]?.let { invites[it.id] = it.copy(status = PFProjectInviteStatus.Accepted.raw) }
        }
    }

    override suspend fun declineProjectInvite(inviteId: String) {
        simulate()
        mutex.withLock {
            invites[inviteId]?.let { invites[it.id] = it.copy(status = PFProjectInviteStatus.Declined.raw) }
        }
    }

    override suspend fun fetchPendingInvites(email: String): List<ProjectInviteDto> {
        simulate()
        return mutex.withLock {
            invites.values.filter {
                it.invitedEmail.equals(email, ignoreCase = true) &&
                    it.status == PFProjectInviteStatus.Pending.raw
            }
        }
    }

    override suspend fun setDeckProject(deckId: String, projectId: String?) {
        simulate()
        mutex.withLock {
            decks[deckId]?.let { decks[deckId] = it.copy(projectId = projectId, updatedAt = System.currentTimeMillis()) }
        }
    }

    override suspend fun removeProjectMember(projectId: String, userId: String) {
        simulate()
        mutex.withLock { members.removeAll { it.projectId == projectId && it.userId == userId } }
    }

    override suspend fun updateProjectMemberRole(projectId: String, userId: String, role: PFProjectRole) {
        simulate()
        mutex.withLock {
            val index = members.indexOfFirst { it.projectId == projectId && it.userId == userId }
            if (index >= 0) members[index] = members[index].copy(role = role.raw)
        }
    }

    // ── Project join requests ─────────────────────────────────────────

    override suspend fun previewProjectJoin(token: String): ProjectJoinPreviewDto {
        simulate()
        val project = mutex.withLock { projects.values.firstOrNull { it.joinLinkToken == token } }
        return ProjectJoinPreviewDto(
            project = ProjectJoinPreviewDto.PreviewProject(
                id = project?.id ?: newId(),
                name = project?.name ?: "Sample project",
                description = project?.description ?: "Mock preview — no real backend.",
                ownerUserId = project?.ownerUserId ?: newId(),
                joinLinkEnabled = true,
                ownerName = "A teammate",
            ),
            alreadyMember = false,
            alreadyPending = false,
            pendingRequestId = null,
            isPro = true,
        )
    }

    override suspend fun requestProjectJoin(
        token: String,
        message: String?,
        requestedRole: PFProjectRole,
    ): ProjectJoinRequestDto {
        simulate()
        return ProjectJoinRequestDto(
            id = newId(),
            projectId = newId(),
            requesterUserId = newId(),
            requestedRole = requestedRole.raw,
            status = PFProjectJoinRequestStatus.Pending.raw,
            message = message,
        )
    }

    override suspend fun fetchProjectJoinRequests(projectId: String): ProjectJoinRequestsResult {
        simulate()
        return ProjectJoinRequestsResult(emptyList(), emptyMap())
    }

    override suspend fun approveProjectJoinRequest(id: String) = simulate()
    override suspend fun declineProjectJoinRequest(id: String) = simulate()

    // ── Notifications ─────────────────────────────────────────────────

    override suspend fun fetchNotifications(userId: String): List<NotificationDto> {
        simulate()
        return mutex.withLock { notifications.values.sortedByDescending { it.createdAt } }
    }

    override suspend fun markNotificationRead(id: String) {
        simulate()
        mutex.withLock { notifications[id]?.let { notifications[id] = it.copy(read = true) } }
    }

    override suspend fun markAllNotificationsRead(userId: String) {
        simulate()
        mutex.withLock {
            notifications.keys.toList().forEach { key ->
                notifications[key]?.let { notifications[key] = it.copy(read = true) }
            }
        }
    }

    override suspend fun deleteNotification(id: String) {
        simulate()
        mutex.withLock { notifications.remove(id) }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private suspend fun simulate() {
        delay(latencyMillis)
        if (shouldFailNextCall) {
            shouldFailNextCall = false
            throw RemoteStoreError.Network("Simulated failure.")
        }
    }

    private fun guessTitle(sourceType: PFSourceType) = when (sourceType) {
        PFSourceType.Pdf -> "New deck from PDF"
        PFSourceType.Youtube -> "New deck from YouTube"
        PFSourceType.Article -> "New deck from article"
    }

    private fun fakeCards(count: Int): List<CardDto> {
        val pool = listOf(
            "Sample question 1?" to "Sample answer 1.",
            "Sample question 2?" to "Sample answer 2.",
            "Sample question 3?" to "Sample answer 3.",
            "Define X" to "X is defined as Y.",
            "List 3 properties of Z" to "Property A, property B, property C.",
            "What does Q stand for?" to "Q stands for the QQQ thing.",
            "Compare A and B" to "A is more X; B is more Y.",
            "Why does P happen?" to "Because of underlying mechanism M.",
        )
        return (0 until count).map { i ->
            val (q, a) = pool[i % pool.size]
            CardDto(id = newId(), front = q, back = a)
        }
    }

    // ── Seed data ─────────────────────────────────────────────────────

    private fun populate(userId: String) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val school = FolderDto(
            id = newId(), userId = userId, name = "School", color = "#5B5BD6",
            createdAt = now, updatedAt = now,
        )
        val work = FolderDto(
            id = newId(), userId = userId, name = "Work", color = "#16A34A",
            createdAt = now, updatedAt = now,
        )
        folders[school.id] = school
        folders[work.id] = work

        val deck1 = DeckDto(
            id = newId(), userId = userId, folderId = school.id,
            title = "Atomic Habits — Ch. 1",
            summary = "The fundamentals of habit formation.",
            sourceType = PFSourceType.Pdf.raw,
            sourceRef = "atomic-habits.pdf",
            cards = listOf(
                CardDto(newId(), "What is a habit?", "A behavior repeated enough to become automatic."),
                CardDto(newId(), "The 4-step habit loop?", "Cue → craving → response → reward."),
                CardDto(newId(), "Two-minute rule?", "Scale a new habit down to a version that takes ≤ 2 minutes.", "implementation"),
                CardDto(newId(), "Identity-based habits?", "Habits derived from who you want to be, not what you want to achieve."),
            ),
            createdAt = now, updatedAt = now,
        )

        val deck2 = DeckDto(
            id = newId(), userId = userId,
            title = "How synapses form",
            summary = "From a 12-min Veritasium explainer.",
            sourceType = PFSourceType.Youtube.raw,
            sourceRef = "https://youtu.be/sample",
            cards = listOf(
                CardDto(newId(), "What is a synapse?", "The junction between two neurons where signals pass."),
                CardDto(newId(), "Long-term potentiation (LTP)?", "Persistent strengthening of synapses, the basis of learning."),
                CardDto(newId(), "Hebbian principle?", "Neurons that fire together, wire together."),
            ),
            createdAt = now - day, updatedAt = now - day,
        )

        val deck3 = DeckDto(
            id = newId(), userId = userId, folderId = work.id,
            title = "TLS handshake basics",
            summary = "From the Cloudflare blog.",
            sourceType = PFSourceType.Article.raw,
            sourceRef = "https://blog.cloudflare.com/tls",
            cards = listOf(
                CardDto(newId(), "What is TLS?", "Transport Layer Security — encrypts data in transit."),
                CardDto(newId(), "TLS handshake key exchange?", "Asymmetric crypto used to agree on a shared symmetric key."),
                CardDto(newId(), "What's a cipher suite?", "A set of algorithms (key exchange, bulk cipher, MAC) used together."),
            ),
            createdAt = now - 3 * day, updatedAt = now - 3 * day,
        )

        listOf(deck1, deck2, deck3).forEach { decks[it.id] = it }
    }
}
