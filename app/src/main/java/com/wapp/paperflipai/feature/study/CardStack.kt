package com.wapp.paperflipai.feature.study

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.srs.SRSResponse
import com.wapp.paperflipai.designsystem.theme.PFMotion

/**
 * Up to three visible study cards with depth — the Android counterpart of
 * `CardStackView.swift`. The top card is full size and interactive; the
 * ones behind are scaled down and offset to suggest a stack, and slide
 * forward as the top card flies off.
 */
@Composable
fun CardStack(
    cards: List<PFFlashcard>,
    onResponse: (SRSResponse) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = cards.take(3)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(460.dp),
    ) {
        // Draw back-to-front so the top card ends up on top.
        visible.indices.reversed().forEach { index ->
            val card = visible[index]
            val scale by animateFloatAsState(
                targetValue = when (index) {
                    0 -> 1f
                    1 -> 0.96f
                    else -> 0.92f
                },
                animationSpec = PFMotion.snappy(),
                label = "stackScale",
            )
            val yOffset by animateDpAsState(
                targetValue = when (index) {
                    0 -> 0.dp
                    1 -> 14.dp
                    else -> 26.dp
                },
                animationSpec = PFMotion.snappy(),
                label = "stackOffset",
            )
            val cardAlpha by animateFloatAsState(
                targetValue = when (index) {
                    0 -> 1f
                    1 -> 0.85f
                    else -> 0.65f
                },
                animationSpec = PFMotion.snappy(),
                label = "stackAlpha",
            )

            key(card.id) {
                StudyCard(
                    card = card,
                    interactive = index == 0,
                    onResponse = if (index == 0) onResponse else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex((visible.size - index).toFloat())
                        .offset(y = yOffset)
                        .scale(scale)
                        .alpha(cardAlpha),
                )
            }
        }
    }
}
