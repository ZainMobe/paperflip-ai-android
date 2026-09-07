package com.wapp.paperflipai.feature.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * End-of-session screen — the Android counterpart of
 * `StudySummaryView.swift`. Celebrates the work and surfaces the streak so
 * the retention loop closes.
 */
@Composable
fun StudySummary(
    cardsReviewed: Int,
    cardsCorrect: Int,
    durationMillis: Long,
    streak: Int,
    didIncrementStreak: Boolean,
    onDone: () -> Unit,
    onStudyMore: (() -> Unit)? = null,
) {
    val haptics = rememberPFHaptics()
    val accuracy = if (cardsReviewed == 0) 0 else (cardsCorrect * 100f / cardsReviewed).roundToInt()

    val heroTint = when {
        accuracy >= 100 -> PFTheme.colors.warning
        accuracy >= 80 -> PFTheme.colors.accent
        else -> PFTheme.colors.success
    }
    val heroIcon: ImageVector = when {
        accuracy >= 100 -> PFIcons.Star
        accuracy >= 80 -> PFIcons.Pro
        else -> PFIcons.Check
    }
    val heroTitle = stringResource(
        when {
            accuracy >= 100 -> R.string.perfect
            accuracy >= 80 -> R.string.outstanding
            accuracy >= 60 -> R.string.nice_session
            else -> R.string.keep_going
        }
    )
    val intensity = when {
        accuracy >= 100 -> ConfettiIntensity.Epic
        accuracy >= 80 -> ConfettiIntensity.Great
        else -> ConfettiIntensity.Good
    }

    var showHero by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showStreak by remember { mutableStateOf(false) }
    var showAchievement by remember { mutableStateOf(false) }
    var displayedCards by remember { mutableIntStateOf(0) }
    var displayedAccuracy by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        haptics.perform(PFHaptic.Success)
        delay(200); showHero = true
        delay(200); showStats = true
        repeat(18) { step ->
            delay(40)
            val fraction = (step + 1) / 18f
            displayedCards = (cardsReviewed * fraction).roundToInt()
            displayedAccuracy = (accuracy * fraction).roundToInt()
        }
        showStreak = true
        delay(300); showAchievement = true
    }

    val pulseTransition = rememberInfiniteTransition(label = "summaryPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "summaryPulseScale",
    )
    val ring by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Restart),
        label = "summaryRing",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Spacer(Modifier.weight(1f))

            // ── Hero ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                repeat(3) { index ->
                    val phase = ((ring + index * 0.33f) % 1f)
                    Box(
                        modifier = Modifier
                            .size((110 + 120 * phase).dp)
                            .border(
                                width = 1.5.dp,
                                color = heroTint.copy(alpha = (0.18f - index * 0.05f) * (1f - phase)),
                                shape = CircleShape,
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(heroTint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = heroIcon,
                        contentDescription = null,
                        tint = heroTint,
                        modifier = Modifier.size((50 * pulse).dp),
                    )
                }
            }

            AnimatedVisibility(visible = showHero, enter = fadeIn() + slideInVertically { it / 4 }) {
                Text(
                    text = heroTitle,
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            // ── Stats ─────────────────────────────────────────────────
            AnimatedVisibility(visible = showStats, enter = fadeIn() + slideInVertically { it / 4 }) {
                PFCard(elevation = PFElevation.Card, surface = PFTheme.colors.paper) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SummaryStat(
                            value = displayedCards.toString(),
                            label = stringResource(R.string.cards_3),
                            modifier = Modifier.weight(1f),
                        )
                        StatDivider()
                        SummaryStat(
                            value = durationLabel(durationMillis),
                            label = stringResource(R.string.time),
                            modifier = Modifier.weight(1f),
                        )
                        StatDivider()
                        SummaryStat(
                            value = "$displayedAccuracy%",
                            label = stringResource(R.string.accuracy_2),
                            tint = if (accuracy >= 80) PFTheme.colors.success else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Streak ────────────────────────────────────────────────
            AnimatedVisibility(visible = showStreak, enter = fadeIn() + slideInVertically { it / 4 }) {
                PFCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(PFTheme.colors.hard.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PFIcons.Streak,
                                contentDescription = null,
                                tint = PFTheme.colors.hard,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(PFTheme.spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.day_streak_2, streak),
                                style = PFTheme.type.bodyEmphasis,
                                color = PFTheme.colors.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    if (didIncrementStreak) R.string.you_added_another_day_keep_it_going
                                    else R.string.today_s_already_counted_come_back_tomorrow
                                ),
                                style = PFTheme.type.footnote,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                        }
                        if (didIncrementStreak) {
                            PFTagPill(
                                title = "+1",
                                icon = PFIcons.Trending,
                                tint = PFTheme.colors.success,
                                prominent = true,
                                uppercase = false,
                            )
                        }
                    }
                }
            }

            // ── Achievement ───────────────────────────────────────────
            val achievementTitle: String? = when {
                accuracy >= 100 && cardsReviewed >= 5 -> stringResource(R.string.perfect_score)
                didIncrementStreak && streak in listOf(3, 7, 14, 30) ->
                    stringResource(R.string.day_streak, streak)
                else -> null
            }
            val achievementSubtitle: String? = when {
                accuracy >= 100 && cardsReviewed >= 5 ->
                    stringResource(R.string.you_nailed_every_single_card_impressive)
                didIncrementStreak && streak in listOf(3, 7, 14, 30) ->
                    stringResource(R.string.days_of_consistent_study_you_re_building_a_hab, streak)
                else -> null
            }

            if (achievementTitle != null) {
                AnimatedVisibility(visible = showAchievement, enter = fadeIn() + slideInVertically { it / 4 }) {
                    PFCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(heroTint.copy(alpha = 0.14f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PFIcons.Pro,
                                    contentDescription = null,
                                    tint = heroTint,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(PFTheme.spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = achievementTitle,
                                    style = PFTheme.type.bodyEmphasis,
                                    color = PFTheme.colors.onSurface,
                                )
                                achievementSubtitle?.let {
                                    Text(
                                        text = it,
                                        style = PFTheme.type.footnote,
                                        color = PFTheme.colors.onSurfaceMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                onStudyMore?.let {
                    PFButton(
                        title = stringResource(R.string.study_more),
                        onClick = it,
                        size = PFButtonSize.Lg,
                        leadingIcon = PFIcons.Refresh,
                    )
                }
                PFButton(
                    title = stringResource(R.string.done),
                    onClick = onDone,
                    size = PFButtonSize.Lg,
                    variant = if (onStudyMore != null) PFButtonVariant.Ghost else PFButtonVariant.Filled,
                )
            }
        }

        ConfettiBurst(intensity = intensity, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = PFTheme.type.title2,
            color = tint ?: PFTheme.colors.onSurface,
        )
        Text(text = label, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(0.7.dp)
            .height(36.dp)
            .background(PFTheme.colors.divider)
    )
}

private fun durationLabel(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
