package com.wapp.paperflipai.designsystem.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import com.wapp.paperflipai.designsystem.theme.PFMotion

/**
 * Subtle scale-on-press feedback — the Android counterpart of
 * `SpringPressModifier.swift`. Used on every tappable element so the whole
 * app feels alive without being noisy.
 *
 * Unlike the iOS version this also owns the click, because on Android the
 * press state has to come from the same interaction source the click uses —
 * otherwise a press inside a scrolling list either eats the drag or never
 * releases.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pfPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    haptic: PFHaptic? = PFHaptic.Light,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    rippleIndication: Boolean = false,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = PFMotion.snappy(),
        label = "pfPressScale",
    )
    val haptics = rememberPFHaptics()
    val indication = if (rippleIndication) LocalIndication.current else null

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            onLongClick = {
                haptics.perform(PFHaptic.Medium)
                onLongClick()
            },
            onClick = {
                haptic?.let(haptics::perform)
                onClick()
            },
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            onClick = {
                haptic?.let(haptics::perform)
                onClick()
            },
        )
    }

    return this
        .scale(scale)
        .then(clickModifier)
}
