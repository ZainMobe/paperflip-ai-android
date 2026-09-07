package com.wapp.paperflipai.core.data

import kotlinx.serialization.Serializable
import java.util.UUID

/*
 * Local domain models — the Android counterpart of `Models.swift`.
 *
 * iOS stores these in SwiftData; here they are immutable data classes held
 * by [PaperflipDatabase] and persisted as one JSON snapshot. Relationships
 * are expressed as ids rather than object references, which keeps the
 * models serialisable and makes every update a pure copy().
 *
 * Timestamps are epoch milliseconds throughout; the DTO layer converts to
 * and from the ISO-8601 strings PostgREST speaks.
 */

// MARK: - Enums (mirror the Supabase enums)

@Serializable
enum class PFPlan(val raw: String) {
    Free("free"), Pro("pro"), Annual("annual");

    val isPro: Boolean get() = this != Free

    companion object {
        fun from(raw: String?): PFPlan = entries.firstOrNull { it.raw == raw } ?: Free
    }
}

@Serializable
enum class PFSourceType(val raw: String) {
    Youtube("youtube"), Pdf("pdf"), Article("article");

    companion object {
        fun from(raw: String?): PFSourceType = entries.firstOrNull { it.raw == raw } ?: Pdf
    }
}

@Serializable
enum class PFSubscriptionStatus(val raw: String) {
    Active("active"), Canceled("canceled"), PastDue("past_due"),
    Trialing("trialing"), Incomplete("incomplete");

    companion object {
        fun from(raw: String?): PFSubscriptionStatus =
            entries.firstOrNull { it.raw == raw } ?: Active
    }
}

@Serializable
enum class PFProjectRole(val raw: String) {
    Owner("owner"), Editor("editor"), Viewer("viewer");

    val canEdit: Boolean get() = this != Viewer

    companion object {
        fun from(raw: String?): PFProjectRole = entries.firstOrNull { it.raw == raw } ?: Viewer
    }
}

@Serializable
enum class PFProjectInviteStatus(val raw: String) {
    Pending("pending"), Accepted("accepted"), Declined("declined"),
    Revoked("revoked"), Expired("expired");

    companion object {
        fun from(raw: String?): PFProjectInviteStatus =
            entries.firstOrNull { it.raw == raw } ?: Pending
    }
}

@Serializable
enum class PFProjectJoinRequestStatus(val raw: String) {
    Pending("pending"), Approved("approved"), Declined("declined");

    companion object {
        fun from(raw: String?): PFProjectJoinRequestStatus =
            entries.firstOrNull { it.raw == raw } ?: Pending
    }
}

fun newId(): String = UUID.randomUUID().toString()

// MARK: - Profile

@Serializable
data class PFProfile(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val plan: PFPlan = PFPlan.Free,
    val stripeCustomerId: String? = null,
    val decksGeneratedThisMonth: Int = 0,
    val generationResetAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** Best-effort display name: full name, else the local part of the email. */
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    val initials: String
        get() = displayName.trim().split(" ", limit = 2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

// MARK: - Folder

@Serializable
data class PFFolder(
    val id: String = newId(),
    val userId: String,
    val name: String,
    /** Hex color string ("#RRGGBB"). Null means "default". */
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

// MARK: - Deck

@Serializable
data class PFDeck(
    val id: String = newId(),
    val userId: String,
    val title: String,
    val summary: String? = null,
    val sourceType: PFSourceType,
    /** Original URL (YouTube link, article URL) or PDF filename. */
    val sourceRef: String? = null,
    /** Public-share flag. Mirrors `decks.is_public` on Supabase. */
    val isPublic: Boolean = false,
    val folderId: String? = null,
    /** Null for personal decks; set when a deck belongs to a project. */
    val projectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Last time the user studied this deck. Null = never. Local-only. */
    val lastStudiedAt: Long? = null,
)

// MARK: - Card

@Serializable
data class PFFlashcard(
    val id: String = newId(),
    val deckId: String,
    val front: String,
    val back: String,
    val hint: String? = null,
    /** Position within the deck for stable ordering. */
    val orderIndex: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // ── SRS state (SM-2) ──────────────────────────────────────────────
    /** SuperMemo "easiness factor". Starts at 2.5. */
    val srsEase: Double = 2.5,
    /** Days until next review. 0 = brand new. */
    val srsInterval: Int = 0,
    /** Number of times correctly recalled in a row. */
    val srsRepetitions: Int = 0,
    /** Epoch millis the card is next due. 0 = never scheduled. */
    val srsDueDate: Long = 0L,
    /** Epoch millis last reviewed (for stats). Null = never reviewed. */
    val srsLastReviewedAt: Long? = null,
)

// MARK: - Project

@Serializable
data class PFProject(
    val id: String = newId(),
    val ownerUserId: String,
    val name: String,
    val description: String? = null,
    val joinLinkToken: String = newId(),
    val joinLinkEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PFProjectMember(
    /** Composite local id: "{projectId}:{userId}". */
    val id: String,
    val projectId: String,
    val userId: String,
    val role: PFProjectRole,
    val joinedAt: Long = System.currentTimeMillis(),
    val invitedByUserId: String? = null,
    /** Mirrors the member's profile for display without joining tables. */
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
) {
    companion object {
        fun idFor(projectId: String, userId: String) = "$projectId:$userId"
    }
}

@Serializable
data class PFProjectInvite(
    val id: String,
    val projectId: String,
    val invitedEmail: String,
    val role: PFProjectRole,
    val status: PFProjectInviteStatus,
    val token: String,
    val invitedByUserId: String,
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    /** Snapshot captured at fetch time so the inbox renders without joins. */
    val projectName: String? = null,
    val inviterName: String? = null,
)

// MARK: - Study session

@Serializable
data class PFStudySession(
    val id: String = newId(),
    val deckId: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val cardsStudied: Int = 0,
    val cardsCorrect: Int = 0,
) {
    val accuracy: Float
        get() = if (cardsStudied == 0) 0f else cardsCorrect.toFloat() / cardsStudied
}
