package com.wapp.paperflipai.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.core.data.PFStudySession
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFProgressRing
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.component.tabBarContentPadding
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.library.icon
import com.wapp.paperflipai.feature.library.tint
import com.wapp.paperflipai.util.asShortDate
import com.wapp.paperflipai.util.isSameDay
import kotlin.math.roundToInt

/**
 * Tab 4 — streak, mastery breakdown, accuracy ring, recent sessions and the
 * source-type split. The Android counterpart of `StatsHomeView.swift`, all
 * computed from the local store plus the streak tracker.
 */
@Composable
fun StatsHomeScreen(
    onOpenDeck: (String) -> Unit,
    onStartSession: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val snapshot by env.database.snapshot.collectAsStateWithLifecycle()
    val streak by env.streaks.count.collectAsStateWithLifecycle()
    val lastStudyDate by env.streaks.lastStudyDate.collectAsStateWithLifecycle()

    val decks = snapshot.decks
    val cards = snapshot.cards
    val sessions = remember(snapshot.sessions) { snapshot.sessions.sortedByDescending { it.startedAt } }

    val totalReviewed = remember(sessions) { sessions.sumOf { it.cardsStudied } }
    val totalCorrect = remember(sessions) { sessions.sumOf { it.cardsCorrect } }
    val accuracy = if (totalReviewed == 0) 0f else totalCorrect.toFloat() / totalReviewed

    val newCount = remember(cards) { cards.count { it.srsRepetitions == 0 } }
    val learningCount = remember(cards) { cards.count { it.srsRepetitions > 0 && it.srsInterval < 21 } }
    val masteredCount = remember(cards) { cards.count { it.srsRepetitions > 0 && it.srsInterval >= 21 } }

    val accuracyTint = when {
        accuracy >= 0.8f -> PFTheme.colors.success
        accuracy >= 0.6f -> PFTheme.colors.warning
        else -> PFTheme.colors.danger
    }

    PFScreen(topBar = { PFTopBar(title = stringResource(R.string.stats)) }) { padding ->
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
            // ── Streak hero ───────────────────────────────────────────
            PFCard(
                elevation = PFElevation.Hero,
                surface = PFTheme.colors.paper,
                modifier = Modifier.padding(top = PFTheme.spacing.md),
                onClick = if (streak == 0) onStartSession else null,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(PFTheme.colors.hard.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PFIcons.Streak,
                            contentDescription = null,
                            tint = PFTheme.colors.hard,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(Modifier.width(PFTheme.spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.day_streak_2, streak),
                            style = PFTheme.type.title2,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = when {
                                streak == 0 -> stringResource(R.string.study_any_deck_today_to_start_one)
                                lastStudyDate?.let { isSameDay(it, System.currentTimeMillis()) } == true ->
                                    stringResource(R.string.you_ve_studied_today_keep_it_up)
                                else -> stringResource(R.string.study_today_to_keep_it_alive)
                            },
                            style = PFTheme.type.callout,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            }

            // ── Overview tiles ────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                SectionLabel(stringResource(R.string.overview))
                Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    StatTile(
                        value = decks.size.toString(),
                        label = stringResource(R.string.decks_3),
                        icon = PFIcons.Deck,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = cards.size.toString(),
                        label = stringResource(R.string.cards_3),
                        icon = PFIcons.Card,
                        tint = PFTheme.colors.warning,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = totalReviewed.toString(),
                        label = stringResource(R.string.reviewed),
                        icon = PFIcons.CheckCircle,
                        tint = PFTheme.colors.success,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Mastery ───────────────────────────────────────────────
            if (cards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    SectionLabel(stringResource(R.string.mastery))
                    PFCard {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MasteryBadge(newCount, stringResource(R.string.new_status), PFTheme.colors.accent, Modifier.weight(1f))
                            MasteryBadge(learningCount, stringResource(R.string.learning), PFTheme.colors.warning, Modifier.weight(1f))
                            MasteryBadge(masteredCount, stringResource(R.string.mastered), PFTheme.colors.success, Modifier.weight(1f))
                        }
                        Spacer(Modifier.size(PFTheme.spacing.lg))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (newCount > 0) {
                                Box(
                                    Modifier
                                        .weight(newCount.toFloat())
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(PFTheme.colors.accent, CircleShape)
                                )
                            }
                            if (learningCount > 0) {
                                Box(
                                    Modifier
                                        .weight(learningCount.toFloat())
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(PFTheme.colors.warning, CircleShape)
                                )
                            }
                            if (masteredCount > 0) {
                                Box(
                                    Modifier
                                        .weight(masteredCount.toFloat())
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(PFTheme.colors.success, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // ── Accuracy ──────────────────────────────────────────────
            if (sessions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    SectionLabel(stringResource(R.string.accuracy))
                    PFCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PFProgressRing(
                                progress = accuracy,
                                size = 80.dp,
                                lineWidth = 8.dp,
                                tint = accuracyTint,
                            )
                            Spacer(Modifier.width(PFTheme.spacing.xl))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
                            ) {
                                AccuracyStat(
                                    stringResource(R.string.correct),
                                    totalCorrect.toString(),
                                    PFTheme.colors.success,
                                )
                                AccuracyStat(
                                    stringResource(R.string.incorrect),
                                    (totalReviewed - totalCorrect).toString(),
                                    PFTheme.colors.danger,
                                )
                                AccuracyStat(
                                    stringResource(R.string.sessions),
                                    sessions.size.toString(),
                                    PFTheme.colors.accent,
                                )
                            }
                        }
                    }
                }

                // ── Recent sessions ───────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    SectionLabel(stringResource(R.string.recent_sessions))
                    sessions.take(5).forEach { session ->
                        SessionRow(
                            session = session,
                            deckTitle = snapshot.decks.firstOrNull { it.id == session.deckId }?.title,
                            deckIcon = snapshot.decks.firstOrNull { it.id == session.deckId }?.icon
                                ?: PFIcons.Deck,
                            deckTint = snapshot.decks.firstOrNull { it.id == session.deckId }?.tint()
                                ?: PFTheme.colors.accent,
                            onClick = { session.deckId?.let(onOpenDeck) },
                        )
                    }
                }
            }

            // ── By source ─────────────────────────────────────────────
            if (decks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    SectionLabel(stringResource(R.string.by_source))
                    PFCard {
                        PFSourceType.entries.forEachIndexed { index, type ->
                            SourceRow(
                                type = type,
                                count = decks.count { it.sourceType == type },
                            )
                            if (index < PFSourceType.entries.lastIndex) {
                                Box(
                                    Modifier
                                        .padding(vertical = PFTheme.spacing.md)
                                        .fillMaxWidth()
                                        .height(0.7.dp)
                                        .background(PFTheme.colors.divider)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = PFTheme.type.overline, color = PFTheme.colors.onSurfaceMuted)
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    PFCard(modifier = modifier, padding = PFTheme.spacing.md) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(PFTheme.spacing.sm))
        Text(text = value, style = PFTheme.type.title2, color = PFTheme.colors.onSurface)
        Text(text = label, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun MasteryBadge(count: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
    ) {
        Text(text = count.toString(), style = PFTheme.type.title2, color = tint)
        Text(text = label, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun AccuracyStat(label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = label, style = PFTheme.type.footnote, color = PFTheme.colors.onSurfaceMuted)
        Spacer(Modifier.weight(1f))
        Text(text = value, style = PFTheme.type.footnoteBold, color = PFTheme.colors.onSurface)
    }
}

@Composable
private fun SessionRow(
    session: PFStudySession,
    deckTitle: String?,
    deckIcon: ImageVector,
    deckTint: Color,
    onClick: () -> Unit,
) {
    val accuracy = if (session.cardsStudied == 0) 0
    else (session.cardsCorrect * 100f / session.cardsStudied).roundToInt()
    val accuracyColor = when {
        accuracy >= 80 -> PFTheme.colors.success
        accuracy >= 60 -> PFTheme.colors.warning
        else -> PFTheme.colors.danger
    }
    val now = System.currentTimeMillis()
    val dateLabel = when {
        isSameDay(session.startedAt, now) -> stringResource(R.string.today_2)
        isSameDay(session.startedAt, now - 86_400_000L) -> stringResource(R.string.yesterday)
        else -> session.startedAt.asShortDate()
    }
    val shape = RoundedCornerShape(PFRadius.lg)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.99f)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(deckTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(deckIcon, contentDescription = null, tint = deckTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = deckTitle ?: stringResource(R.string.mixed_session),
                style = PFTheme.type.bodyEmphasis,
                color = PFTheme.colors.onSurface,
                maxLines = 1,
            )
            Text(text = dateLabel, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "$accuracy%", style = PFTheme.type.bodyEmphasis, color = accuracyColor)
            Text(
                text = stringResource(R.string.cards, session.cardsStudied),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun SourceRow(type: PFSourceType, count: Int) {
    val tint = when (type) {
        PFSourceType.Pdf -> PFTheme.colors.accent
        PFSourceType.Youtube -> PFTheme.colors.danger
        PFSourceType.Article -> PFTheme.colors.warning
    }
    val icon = when (type) {
        PFSourceType.Pdf -> PFIcons.Pdf
        PFSourceType.Youtube -> PFIcons.Video
        PFSourceType.Article -> PFIcons.Article
    }
    val label = stringResource(
        when (type) {
            PFSourceType.Pdf -> R.string.pdf
            PFSourceType.Youtube -> R.string.youtube
            PFSourceType.Article -> R.string.articles
        }
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(PFTheme.spacing.md))
        Text(text = label, style = PFTheme.type.body, color = PFTheme.colors.onSurface)
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = PFTheme.type.bodyEmphasis,
            color = PFTheme.colors.onSurfaceMuted,
            modifier = Modifier
                .background(tint.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = PFTheme.spacing.sm, vertical = 2.dp),
        )
    }
}
