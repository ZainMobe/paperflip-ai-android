package com.wapp.paperflipai.feature.study

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlin.random.Random

/**
 * The end-of-session confetti — the Android counterpart of the private
 * `ConfettiBurst` in `StudySummaryView.swift`. One Canvas rather than
 * dozens of composables, so a 60-piece "epic" burst still draws in a
 * single pass.
 */
enum class ConfettiIntensity(val pieces: Int) {
    Good(25), Great(40), Epic(60),
}

private data class ConfettiPiece(
    val colorIndex: Int,
    val size: Float,
    val startX: Float,
    val endX: Float,
    val endY: Float,
    val rotation: Float,
    val delay: Float,
    val duration: Float,
    val shape: Int,
)

@Composable
fun ConfettiBurst(
    intensity: ConfettiIntensity,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        PFTheme.colors.accent,
        PFTheme.colors.success,
        PFTheme.colors.hard,
        PFTheme.colors.warning,
        PFTheme.colors.danger,
    )

    val pieces = remember(intensity) {
        val random = Random(intensity.pieces)
        List(intensity.pieces) { index ->
            ConfettiPiece(
                colorIndex = index % 5,
                size = random.nextFloat() * 5f + 5f,
                startX = random.nextFloat() * 0.4f + 0.3f,
                endX = random.nextFloat() * 1.2f - 0.1f,
                endY = random.nextFloat() * 0.5f + 0.65f,
                rotation = random.nextFloat() * 540f + 180f,
                delay = random.nextFloat() * 0.4f,
                duration = random.nextFloat() * 1.4f + 1.8f,
                shape = random.nextInt(3),
            )
        }
    }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 3200, easing = LinearEasing),
        label = "confetti",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val local = ((progress - piece.delay) / (piece.duration / 3.2f)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val eased = 1f - (1f - local) * (1f - local)
            val x = (piece.startX + (piece.endX - piece.startX) * eased) * size.width
            val y = (-0.05f + (piece.endY + 0.05f) * eased) * size.height
            val alpha = (1f - local).coerceIn(0f, 1f)
            val color = colors[piece.colorIndex].copy(alpha = alpha)

            rotate(degrees = piece.rotation * eased, pivot = Offset(x, y)) {
                when (piece.shape) {
                    0 -> drawRect(
                        color = color,
                        topLeft = Offset(x - piece.size / 2f, y - piece.size * 0.8f),
                        size = Size(piece.size, piece.size * 1.6f),
                    )
                    1 -> drawCircle(color = color, radius = piece.size / 2f, center = Offset(x, y))
                    else -> drawRect(
                        color = color,
                        topLeft = Offset(x - piece.size * 0.25f, y - piece.size),
                        size = Size(piece.size * 0.5f, piece.size * 2f),
                    )
                }
            }
        }
    }
}
