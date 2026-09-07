package com.wapp.paperflipai.designsystem.modifier

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Skeleton-loading shimmer — the Android counterpart of
 * `ShimmerModifier.swift`. Apply to placeholder shapes while data loads.
 */
@Composable
fun Modifier.pfShimmer(active: Boolean = true): Modifier {
    if (!active) return this
    val highlight = if (PFTheme.colors.isDark) Color.White.copy(alpha = 0.10f)
    else Color.White.copy(alpha = 0.75f)

    val transition = rememberInfiniteTransition(label = "pfShimmer")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pfShimmerPhase",
    )

    return this.drawWithContent {
        drawContent()
        val width = size.width
        val start = width * phase
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f to highlight,
                    1f to Color.Transparent,
                ),
                start = Offset(start, 0f),
                end = Offset(start + width * 0.6f, size.height),
            ),
        )
    }
}
