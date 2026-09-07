package com.wapp.paperflipai.feature.onboarding

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure-Compose onboarding illustrations — the Android counterpart of
 * `OnboardingIllustrations.swift`. No PNGs or SVGs: every scene is shapes,
 * gradients and gentle looping motion, so the APK stays lean and the brand
 * visuals evolve in code.
 */

@Composable
private fun rememberBreathing(durationMillis: Int, label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = label,
    )
    return value
}

@Composable
private fun RadialGlow(size: Dp, alpha: Float = 0.18f) {
    val accent = PFTheme.colors.accent
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
                ),
                CircleShape,
            )
    )
}

// ── 1. Sources transforming into cards ───────────────────────────────

@Composable
fun OnboardingSourcesIllustration(modifier: Modifier = Modifier) {
    val lift = rememberBreathing(2400, "sourcesLift")
    val stack = rememberBreathing(2600, "sourcesStack")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        RadialGlow(320.dp)

        SourceChip(PFIcons.Document, PFTheme.colors.accent, (-78).dp, (-76).dp, -8f, lift)
        SourceChip(PFIcons.Video, PFTheme.colors.danger, 78.dp, (-64).dp, 10f, 1f - lift)
        SourceChip(PFIcons.Article, PFTheme.colors.warning, (-50).dp, 70.dp, -4f, lift)

        Box(
            modifier = Modifier.offset(y = (30 + (1f - stack) * 8f).dp),
            contentAlignment = Alignment.Center,
        ) {
            // Back-most cards peek out
            (2 downTo 1).forEach { depth ->
                Box(
                    modifier = Modifier
                        .offset(y = (depth * 14).dp)
                        .scale(1f - depth * 0.05f)
                        .size(width = 200.dp, height = 130.dp)
                        .pfElevation(PFElevation.Card, RoundedCornerShape(PFRadius.xl))
                        .background(
                            PFTheme.colors.paper.copy(alpha = 1f - depth * 0.25f),
                            RoundedCornerShape(PFRadius.xl),
                        )
                )
            }
            Column(
                modifier = Modifier
                    .size(width = 200.dp, height = 130.dp)
                    .pfElevation(PFElevation.Hero, RoundedCornerShape(PFRadius.xl))
                    .background(PFTheme.colors.paper, RoundedCornerShape(PFRadius.xl))
                    .padding(PFTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                PFTagPill(title = "PDF", icon = PFIcons.Document)
                Text(
                    text = stringResource(R.string.what_is_spaced_repetition),
                    style = PFTheme.type.headline,
                    color = PFTheme.colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SourceChip(
    icon: ImageVector,
    tint: Color,
    offsetX: Dp,
    offsetY: Dp,
    rotation: Float,
    phase: Float,
) {
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY + (phase * 8f - 4f).dp)
            .rotate(rotation)
            .size(64.dp)
            .pfElevation(PFElevation.Card, RoundedCornerShape(PFRadius.lg))
            .background(PFTheme.colors.elevatedHigh, RoundedCornerShape(PFRadius.lg)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

// ── 2. Brain with orbiting memories ──────────────────────────────────

@Composable
fun OnboardingStudyIllustration(modifier: Modifier = Modifier) {
    val pulse = rememberBreathing(1800, "studyPulse")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        RadialGlow(320.dp)

        repeat(5) { i ->
            val angle = i / 5.0 * 360.0
            val radius = 110f
            val x = (cos(Math.toRadians(angle)) * radius).toFloat()
            val y = (sin(Math.toRadians(angle)) * radius).toFloat()
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(32.dp)
                    .pfElevation(PFElevation.Card, RoundedCornerShape(PFRadius.md))
                    .background(PFTheme.colors.elevatedHigh, RoundedCornerShape(PFRadius.md)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            PFTheme.colors.accent.copy(alpha = 0.4f + 0.6f * pulse),
                            CircleShape,
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .pfElevation(PFElevation.Hero, CircleShape)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PFIcons.Study,
                contentDescription = null,
                tint = PFTheme.colors.accent,
                modifier = Modifier
                    .size(60.dp)
                    .scale(0.97f + 0.08f * pulse),
            )
        }
    }
}

// ── 3. Phone with floating widgets ───────────────────────────────────

@Composable
fun OnboardingAnywhereIllustration(modifier: Modifier = Modifier) {
    val float = rememberBreathing(2600, "anywhereFloat")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        RadialGlow(320.dp, alpha = 0.16f)

        Column(
            modifier = Modifier
                .size(width = 168.dp, height = 240.dp)
                .pfElevation(PFElevation.Hero, RoundedCornerShape(36.dp))
                .background(PFTheme.colors.elevatedHigh, RoundedCornerShape(36.dp))
                .padding(PFTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = PFTheme.spacing.sm)
                    .size(width = 50.dp, height = 5.dp)
                    .background(PFTheme.colors.divider, CircleShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.md))
                    .padding(PFTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PFTagPill(title = stringResource(R.string.today))
                Text(
                    text = stringResource(R.string.n_12_cards_due),
                    style = PFTheme.type.headline,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.n_8_day_streak),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }

        FloatingWidget(
            icon = PFIcons.Card,
            title = stringResource(R.string.decks_3),
            offsetX = (-100).dp,
            offsetY = (-80).dp,
            phase = float,
        )
        FloatingWidget(
            icon = PFIcons.Streak,
            title = stringResource(R.string.streak),
            offsetX = 100.dp,
            offsetY = 80.dp,
            tint = PFTheme.colors.hard,
            phase = 1f - float,
        )
    }
}

@Composable
private fun FloatingWidget(
    icon: ImageVector,
    title: String,
    offsetX: Dp,
    offsetY: Dp,
    phase: Float,
    tint: Color = PFTheme.colors.accent,
) {
    Row(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY + (phase * 12f - 6f).dp)
            .pfElevation(PFElevation.Card, CircleShape)
            .background(PFTheme.colors.elevatedHigh, CircleShape)
            .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = title, style = PFTheme.type.caption, color = PFTheme.colors.onSurface)
    }
}
