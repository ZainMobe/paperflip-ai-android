package com.wapp.paperflipai.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Animated circular progress ring — the Android counterpart of
 * `PFProgressRing.swift`. Used during AI deck generation, study session
 * progress, and the onboarding step indicator.
 */
@Composable
fun PFProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    lineWidth: Dp = 6.dp,
    trackColor: Color = PFTheme.colors.divider,
    tint: Color = PFTheme.colors.accent,
    showsLabel: Boolean = true,
) {
    val clamped = max(0.001f, min(1f, progress))
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = PFMotion.snappy(),
        label = "pfRingProgress",
    )

    Box(
        modifier = modifier
            .size(size)
            // TalkBack announces "x percent" instead of walking into an
            // unlabelled Canvas, matching the iOS `.accessibilityValue`.
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(animated, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = lineWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = tint,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (showsLabel) {
            Text(
                text = "${(animated * 100).roundToInt()}%",
                style = PFTheme.type.footnoteBold,
                color = PFTheme.colors.onSurface,
            )
        }
    }
}

/** Indeterminate spinning variant — for unknown-duration work. */
@Composable
fun PFIndeterminateRing(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    lineWidth: Dp = 3.dp,
    tint: Color = PFTheme.colors.accent,
) {
    val transition = rememberInfiniteTransition(label = "pfIndeterminate")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "pfIndeterminateRotation",
    )
    Canvas(modifier.size(size)) {
        val stroke = lineWidth.toPx()
        val inset = stroke / 2f
        rotate(rotation) {
            drawArc(
                color = tint,
                startAngle = 0f,
                sweepAngle = 252f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
