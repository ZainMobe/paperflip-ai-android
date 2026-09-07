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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Live [RemoteStore] against Supabase — the Android counterpart of
 * `SupabaseRemoteStore.swift`, and a drop-in replacement for
 * [MockRemoteStore] behind the same interface.
 *
 * Split of responsibilities, matching iOS exactly so the two clients can't
 * drift on business rules:
 *  - **PostgREST** for plain CRUD, with RLS doing the authorisation.
 *  - **Edge Functions** for anything that needs privilege or side effects the
 *    client must not own: AI generation and its quota accounting, invite
 *    email + notification fan-out, join-request approval, shared-deck import.
 *
 * Nothing here enforces the free-tier cap. That is deliberate: the cap lives
 * in `generate-flashcards`, which owns the counter, and a client-side check
 * would be both duplicated and trivially bypassed.
 */
class SupabaseRemoteStore(
    private val client: SupabaseClient,
) : RemoteStore {

    private val json get() = client.json

    // ── Pull ──────────────────────────────────────────────────────────

    override suspend fun snapshot(userId: String): RemoteSnapshot = coroutineScope {
        // Three independent round-trips; iOS fires them concurrently with
        // async-let and so do we. Projects are pulled separately by
        // SyncEngine.pullProjects, which is why they are absent here.
        val folders = async { fetchFolders(userId) }
        val decks = async { fetchDecks(userId) }
        val profile = async { fetchProfile(userId) }
        RemoteSnapshot(
            profile = profile.await(),
            folders = folders.await(),
            decks = decks.await(),
        )
    }

    private suspend fun fetchFolders(userId: String): List<FolderDto> =
        decodeList(
            FolderDto.serializer(),
            client.rest("GET", "folders", "select=*&user_id=eq.$userId&order=created_at.asc"),
        )

    private suspend fun fetchDecks(userId: String): List<DeckDto> =
        decodeList(
            DeckDto.serializer(),
            client.rest("GET", "decks", "select=*&user_id=eq.$userId&order=updated_at.desc"),
        )

    private suspend fun fetchProfile(userId: String): ProfileDto? = runCatching {
        json.decodeFromString(
            ProfileDto.serializer(),
            client.rest("GET", "profiles", "select=*&id=eq.$userId", single = true),
        )
    }.getOrNull()

    // ── Decks ─────────────────────────────────────────────────────────

    override suspend fun createDeck(deck: DeckDto): DeckDto =
        json.decodeFromString(
            DeckDto.serializer(),
            client.rest(
                "POST", "decks",
                body = json.encodeToString(DeckDto.serializer(), deck),
                prefer = "return=representation",
                single = true,
            ),
        )

    override suspend fun updateDeck(deck: DeckDto): DeckDto =
        json.decodeFromString(
            DeckDto.serializer(),
            client.rest(
                "PATCH", "decks", "id=eq.${deck.id}",
                body = json.encodeToString(DeckDto.serializer(), deck),
                prefer = "return=representation",
                single = true,
            ),
        )

    override suspend fun deleteDeck(id: String) {
        client.rest("DELETE", "decks", "id=eq.$id")
    }

    override suspend fun setDeckPublic(deckId: String, isPublic: Boolean) {
        client.rest(
            "PATCH", "decks", "id=eq.$deckId",
            body = buildJsonObject { put("is_public", isPublic) }.toString(),
        )
    }

    override suspend fun setDeckProject(deckId: String, projectId: String?) {
        client.rest(
            "PATCH", "decks", "id=eq.$deckId",
            body = buildJsonObject {
                if (projectId == null) put("project_id", null as String?) else put("project_id", projectId)
            }.toString(),
        )
    }

    /** Edge Function owns plan-gating, de-duplication and the copy itself. */
    override suspend fun importSharedDeck(link: String): DeckDto {
        val body = client.invoke(
            "import-shared-deck",
            buildJsonObject { put("link", link) }.toString(),
        )
        throwIfError(body)
        val deckId = stringField(body, "deckId")
            ?: throw RemoteStoreError.Unknown("Server didn't return a deck id.")
        return json.decodeFromString(
            DeckDto.serializer(),
            client.rest("GET", "decks", "select=*&id=eq.$deckId", single = true),
        )
    }

    // ── Folders ───────────────────────────────────────────────────────

    override suspend fun createFolder(folder: FolderDto): FolderDto =
        json.decodeFromString(
            FolderDto.serializer(),
            client.rest(
                "POST", "folders",
                body = json.encodeToString(FolderDto.serializer(), folder),
                prefer = "return=representation",
                single = true,
            ),
        )

    override suspend fun updateFolder(folder: FolderDto): FolderDto =
        json.decodeFromString(
            FolderDto.serializer(),
            client.rest(
                "PATCH", "folders", "id=eq.${folder.id}",
                body = json.encodeToString(FolderDto.serializer(), folder),
                prefer = "return=representation",
                single = true,
            ),
        )

    override suspend fun deleteFolder(id: String) {
        client.rest("DELETE", "folders", "id=eq.$id")
    }

    // ── AI generation ─────────────────────────────────────────────────

    /**
     * `generate-flashcards` returns the deck row at the *top level* of the
     * response, with an optional `error` field alongside it — not nested
     * under a `deck` key. Same shape iOS decodes.
     */
    override suspend fun generateDeck(request: GenerationRequest, userId: String): DeckDto {
        val body = client.invoke(
            "generate-flashcards",
            json.encodeToString(GenerationRequest.serializer(), request),
        )
        throwIfError(body)
        return runCatching { json.decodeFromString(DeckDto.serializer(), body) }
            .getOrElse { throw RemoteStoreError.Unknown("No deck was returned by the server.") }
    }

    // ── Storage ───────────────────────────────────────────────────────

    override suspend fun uploadPdf(bytes: ByteArray, fileName: String, userId: String): String =
        client.storageUpload("pdfs", userId, bytes, "application/pdf", "pdf")

    override suspend fun uploadAvatar(bytes: ByteArray, contentType: String, userId: String): String {
        val ext = if (contentType.endsWith("png")) "png" else "jpg"
        val path = client.storageUpload("avatars", userId, bytes, contentType, ext)
        return client.publicUrl("avatars", path)
    }

    // ── Support chat ──────────────────────────────────────────────────

    override suspend fun supportChat(messages: List<SupportChatMessage>): String {
        val payload = buildString {
            append("{\"messages\":")
            append(json.encodeToString(ListSerializer(SupportChatMessage.serializer()), messages))
            append("}")
        }
        val body = client.invoke("support-chat", payload)
        val answer = stringField(body, "answer")
        if (!answer.isNullOrBlank()) return answer
        throw RemoteStoreError.Unknown(stringField(body, "error") ?: "Empty response")
    }

    override suspend fun flagSupportMessage(content: String, reason: String?, userId: String) {
        client.rest(
            "POST", "support_chat_flags",
            body = buildJsonObject {
                put("user_id", userId)
                put("message", content)
                if (reason == null) put("reason", null as String?) else put("reason", reason)
            }.toString(),
        )
    }

    // ── Projects ──────────────────────────────────────────────────────

    override suspend fun fetchProjects(userId: String): List<ProjectDto> {
        val memberRows = json.parseToJsonElement(
            client.rest("GET", "project_members", "select=project_id&user_id=eq.$userId")
        )
        val ids = runCatching {
            memberRows.jsonArrayOrEmpty().mapNotNull {
                it.jsonObject["project_id"]?.jsonPrimitive?.content
            }
        }.getOrDefault(emptyList())
        if (ids.isEmpty()) return emptyList()
        return decodeList(
            ProjectDto.serializer(),
            client.rest(
                "GET", "projects",
                "select=*&id=in.(${ids.joinToString(",")})&order=updated_at.desc",
            ),
        )
    }

    /**
     * Three steps, and the order matters. The SELECT policy on `projects`
     * requires membership, so a `return=representation` insert would write
     * the row and then fail to read it back. Instead: mint the id locally,
     * insert, add the owner's membership row, then read.
     */
    override suspend fun createProject(
        name: String,
        description: String?,
        ownerUserId: String,
    ): ProjectDto {
        val id = UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString().replace("-", "").take(24)

        client.rest(
            "POST", "projects",
            body = buildJsonObject {
                put("id", id)
                put("owner_user_id", ownerUserId)
                put("name", name)
                if (description == null) put("description", null as String?) else put("description", description)
                put("join_link_token", token)
                put("join_link_enabled", false)
            }.toString(),
        )
        client.rest(
            "POST", "project_members",
            body = buildJsonObject {
                put("project_id", id)
                put("user_id", ownerUserId)
                put("role", PFProjectRole.Owner.raw)
            }.toString(),
        )
        return json.decodeFromString(
            ProjectDto.serializer(),
            client.rest("GET", "projects", "select=*&id=eq.$id", single = true),
        )
    }

    override suspend fun updateProject(project: ProjectDto): ProjectDto =
        json.decodeFromString(
            ProjectDto.serializer(),
            client.rest(
                "PATCH", "projects", "id=eq.${project.id}",
                body = json.encodeToString(ProjectDto.serializer(), project),
                prefer = "return=representation",
                single = true,
            ),
        )

    override suspend fun deleteProject(id: String) {
        client.rest("DELETE", "projects", "id=eq.$id")
    }

    override suspend fun fetchProjectMembers(projectId: String): ProjectMembersResult {
        val members = decodeList(
            ProjectMemberDto.serializer(),
            client.rest("GET", "project_members", "select=*&project_id=eq.$projectId"),
        )
        return ProjectMembersResult(members, profilesFor(members.map { it.userId }))
    }

    override suspend fun removeProjectMember(projectId: String, userId: String) {
        client.rest("DELETE", "project_members", "project_id=eq.$projectId&user_id=eq.$userId")
    }

    override suspend fun updateProjectMemberRole(
        projectId: String,
        userId: String,
        role: PFProjectRole,
    ) {
        client.rest(
            "PATCH", "project_members", "project_id=eq.$projectId&user_id=eq.$userId",
            body = buildJsonObject { put("role", role.raw) }.toString(),
        )
    }

    // ── Invites ───────────────────────────────────────────────────────

    /**
     * Goes through `project-invite-send` rather than inserting directly, so
     * the Resend email, the in-app notification and the RLS-safe insert are
     * the same flow the web app uses.
     */
    override suspend fun inviteProjectMember(
        projectId: String,
        email: String,
        role: PFProjectRole,
    ): ProjectInviteDto {
        val body = client.invoke(
            "project-invite-send",
            buildJsonObject {
                put("project_id", projectId)
                put("email", email.lowercase())
                put("role", role.raw)
            }.toString(),
        )
        throwIfError(body)
        if (boolField(body, "alreadyMember") == true) {
            throw RemoteStoreError.Unknown(
                stringField(body, "message") ?: "This user is already part of the project."
            )
        }
        val invite = objectField(body, "invite")
            ?: throw RemoteStoreError.Unknown("Couldn't send invite (no invite returned).")
        return json.decodeFromString(ProjectInviteDto.serializer(), invite.toString())
    }

    override suspend fun revokeProjectInvite(inviteId: String) {
        client.rest(
            "PATCH", "project_invites", "id=eq.$inviteId",
            body = buildJsonObject { put("status", "revoked") }.toString(),
        )
    }

    /** No email service on this path yet — bump `updated_at` and reset expiry. */
    override suspend fun resendProjectInvite(inviteId: String) {
        val now = Instant.now()
        client.rest(
            "PATCH", "project_invites", "id=eq.$inviteId",
            body = buildJsonObject {
                put("updated_at", now.toString())
                put("expires_at", now.plus(7, ChronoUnit.DAYS).toString())
                put("status", "pending")
            }.toString(),
        )
    }

    override suspend fun fetchProjectInvites(projectId: String): List<ProjectInviteDto> =
        decodeList(
            ProjectInviteDto.serializer(),
            client.rest(
                "GET", "project_invites",
                "select=*&project_id=eq.$projectId&order=created_at.desc",
            ),
        )

    override suspend fun fetchPendingInvites(email: String): List<ProjectInviteDto> =
        decodeList(
            ProjectInviteDto.serializer(),
            client.rest(
                "GET", "project_invites",
                "select=*&invited_email=eq.${SupabaseClient.enc(email.lowercase())}" +
                    "&status=eq.pending&order=created_at.desc",
            ),
        )

    override suspend fun acceptProjectInvite(inviteId: String) = respondToInvite(inviteId, "accept")

    override suspend fun declineProjectInvite(inviteId: String) = respondToInvite(inviteId, "decline")

    private suspend fun respondToInvite(inviteId: String, action: String) {
        throwIfError(
            client.invoke(
                "project-invite-respond",
                buildJsonObject {
                    put("invite_id", inviteId)
                    put("action", action)
                }.toString(),
            )
        )
    }

    // ── Join requests ─────────────────────────────────────────────────

    override suspend fun previewProjectJoin(token: String): ProjectJoinPreviewDto {
        val body = client.invoke(
            "project-join",
            buildJsonObject {
                put("action", "preview")
                put("token", token)
            }.toString(),
        )
        throwIfError(body)
        val project = objectField(body, "project") ?: throw RemoteStoreError.NotFound
        return ProjectJoinPreviewDto(
            project = json.decodeFromString(
                ProjectJoinPreviewDto.PreviewProject.serializer(), project.toString()
            ),
            alreadyMember = boolField(body, "alreadyMember") ?: false,
            alreadyPending = boolField(body, "alreadyPending") ?: false,
            pendingRequestId = stringField(body, "pendingRequestId"),
            isPro = boolField(body, "isPro") ?: false,
        )
    }

    override suspend fun requestProjectJoin(
        token: String,
        message: String?,
        requestedRole: PFProjectRole,
    ): ProjectJoinRequestDto {
        val body = client.invoke(
            "project-join",
            buildJsonObject {
                put("action", "request")
                put("token", token)
                if (message == null) put("message", null as String?) else put("message", message)
                put("requested_role", requestedRole.raw)
            }.toString(),
        )
        throwIfError(body)
        if (boolField(body, "alreadyMember") == true) {
            throw RemoteStoreError.Unknown("You're already a member of this project.")
        }
        val request = objectField(body, "request")
            ?: throw RemoteStoreError.Unknown("Couldn't submit join request.")
        return json.decodeFromString(ProjectJoinRequestDto.serializer(), request.toString())
    }

    override suspend fun fetchProjectJoinRequests(projectId: String): ProjectJoinRequestsResult {
        val requests = decodeList(
            ProjectJoinRequestDto.serializer(),
            client.rest(
                "GET", "project_join_requests",
                "select=*&project_id=eq.$projectId&status=eq.pending&order=created_at.desc",
            ),
        )
        return ProjectJoinRequestsResult(requests, profilesFor(requests.map { it.requesterUserId }))
    }

    override suspend fun approveProjectJoinRequest(id: String) = respondToJoinRequest(id, "approve")

    override suspend fun declineProjectJoinRequest(id: String) = respondToJoinRequest(id, "decline")

    private suspend fun respondToJoinRequest(id: String, decision: String) {
        throwIfError(
            client.invoke(
                "project-join",
                buildJsonObject {
                    put("action", "respond")
                    put("request_id", id)
                    put("decision", decision)
                }.toString(),
            )
        )
    }

    // ── Notifications ─────────────────────────────────────────────────

    override suspend fun fetchNotifications(userId: String): List<NotificationDto> =
        decodeList(
            NotificationDto.serializer(),
            client.rest(
                "GET", "notifications",
                "select=*&user_id=eq.$userId&order=created_at.desc&limit=50",
            ),
        )

    override suspend fun markNotificationRead(id: String) {
        client.rest(
            "PATCH", "notifications", "id=eq.$id",
            body = buildJsonObject { put("read", true) }.toString(),
        )
    }

    override suspend fun markAllNotificationsRead(userId: String) {
        client.rest(
            "PATCH", "notifications", "user_id=eq.$userId&read=eq.false",
            body = buildJsonObject { put("read", true) }.toString(),
        )
    }

    override suspend fun deleteNotification(id: String) {
        client.rest("DELETE", "notifications", "id=eq.$id")
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private suspend fun profilesFor(userIds: List<String>): Map<String, ProfileDto> {
        val ids = userIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyMap()
        val profiles = decodeList(
            ProfileDto.serializer(),
            client.rest("GET", "profiles", "select=*&id=in.(${ids.joinToString(",")})"),
        )
        return profiles.associateBy { it.id }
    }

    private fun <T> decodeList(
        serializer: kotlinx.serialization.KSerializer<T>,
        body: String,
    ): List<T> = json.decodeFromString(ListSerializer(serializer), body)

    /** Edge Functions answer 200 with an `error` field rather than a 4xx. */
    private fun throwIfError(body: String) {
        stringField(body, "error")?.takeIf { it.isNotBlank() }?.let {
            throw RemoteStoreError.Unknown(it)
        }
    }

    private fun stringField(body: String, name: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.content
    }.getOrNull()?.takeIf { it != "null" }

    private fun boolField(body: String, name: String): Boolean? = runCatching {
        json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
    }.getOrNull()

    private fun objectField(body: String, name: String): JsonObject? = runCatching {
        json.parseToJsonElement(body).jsonObject[name]?.jsonObject
    }.getOrNull()

    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrEmpty() =
        (this as? kotlinx.serialization.json.JsonArray) ?: kotlinx.serialization.json.JsonArray(emptyList())
}
