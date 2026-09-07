package com.wapp.paperflipai.core.network

import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.FolderDto
import com.wapp.paperflipai.core.data.GenerationRequest
import com.wapp.paperflipai.core.data.NotificationDto
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.core.data.ProfileDto
import com.wapp.paperflipai.core.data.ProjectDto
import com.wapp.paperflipai.core.data.ProjectInviteDto
import com.wapp.paperflipai.core.data.ProjectJoinPreviewDto
import com.wapp.paperflipai.core.data.ProjectJoinRequestDto
import com.wapp.paperflipai.core.data.ProjectMemberDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Abstraction over the backend — the Android counterpart of
 * `RemoteStore.swift`. The rest of the app only ever talks to this
 * interface, so a real `SupabaseRemoteStore` can be dropped in without
 * touching a single screen.
 *
 * Ships with [MockRemoteStore] (realistic seed data, simulated latency) so
 * the whole app runs end-to-end with no backend configured — exactly how the
 * iOS target behaves.
 */
sealed class RemoteStoreError(message: String) : Exception(message) {
    data object NotAuthenticated : RemoteStoreError("You need to sign in first.")
    data object NotFound : RemoteStoreError("We couldn't find that.")
    data object RateLimited :
        RemoteStoreError("You're going a little fast — please try again in a moment.")

    class PlanLimitReached(message: String) : RemoteStoreError(message)
    class Network(detail: String) : RemoteStoreError("Network error: $detail")
    class Unknown(detail: String) : RemoteStoreError(detail)
}

/** Lightweight pull payload used by the sync engine on cold-start. */
data class RemoteSnapshot(
    val profile: ProfileDto? = null,
    val folders: List<FolderDto> = emptyList(),
    val decks: List<DeckDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val projectMembers: List<ProjectMemberDto> = emptyList(),
    val memberProfiles: Map<String, ProfileDto> = emptyMap(),
    val pendingInvites: List<ProjectInviteDto> = emptyList(),
)

/** Wire-format message for the support chat endpoint. */
@Serializable
data class SupportChatMessage(
    @kotlinx.serialization.Transient val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"

        fun user(content: String) = SupportChatMessage(role = ROLE_USER, content = content)
        fun assistant(content: String) = SupportChatMessage(role = ROLE_ASSISTANT, content = content)
    }

    val isUser: Boolean get() = role == ROLE_USER
}

data class ProjectMembersResult(
    val members: List<ProjectMemberDto>,
    val profiles: Map<String, ProfileDto>,
)

data class ProjectJoinRequestsResult(
    val requests: List<ProjectJoinRequestDto>,
    val profiles: Map<String, ProfileDto>,
)

interface RemoteStore {

    // ── Pull ──────────────────────────────────────────────────────────

    /** Fetch everything for the current user in one round-trip. */
    suspend fun snapshot(userId: String): RemoteSnapshot

    // ── Decks ─────────────────────────────────────────────────────────

    suspend fun createDeck(deck: DeckDto): DeckDto
    suspend fun updateDeck(deck: DeckDto): DeckDto
    suspend fun deleteDeck(id: String)

    /** Toggle a deck's `is_public` flag. Pro-only (server also enforces). */
    suspend fun setDeckPublic(deckId: String, isPublic: Boolean)

    /** Import a deck shared via paperflip.ai/share/{id}. */
    suspend fun importSharedDeck(link: String): DeckDto

    // ── Folders ───────────────────────────────────────────────────────

    suspend fun createFolder(folder: FolderDto): FolderDto
    suspend fun updateFolder(folder: FolderDto): FolderDto
    suspend fun deleteFolder(id: String)

    // ── AI generation ─────────────────────────────────────────────────

    /**
     * Calls the `generate-flashcards` Edge Function. The server returns a
     * freshly created deck with cards already generated.
     */
    suspend fun generateDeck(request: GenerationRequest, userId: String): DeckDto

    // ── Storage ───────────────────────────────────────────────────────

    /**
     * Uploads a PDF to the `pdfs` bucket under `{userId}/{uuid}.pdf` and
     * returns the storage path, which is passed back as `source_ref`.
     */
    suspend fun uploadPdf(bytes: ByteArray, fileName: String, userId: String): String

    /** Uploads an avatar to the public `avatars` bucket; returns its URL. */
    suspend fun uploadAvatar(bytes: ByteArray, contentType: String, userId: String): String

    // ── Support chat ──────────────────────────────────────────────────

    suspend fun supportChat(messages: List<SupportChatMessage>): String

    suspend fun flagSupportMessage(content: String, reason: String?, userId: String)

    // ── Projects ──────────────────────────────────────────────────────

    suspend fun fetchProjects(userId: String): List<ProjectDto>
    suspend fun createProject(name: String, description: String?, ownerUserId: String): ProjectDto
    suspend fun updateProject(project: ProjectDto): ProjectDto
    suspend fun deleteProject(id: String)

    suspend fun fetchProjectMembers(projectId: String): ProjectMembersResult

    suspend fun inviteProjectMember(projectId: String, email: String, role: PFProjectRole): ProjectInviteDto
    suspend fun revokeProjectInvite(inviteId: String)
    suspend fun resendProjectInvite(inviteId: String)
    suspend fun fetchProjectInvites(projectId: String): List<ProjectInviteDto>

    suspend fun acceptProjectInvite(inviteId: String)
    suspend fun declineProjectInvite(inviteId: String)
    suspend fun fetchPendingInvites(email: String): List<ProjectInviteDto>

    suspend fun setDeckProject(deckId: String, projectId: String?)
    suspend fun removeProjectMember(projectId: String, userId: String)
    suspend fun updateProjectMemberRole(projectId: String, userId: String, role: PFProjectRole)

    // ── Project join requests ─────────────────────────────────────────

    suspend fun previewProjectJoin(token: String): ProjectJoinPreviewDto
    suspend fun requestProjectJoin(
        token: String,
        message: String?,
        requestedRole: PFProjectRole,
    ): ProjectJoinRequestDto

    suspend fun fetchProjectJoinRequests(projectId: String): ProjectJoinRequestsResult
    suspend fun approveProjectJoinRequest(id: String)
    suspend fun declineProjectJoinRequest(id: String)

    // ── Notifications ─────────────────────────────────────────────────

    suspend fun fetchNotifications(userId: String): List<NotificationDto>
    suspend fun markNotificationRead(id: String)
    suspend fun markAllNotificationsRead(userId: String)
    suspend fun deleteNotification(id: String)
}
