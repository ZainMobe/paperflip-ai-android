package com.wapp.paperflipai.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.IntentInbox
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.app.PFAction
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.srs.StudyQueue
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.component.tabBarContentPadding
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.library.icon
import com.wapp.paperflipai.feature.library.tint
import com.wapp.paperflipai.util.daysFromNow
import kotlin.math.max

/**
 * Tab 2 — today's review queue across every deck, the Android counterpart
 * of `StudyHomeView.swift`. Start a mixed session or jump straight into a
 * specific deck.
 */
@Composable
fun StudyHomeScreen(
    onStartSession: (String?) -> Unit,
    onOpenImport: () -> Unit,
    onOpenDeck: (String) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val snapshot by env.database.snapshot.collectAsStateWithLifecycle()
    val pendingAction by IntentInbox.pending.collectAsStateWithLifecycle()

    val decks = snapshot.decks
    val cardsByDeck = remember(snapshot.cards) { snapshot.cards.groupBy { it.deckId } }
    val dueByDeck = remember(cardsByDeck) {
        cardsByDeck.mapValues { (_, cards) -> StudyQueue.dueCount(cards) }
    }
    val totalDue = remember(dueByDeck) { dueByDeck.values.sum() }
    val decksWithDue = remember(decks, dueByDeck) {
        decks.filter { (dueByDeck[it.id] ?: 0) > 0 }
            .sortedByDescending { dueByDeck[it.id] ?: 0 }
    }

    LaunchedEffect(pendingAction) {
        if (pendingAction is PFAction.StartStudy) {
            IntentInbox.clear()
            onStartSession(null)
        }
    }

    LaunchedEffect(totalDue) { env.refreshWidget() }

    PFScreen(topBar = { PFTopBar(title = stringResource(R.string.study)) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = tabBarContentPadding(PFTheme.spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            // ── Today hero ────────────────────────────────────────────
            PFCard(
                elevation = PFElevation.Hero,
                surface = PFTheme.colors.paper,
                modifier = Modifier.padding(top = PFTheme.spacing.md),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    PFTagPill(
                        title = stringResource(R.string.today),
                        icon = if (totalDue > 0) PFIcons.Streak else PFIcons.CheckCircle,
                        tint = if (totalDue > 0) PFTheme.colors.hard else PFTheme.colors.success,
                        prominent = true,
                    )
                    Text(
                        text = when {
                            decks.isEmpty() -> stringResource(R.string.no_decks_yet)
                            totalDue == 0 -> stringResource(R.string.all_caught_up_2)
                            else -> stringResource(R.string.cards_due, totalDue)
                        },
                        style = PFTheme.type.title1,
                        color = PFTheme.colors.onSurface,
                    )
                    Text(
                        text = when {
                            decks.isEmpty() ->
                                stringResource(R.string.import_a_pdf_youtube_video_or_article_to_get_s)
                            totalDue == 0 ->
                                stringResource(R.string.nothing_to_review_right_now_come_back_tomorrow)
                            else ->
                                stringResource(R.string.about_minutes_of_focused_study, max(1, totalDue / 4))
                        },
                        style = PFTheme.type.callout,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    when {
                        totalDue > 0 -> PFButton(
                            title = stringResource(R.string.start_session),
                            onClick = { onStartSession(null) },
                            size = PFButtonSize.Lg,
                            trailingIcon = PFIcons.Forward,
                            modifier = Modifier.padding(top = PFTheme.spacing.xs),
                        )
                        decks.isNotEmpty() -> PFButton(
                            title = stringResource(R.string.study_any_deck),
                            onClick = { onStartSession(null) },
                            variant = PFButtonVariant.Outline,
                            leadingIcon = PFIcons.Deck,
                            fullWidth = false,
                            modifier = Modifier.padding(top = PFTheme.spacing.xs),
                        )
                        else -> PFButton(
                            title = stringResource(R.string.create_your_first_deck),
                            onClick = onOpenImport,
                            size = PFButtonSize.Lg,
                            trailingIcon = PFIcons.Forward,
                            modifier = Modifier.padding(top = PFTheme.spacing.xs),
                        )
                    }
                }
            }

            // ── Due by deck ───────────────────────────────────────────
            if (decksWithDue.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.due_by_deck),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    decksWithDue.forEach { deck ->
                        DueDeckRow(
                            deck = deck,
                            dueCount = dueByDeck[deck.id] ?: 0,
                            onClick = { onStartSession(deck.id) },
                        )
                    }
                }
            }

            // ── Up next ───────────────────────────────────────────────
            if (decks.isNotEmpty() && totalDue == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.up_next),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    decks.take(3).forEach { deck ->
                        val nextDue = cardsByDeck[deck.id].orEmpty()
                            .filter { it.srsRepetitions > 0 }
                            .minOfOrNull { it.srsDueDate }
                        val label = when {
                            nextDue == null -> stringResource(R.string.not_yet_studied)
                            else -> {
                                val days = daysFromNow(nextDue)
                                when {
                                    days <= 0 -> stringResource(R.string.today_2)
                                    days == 1 -> stringResource(R.string.tomorrow)
                                    else -> stringResource(R.string.in_days, days)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.md))
                                .pfPressable(onClick = { onOpenDeck(deck.id) }, pressedScale = 0.99f)
                                .padding(PFTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = deck.icon,
                                contentDescription = null,
                                tint = deck.tint(),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(PFTheme.spacing.sm))
                            Text(
                                text = deck.title,
                                style = PFTheme.type.body,
                                color = PFTheme.colors.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = label,
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DueDeckRow(deck: PFDeck, dueCount: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.985f)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(deck.tint().copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(deck.icon, contentDescription = null, tint = deck.tint(), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = deck.title,
                style = PFTheme.type.bodyEmphasis,
                color = PFTheme.colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.cards_due, dueCount),
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
        Text(
            text = dueCount.toString(),
            style = PFTheme.type.title3,
            color = PFTheme.colors.accent,
            modifier = Modifier
                .background(PFTheme.colors.accentSoft, CircleShape)
                .padding(horizontal = PFTheme.spacing.md, vertical = 4.dp),
        )
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Icon(
            imageVector = PFIcons.Chevron,
            contentDescription = null,
            tint = PFTheme.colors.onSurfaceFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}
