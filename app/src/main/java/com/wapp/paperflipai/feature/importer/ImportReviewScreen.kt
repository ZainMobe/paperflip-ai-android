package com.wapp.paperflipai.feature.importer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Review step for decks that need no AI (Anki imports). The deck was parsed
 * on the Import hub and handed over through [ImportDraft].
 */
@Composable
fun ImportReviewScreen(
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val draft = remember { ImportDraft.take() }
    if (draft == null) {
        // Process death or a stale back-stack entry: nothing to review.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    ReviewGeneratedDeckContent(
        dto = draft,
        onSaved = onOpenDeck,
        onDiscarded = onBack,
        onOpenPaywall = onOpenPaywall,
    )
}
