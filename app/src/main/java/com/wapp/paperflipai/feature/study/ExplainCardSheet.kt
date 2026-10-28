package com.wapp.paperflipai.feature.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.CardExplanationDto
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFIndeterminateRing
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFSkeleton
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.core.analytics.TrackScreen
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * "Explain this card": a plain explanation, a concrete example and a memory
 * aid. One `explain-card` call per card; the result is cached on the card
 * (see PaperflipRepository.saveExplanation) so it is never fetched twice.
 * Counterpart of `ExplainCardSheet.swift`.
 */
@Composable
fun ExplainCardSheet(
    cardId: String,
    deckTitle: String?,
    onDismiss: () -> Unit,
) {
    TrackScreen("explain_card", "ExplainCardSheet")
    val env = LocalAppEnvironment.current
    val snapshot by env.database.snapshot.collectAsStateWithLifecycle()
    val card = snapshot.cards.firstOrNull { it.id == cardId } ?: return

    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val fallback = stringResource(R.string.explain_error)
    val hasExplanation = !card.explanation.isNullOrBlank()

    LaunchedEffect(cardId, attempt) {
        if (hasExplanation) return@LaunchedEffect
        error = null
        try {
            val language = env.settings.language.value.localeTag.ifEmpty { null }
            val result: CardExplanationDto = env.remote.explainCard(card.front, card.back, deckTitle, language)
            env.repository.saveExplanation(cardId, result)
            PFAnalytics.log(AnalyticsEvent.cardExplained)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            PFAnalytics.error(failure, "explain_card")
            error = failure.message?.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.explain_title)) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            val shape = RoundedCornerShape(PFRadius.xl)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.paper, shape)
                    .border(0.7.dp, PFTheme.colors.border, shape)
                    .padding(PFTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                Text(card.front, style = PFTheme.type.title3, color = PFTheme.colors.onSurface)
                Text(card.back, style = PFTheme.type.callout, color = PFTheme.colors.onSurfaceMuted)
            }

            val state = when {
                hasExplanation -> 2
                error != null -> 1
                else -> 0
            }
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "explain",
            ) { current ->
                when (current) {
                    2 -> Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                        ExplainBlock(PFIcons.Book, PFTheme.colors.accent, stringResource(R.string.explain_plain_words), card.explanation.orEmpty())
                        card.explanationExample?.takeIf { it.isNotBlank() }?.let {
                            ExplainBlock(PFIcons.Idea, PFTheme.colors.warning, stringResource(R.string.explain_example), it)
                        }
                        card.explanationMnemonic?.takeIf { it.isNotBlank() }?.let {
                            ExplainBlock(PFIcons.Sparkle, PFTheme.colors.success, stringResource(R.string.explain_remember), it)
                        }
                        Text(
                            text = stringResource(R.string.explain_disclaimer),
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceFaint,
                        )
                    }
                    1 -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = PFTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
                    ) {
                        Icon(PFIcons.Warning, null, tint = PFTheme.colors.onSurfaceMuted, modifier = Modifier.size(28.dp))
                        Text(
                            text = error.orEmpty(),
                            style = PFTheme.type.callout,
                            color = PFTheme.colors.onSurfaceMuted,
                            textAlign = TextAlign.Center,
                        )
                        PFButton(
                            title = stringResource(R.string.try_again),
                            onClick = { attempt += 1 },
                            variant = PFButtonVariant.Tonal,
                            leadingIcon = PFIcons.Refresh,
                            fullWidth = false,
                        )
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PFIndeterminateRing(size = 16.dp, lineWidth = 2.dp)
                            Spacer(Modifier.width(PFTheme.spacing.sm))
                            Text(
                                text = stringResource(R.string.explain_thinking),
                                style = PFTheme.type.footnote,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                        }
                        repeat(3) { PFSkeleton(height = 72.dp, radius = PFRadius.lg) }
                    }
                }
            }
            Spacer(Modifier.size(PFTheme.spacing.sm))
        }
    }
}

@Composable
private fun ExplainBlock(icon: ImageVector, tint: Color, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(PFTheme.spacing.sm))
            Text(title, style = PFTheme.type.footnoteBold, color = tint)
        }
        Text(body, style = PFTheme.type.body, color = PFTheme.colors.onSurface)
    }
}
