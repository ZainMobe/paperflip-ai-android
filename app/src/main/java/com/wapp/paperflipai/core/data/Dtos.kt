package com.wapp.paperflipai.core.data

import com.wapp.paperflipai.designsystem.theme.PFGenerationLanguage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Wire-format models — the Android counterpart of `DTOs.swift`.
 *
 * These mirror the live Supabase schema exactly (snake_case columns,
 * cards-as-JSONB inside deck) so PostgREST and Edge Function payloads
 * decode with no fuss. Kotlin property names stay friendly; @SerialName
 * maps them to the real column names.
 */

/** Encodes epoch millis as the ISO-8601 timestamps PostgREST returns. */
object IsoInstantSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IsoInstant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(value)))
    }

    override fun deserialize(decoder: Decoder): Long = parse(decoder.decodeString())

    fun parse(text: String): Long = runCatching {
        OffsetDateTime.parse(text).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(text).toEpochMilli()
    }.recoverCatching {
        LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli()
    }.getOrDefault(0L)
}

private fun now() = System.currentTimeMillis()

// MARK: - Profile

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val plan: String = PFPlan.Free.raw,
    @SerialName("stripe_customer_id") val stripeCustomerId: String? = null,
    @SerialName("decks_generated_this_month") val decksGeneratedThisMonth: Int = 0,
    @SerialName("generation_reset_at")
    @Serializable(with = IsoInstantSerializer::class) val generationResetAt: Long = now(),
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
    @SerialName("updated_at")
    @Serializable(with = IsoInstantSerializer::class) val updatedAt: Long = now(),
) {
    fun toModel() = PFProfile(
        id = id,
        email = email,
        fullName = fullName,
        avatarUrl = avatarUrl,
        plan = PFPlan.from(plan),
        stripeCustomerId = stripeCustomerId,
        decksGeneratedThisMonth = decksGeneratedThisMonth,
        generationResetAt = generationResetAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PFProfile.toDto() = ProfileDto(
    id = id,
    email = email,
    fullName = fullName,
    avatarUrl = avatarUrl,
    plan = plan.raw,
    stripeCustomerId = stripeCustomerId,
    decksGeneratedThisMonth = decksGeneratedThisMonth,
    generationResetAt = generationResetAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// MARK: - Folder

@Serializable
data class FolderDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val color: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
    @SerialName("updated_at")
    @Serializable(with = IsoInstantSerializer::class) val updatedAt: Long = now(),
) {
    fun toModel() = PFFolder(
        id = id, userId = userId, name = name, colorHex = color,
        createdAt = createdAt, updatedAt = updatedAt,
    )
}

fun PFFolder.toDto() = FolderDto(
    id = id, userId = userId, name = name, color = colorHex,
    projectId = null, createdAt = createdAt, updatedAt = updatedAt,
)

// MARK: - Card (nested inside DeckDto's `cards` JSONB column)

@Serializable
data class CardDto(
    val id: String? = null,
    val front: String,
    val back: String,
    val hint: String? = null,
)

// MARK: - Deck

@Serializable
data class DeckDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("folder_id") val folderId: String? = null,
    val title: String,
    /** Server column is `description`. */
    @SerialName("description") val summary: String? = null,
    @SerialName("source_type") val sourceType: String,
    /** Server column is `source_url`. */
    @SerialName("source_url") val sourceRef: String? = null,
    @SerialName("source_title") val sourceTitle: String? = null,
    val cards: List<CardDto> = emptyList(),
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
    @SerialName("updated_at")
    @Serializable(with = IsoInstantSerializer::class) val updatedAt: Long = now(),
) {
    fun toDeck() = PFDeck(
        id = id,
        userId = userId,
        title = title,
        summary = summary,
        sourceType = PFSourceType.from(sourceType),
        sourceRef = sourceRef,
        isPublic = isPublic,
        folderId = folderId,
        projectId = projectId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /** Explodes the JSONB card array into individual local rows. */
    fun toCards(): List<PFFlashcard> = cards.mapIndexed { index, card ->
        PFFlashcard(
            id = card.id ?: newId(),
            deckId = id,
            front = card.front,
            back = card.back,
            hint = card.hint,
            orderIndex = index,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}

fun PFDeck.toDto(cards: List<PFFlashcard>) = DeckDto(
    id = id,
    userId = userId,
    folderId = folderId,
    title = title,
    summary = summary,
    sourceType = sourceType.raw,
    sourceRef = sourceRef,
    sourceTitle = null,
    cards = cards.sortedBy { it.orderIndex }
        .map { CardDto(id = it.id, front = it.front, back = it.back, hint = it.hint) },
    isPublic = isPublic,
    projectId = projectId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// MARK: - Project DTOs

@Serializable
data class ProjectDto(
    val id: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    val name: String,
    val description: String? = null,
    @SerialName("join_link_token") val joinLinkToken: String = "",
    @SerialName("join_link_enabled") val joinLinkEnabled: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
    @SerialName("updated_at")
    @Serializable(with = IsoInstantSerializer::class) val updatedAt: Long = now(),
) {
    fun toModel() = PFProject(
        id = id, ownerUserId = ownerUserId, name = name, description = description,
        joinLinkToken = joinLinkToken, joinLinkEnabled = joinLinkEnabled,
        createdAt = createdAt, updatedAt = updatedAt,
    )
}

@Serializable
data class ProjectMemberDto(
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("joined_at")
    @Serializable(with = IsoInstantSerializer::class) val joinedAt: Long = now(),
    @SerialName("invited_by_user_id") val invitedByUserId: String? = null,
) {
    fun toModel(profile: ProfileDto? = null) = PFProjectMember(
        id = PFProjectMember.idFor(projectId, userId),
        projectId = projectId,
        userId = userId,
        role = PFProjectRole.from(role),
        joinedAt = joinedAt,
        invitedByUserId = invitedByUserId,
        displayName = profile?.fullName,
        avatarUrl = profile?.avatarUrl,
        email = profile?.email,
    )
}

@Serializable
data class ProjectInviteDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("invited_email") val invitedEmail: String,
    val role: String,
    val status: String,
    val token: String,
    @SerialName("invited_by_user_id") val invitedByUserId: String,
    @SerialName("accepted_by_user_id") val acceptedByUserId: String? = null,
    @SerialName("expires_at")
    @Serializable(with = IsoInstantSerializer::class) val expiresAt: Long = now(),
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
    @SerialName("updated_at")
    @Serializable(with = IsoInstantSerializer::class) val updatedAt: Long = now(),
    /** Denormalised for the invites inbox; not a real column. */
    @SerialName("project_name") val projectName: String? = null,
    @SerialName("inviter_name") val inviterName: String? = null,
) {
    fun toModel() = PFProjectInvite(
        id = id, projectId = projectId, invitedEmail = invitedEmail,
        role = PFProjectRole.from(role), status = PFProjectInviteStatus.from(status),
        token = token, invitedByUserId = invitedByUserId, expiresAt = expiresAt,
        createdAt = createdAt, updatedAt = updatedAt,
        projectName = projectName, inviterName = inviterName,
    )
}

@Serializable
data class ProjectJoinRequestDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("requester_user_id") val requesterUserId: String,
    @SerialName("requested_role") val requestedRole: String,
    val status: String,
    val message: String? = null,
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
    @SerialName("reviewed_at")
    @Serializable(with = IsoInstantSerializer::class) val reviewedAt: Long? = null,
    @SerialName("reviewed_by_user_id") val reviewedByUserId: String? = null,
)

@Serializable
data class ProjectJoinPreviewDto(
    val project: PreviewProject,
    val alreadyMember: Boolean = false,
    val alreadyPending: Boolean = false,
    val pendingRequestId: String? = null,
    val isPro: Boolean = false,
) {
    @Serializable
    data class PreviewProject(
        val id: String,
        val name: String,
        val description: String? = null,
        @SerialName("owner_user_id") val ownerUserId: String,
        @SerialName("join_link_enabled") val joinLinkEnabled: Boolean = true,
        val ownerName: String? = null,
    )
}

// MARK: - Notification

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val link: String? = null,
    val read: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Long = now(),
)

// MARK: - Generation request

/**
 * Sent to the `generate-flashcards` Edge Function.
 *
 * `targetCardCount` is always sent; when `autoCardCount` is true the Edge
 * Function picks a count from the source's length, capped at this value (so
 * free-tier limits still hold). `language` is always sent as a fallback —
 * when `useSourceLanguage` is true the function tries to detect the source's
 * language first.
 */
@Serializable
data class GenerationRequest(
    @SerialName("source_type") val sourceType: String,
    @SerialName("source_ref") val sourceRef: String,
    @SerialName("target_card_count") val targetCardCount: Int,
    @SerialName("auto_card_count") val autoCardCount: Boolean,
    val language: String,
    @SerialName("use_source_language") val useSourceLanguage: Boolean,
) {
    companion object {
        fun of(
            sourceType: PFSourceType,
            sourceRef: String,
            targetCardCount: Int,
            autoCardCount: Boolean,
            language: PFGenerationLanguage,
            useSourceLanguage: Boolean,
        ) = GenerationRequest(
            sourceType = sourceType.raw,
            sourceRef = sourceRef,
            targetCardCount = targetCardCount,
            autoCardCount = autoCardCount,
            language = language.rawValue,
            useSourceLanguage = useSourceLanguage,
        )
    }
}
