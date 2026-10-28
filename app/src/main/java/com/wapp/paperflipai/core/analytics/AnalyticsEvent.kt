package com.wapp.paperflipai.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.wapp.paperflipai.core.monetization.Entitlement
import com.wapp.paperflipai.core.monetization.PaywallProduct

/** A named analytics event — the Android counterpart of `AnalyticsEvent.swift`; names and keys match iOS. */
data class AnalyticsEvent(val name: String, val params: Map<String, Any?> = emptyMap()) {

    companion object {
        fun tabSelected(tab: String) = AnalyticsEvent("tab_selected", mapOf("tab" to tab))
        fun deepLinkOpened(route: String) = AnalyticsEvent("deep_link_opened", mapOf("route" to route))

        fun onboardingPageViewed(index: Int) = AnalyticsEvent("onboarding_page_viewed", mapOf("page_index" to index))
        fun onboardingSkipped(index: Int) = AnalyticsEvent("onboarding_skipped", mapOf("page_index" to index))
        val onboardingCompleted = AnalyticsEvent("onboarding_completed")

        fun login(method: String) = AnalyticsEvent(FirebaseAnalytics.Event.LOGIN, mapOf(FirebaseAnalytics.Param.METHOD to method))
        fun signUp(method: String) = AnalyticsEvent(FirebaseAnalytics.Event.SIGN_UP, mapOf(FirebaseAnalytics.Param.METHOD to method))
        fun authFailed(method: String, action: String, error: Throwable) =
            AnalyticsEvent("auth_failed", mapOf("method" to method, "action" to action, "reason" to (error.message ?: error.toString())))
        fun authCancelled(method: String) = AnalyticsEvent("auth_cancelled", mapOf("method" to method))
        val signOut = AnalyticsEvent("sign_out")
        val accountDeleted = AnalyticsEvent("account_deleted")

        fun paywallViewed(source: PaywallSource, plan: Entitlement) =
            AnalyticsEvent("paywall_viewed", mapOf("source" to source.raw, "plan" to plan.raw))
        fun paywallPlanSelected(source: PaywallSource, productId: String) =
            AnalyticsEvent("paywall_plan_selected", mapOf("source" to source.raw, "product_id" to productId))
        fun paywallDismissed(
            source: PaywallSource,
            method: PaywallDismissMethod,
            seconds: Int,
            purchaseAttempted: Boolean,
            lastError: String?,
        ) = AnalyticsEvent(
            "paywall_dismissed",
            mapOf(
                "source" to source.raw,
                "method" to method.raw,
                "seconds" to seconds,
                "purchase_attempted" to purchaseAttempted,
                "last_error" to (lastError ?: "none"),
            ),
        )
        fun paywallProductsFailed(source: PaywallSource, error: String) =
            AnalyticsEvent("paywall_products_failed", mapOf("source" to source.raw, "error" to error))

        fun purchaseStarted(source: PaywallSource, product: PaywallProduct) = AnalyticsEvent("purchase_started", purchaseParams(source, product))
        fun purchaseCompleted(source: PaywallSource, product: PaywallProduct) = AnalyticsEvent("purchase_completed", purchaseParams(source, product))
        fun purchaseCancelled(source: PaywallSource, product: PaywallProduct) = AnalyticsEvent("purchase_cancelled", purchaseParams(source, product))
        fun purchasePending(source: PaywallSource, product: PaywallProduct) = AnalyticsEvent("purchase_pending", purchaseParams(source, product))
        fun purchaseFailed(source: PaywallSource, product: PaywallProduct, error: Throwable) = AnalyticsEvent(
            "purchase_failed",
            purchaseParams(source, product) + mapOf("error" to (error.message ?: error.toString()), "error_code" to 0),
        )

        fun restoreStarted(source: String) = AnalyticsEvent("restore_started", mapOf("source" to source))
        fun restoreCompleted(source: String) = AnalyticsEvent("restore_completed", mapOf("source" to source))
        fun restoreNoneFound(source: String) = AnalyticsEvent("restore_none_found", mapOf("source" to source))
        fun restoreFailed(source: String, error: Throwable) =
            AnalyticsEvent("restore_failed", mapOf("source" to source, "error" to (error.message ?: error.toString())))

        fun importSourceSelected(type: String) = AnalyticsEvent("import_source_selected", mapOf("source_type" to type))
        fun importFailed(type: String, stage: String, error: String) =
            AnalyticsEvent("import_failed", mapOf("source_type" to type, "stage" to stage, "error" to error))
        fun limitReached(limit: String, source: String) = AnalyticsEvent("limit_reached", mapOf("limit" to limit, "source_type" to source))

        fun generationStarted(type: String, cardCount: Int, autoCount: Boolean) =
            AnalyticsEvent("generation_started", mapOf("source_type" to type, "card_count" to cardCount, "auto_count" to autoCount))
        fun generationCompleted(type: String, cardCount: Int, seconds: Int) =
            AnalyticsEvent("generation_completed", mapOf("source_type" to type, "card_count" to cardCount, "seconds" to seconds))
        fun generationFailed(type: String, code: String, message: String, seconds: Int) =
            AnalyticsEvent("generation_failed", mapOf("source_type" to type, "error_code" to code, "error" to message, "seconds" to seconds))
        fun generationCancelled(type: String, seconds: Int) =
            AnalyticsEvent("generation_cancelled", mapOf("source_type" to type, "seconds" to seconds))

        fun deckSaved(type: String, cardCount: Int, inProject: Boolean) =
            AnalyticsEvent("deck_saved", mapOf("source_type" to type, "card_count" to cardCount, "in_project" to inProject))
        fun ankiImportCompleted(cardCount: Int, skipped: Int) =
            AnalyticsEvent("anki_import_completed", mapOf("card_count" to cardCount, "skipped" to skipped))
        val sharedDeckImported = AnalyticsEvent("shared_deck_imported")
        val deckDeleted = AnalyticsEvent("deck_deleted")
        val deckRenamed = AnalyticsEvent("deck_renamed")
        fun deckSharingChanged(isPublic: Boolean) = AnalyticsEvent("deck_sharing_changed", mapOf("is_public" to isPublic))
        val deckMovedToProject = AnalyticsEvent("deck_moved_to_project")

        val projectCreated = AnalyticsEvent("project_created")
        val projectDeleted = AnalyticsEvent("project_deleted")
        val projectLeft = AnalyticsEvent("project_left")
        val projectMemberInvited = AnalyticsEvent("project_member_invited")
        val projectJoinRequested = AnalyticsEvent("project_join_requested")
        fun projectInviteResponded(accepted: Boolean) = AnalyticsEvent("project_invite_responded", mapOf("accepted" to accepted))

        fun studySessionStarted(origin: String, cardCount: Int) =
            AnalyticsEvent("study_session_started", mapOf("origin" to origin, "card_count" to cardCount))
        fun studySessionCompleted(cardsStudied: Int, cardsCorrect: Int, seconds: Int, streakIncremented: Boolean) = AnalyticsEvent(
            "study_session_completed",
            mapOf(
                "cards_studied" to cardsStudied,
                "cards_correct" to cardsCorrect,
                "seconds" to seconds,
                "streak_incremented" to streakIncremented,
            ),
        )
        fun studySessionAbandoned(cardsStudied: Int, totalCards: Int, seconds: Int) = AnalyticsEvent(
            "study_session_abandoned",
            mapOf("cards_studied" to cardsStudied, "total_cards" to totalCards, "seconds" to seconds),
        )
        val cardExplained = AnalyticsEvent("card_explained")

        fun quizStarted(mode: String, questionCount: Int) =
            AnalyticsEvent("quiz_started", mapOf("mode" to mode, "question_count" to questionCount))
        fun quizCompleted(mode: String, questionCount: Int, scorePercent: Int, seconds: Int) = AnalyticsEvent(
            "quiz_completed",
            mapOf("mode" to mode, "question_count" to questionCount, "score_percent" to scorePercent, "seconds" to seconds),
        )
        fun quizAbandoned(mode: String, answered: Int, questionCount: Int) =
            AnalyticsEvent("quiz_abandoned", mapOf("mode" to mode, "answered" to answered, "question_count" to questionCount))
        val matchStarted = AnalyticsEvent("match_started")
        fun matchCompleted(seconds: Int) = AnalyticsEvent("match_completed", mapOf("seconds" to seconds))

        fun notificationPermission(granted: Boolean) = AnalyticsEvent("notification_permission", mapOf("granted" to granted))
        fun reminderToggled(enabled: Boolean) = AnalyticsEvent("reminder_toggled", mapOf("enabled" to enabled))
        val reviewPromptShown = AnalyticsEvent("review_prompt_shown")
        fun appearanceChanged(value: String) = AnalyticsEvent("appearance_changed", mapOf("value" to value))
        fun languageChanged(value: String) = AnalyticsEvent("language_changed", mapOf("value" to value))
        val supportMessageSent = AnalyticsEvent("support_message_sent")

        private fun purchaseParams(source: PaywallSource, product: PaywallProduct): Map<String, Any?> = mapOf(
            "source" to source.raw,
            "product_id" to product.id,
            "cadence" to if (product.cadence == PaywallProduct.Cadence.Annual) "annual" else "monthly",
            "price" to product.priceLabel,
            "has_trial" to (product.introLabel != null),
        )
    }
}

enum class PaywallSource(val raw: String) {
    Settings("settings"),
    ManageSubscription("manage_subscription"),
    LibraryAssignProject("library_assign_project"),
    LibraryProjectsUpsell("library_projects_upsell"),
    DeckShare("deck_share"),
    DeckMoveToProject("deck_move_to_project"),
    ReviewProjectChip("review_project_chip"),
    PdfTooLarge("pdf_too_large"),
    CardCountUpsell("card_count_upsell"),
    AutoCardCount("auto_card_count"),
    SharedLinkImport("shared_link_import"),
    ImportProHint("import_pro_hint"),
    Unknown("unknown");

    companion object {
        fun from(raw: String?): PaywallSource = entries.firstOrNull { it.raw == raw } ?: Unknown
    }
}

enum class PaywallDismissMethod(val raw: String) {
    CloseButton("close_button"),
    Back("back"),
    Purchased("purchased"),
    AlreadyPro("already_pro"),
}
