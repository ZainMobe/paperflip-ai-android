package com.wapp.paperflipai.feature.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.srs.SRSResponse
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A single flashcard as a flippable, swipeable surface — the Android
 * counterpart of `StudyCardView.swift`.
 *
 * Tap flips the card in 3D. Dragging maps to the four responses
 * (left = Again, right = Good, up = Easy, down = Hard) with an edge glow
 * and a corner badge fading in as the gesture approaches the commit
 * threshold; past it the card flies off and `onResponse` fires.
 *
 * TalkBack users get the same four choices as custom actions, since a
 * screen reader can't perform the drag.
 */
@Composable
fun StudyCard(
    card: PFFlashcard,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onResponse: ((SRSResponse) -> Unit)? = null,
) {
    var flipped by remember(card.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()
    val density = LocalDensity.current

    val offset = remember(card.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val commitThresholdPx = with(density) { 110.dp.toPx() }
    val flyDistancePx = with(density) { 900.dp.toPx() }

    val flipRotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = PFMotion.cardFlip(),
        label = "cardFlip",
    )

    val dragOffset = offset.value
    val direction = primaryDirection(dragOffset)
    val magnitude = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
    val intensity = (magnitude / commitThresholdPx).coerceIn(0f, 1f)
    val glowColor = direction?.color() ?: Color.Transparent

    val questionLabel = stringResource(R.string.question_2)
    val answerLabel = stringResource(R.string.answer_2)
    val actions = SRSResponse.entries.map { response ->
        val label = stringResource(response.labelRes)
        CustomAccessibilityAction(label) {
            if (interactive) onResponse?.invoke(response)
            true
        }
    }
    val showAnswerLabel = stringResource(R.string.show_answer)

    fun commit(target: SwipeDirection) {
        haptics.perform(PFHaptic.Medium)
        val destination = when (target) {
            SwipeDirection.Left -> Offset(-flyDistancePx, 60f)
            SwipeDirection.Right -> Offset(flyDistancePx, 60f)
            SwipeDirection.Up -> Offset(0f, -flyDistancePx)
            SwipeDirection.Down -> Offset(0f, flyDistancePx)
        }
        scope.launch {
            offset.animateTo(destination, tween(300))
            onResponse?.invoke(target.response)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Draw-time translation, deliberately NOT Modifier.offset {}:
            // that overload is rtlAware and places with placeRelative, so in
            // Arabic the card slid away from the finger while rotationZ (which
            // is never mirrored) leaned the other way. graphicsLayer keeps
            // gesture and pixels in the same direction in both layout
            // directions, and skips a relayout on every frame.
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                rotationZ = dragOffset.x * 0.02f
            }
            .semantics {
                contentDescription = if (flipped) "$answerLabel. ${card.back}"
                else "$questionLabel. ${card.front}"
                customActions = listOf(
                    CustomAccessibilityAction(showAnswerLabel) { flipped = true; true }
                ) + actions
            }
            .then(
                if (interactive) Modifier.pfPressable(
                    onClick = { flipped = !flipped },
                    haptic = PFHaptic.Selection,
                    pressedScale = 1f,
                ) else Modifier
            )
            .then(
                if (interactive) Modifier.pointerInput(card.id) {
                    detectDragGestures(
                        onDrag = { change, delta ->
                            change.consume()
                            scope.launch { offset.snapTo(offset.value + delta) }
                        },
                        onDragEnd = {
                            val end = offset.value
                            val dir = primaryDirection(end)
                            val mag = sqrt(end.x * end.x + end.y * end.y)
                            if (dir != null && mag >= commitThresholdPx) {
                                commit(dir)
                            } else {
                                scope.launch { offset.animateTo(Offset.Zero, PFMotion.snappy()) }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(Offset.Zero, PFMotion.snappy()) }
                        },
                    )
                } else Modifier
            ),
    ) {
        // Front face
        CardFace(
            modifier = Modifier.graphicsLayer {
                rotationY = flipRotation
                cameraDistance = 14f * density.density
                alpha = if (flipRotation < 90f) 1f else 0f
            },
        ) {
            CardFront(card)
        }

        // Back face (pre-rotated so it reads the right way round)
        CardFace(
            modifier = Modifier.graphicsLayer {
                rotationY = flipRotation - 180f
                cameraDistance = 14f * density.density
                alpha = if (flipRotation >= 90f) 1f else 0f
            },
        ) {
            CardBack(card)
        }

        // Directional feedback
        if (direction != null && intensity > 0.02f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(4.dp, glowColor.copy(alpha = intensity * 0.9f), RoundedCornerShape(PFRadius.xxl))
            )
        }
        if (direction != null && intensity > 0.18f) {
            Row(
                modifier = Modifier
                    .align(direction.badgeAlignment())
                    .padding(PFTheme.spacing.lg)
                    .graphicsLayer {
                        scaleX = 0.9f + 0.2f * intensity
                        scaleY = 0.9f + 0.2f * intensity
                        alpha = intensity
                    }
                    .background(glowColor, CircleShape)
                    .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = direction.response.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.xs))
                Text(
                    text = stringResource(direction.response.labelRes).uppercase(),
                    style = PFTheme.type.footnoteBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CardFace(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(PFRadius.xxl)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pfElevation(PFElevation.Hero, shape)
            .background(PFTheme.colors.paper, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.xl),
    ) {
        content()
    }
}

@Composable
private fun CardFront(card: PFFlashcard) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PFTagPill(title = stringResource(R.string.question), icon = PFIcons.Help)
            Spacer(Modifier.weight(1f))
            if (card.hint != null) {
                Icon(
                    imageVector = PFIcons.Idea,
                    contentDescription = null,
                    tint = PFTheme.colors.warning,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = card.front,
            style = PFTheme.type.title2,
            color = PFTheme.colors.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PFIcons.Refresh,
                contentDescription = null,
                tint = PFTheme.colors.onSurfaceMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(PFTheme.spacing.xs))
            Text(
                text = stringResource(R.string.tap_to_flip),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun CardBack(card: PFFlashcard) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
    ) {
        PFTagPill(
            title = stringResource(R.string.answer),
            icon = PFIcons.CheckCircle,
            tint = PFTheme.colors.success,
            prominent = true,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = card.back,
            style = PFTheme.type.title2,
            color = PFTheme.colors.onSurface,
        )
        card.hint?.let { hint ->
            Row(
                modifier = Modifier
                    .background(
                        PFTheme.colors.warning.copy(alpha = 0.10f),
                        RoundedCornerShape(PFRadius.md),
                    )
                    .padding(PFTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = PFIcons.Idea,
                    contentDescription = null,
                    tint = PFTheme.colors.warning,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.xs))
                Text(text = hint, style = PFTheme.type.footnote, color = PFTheme.colors.warning)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.swipe_how_it_went),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
    }
}

// ── Swipe direction ──────────────────────────────────────────────────

internal enum class SwipeDirection {
    Left, Right, Up, Down;

    val response: SRSResponse
        get() = when (this) {
            Left -> SRSResponse.Again
            Down -> SRSResponse.Hard
            Right -> SRSResponse.Good
            Up -> SRSResponse.Easy
        }

    /** The badge sits opposite the swipe so the finger never covers it. */
    fun badgeAlignment(): Alignment = when (this) {
        Left -> Alignment.TopEnd
        Right -> Alignment.TopStart
        Up -> Alignment.BottomCenter
        Down -> Alignment.TopCenter
    }

    @Composable
    fun color(): Color = response.color()
}

internal fun primaryDirection(offset: Offset): SwipeDirection? {
    val magnitude = sqrt(offset.x * offset.x + offset.y * offset.y)
    if (magnitude < 8f) return null
    return if (abs(offset.x) > abs(offset.y)) {
        if (offset.x > 0) SwipeDirection.Right else SwipeDirection.Left
    } else {
        if (offset.y > 0) SwipeDirection.Down else SwipeDirection.Up
    }
}
