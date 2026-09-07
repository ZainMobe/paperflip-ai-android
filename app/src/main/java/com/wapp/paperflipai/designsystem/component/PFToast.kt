package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Lightweight, top-anchored toast system — the Android counterpart of
 * `PFToast.swift`. From anywhere in the app:
 *
 * ```
 * PFToast.success("Invite sent", "We emailed alice@…")
 * PFToast.error("Couldn't save", err.message)
 * ```
 *
 * The root composable hosts [PFToastHost] once; every call updates the same
 * shared center. Auto-dismisses; tap or swipe up to dismiss early.
 */
enum class PFToastStyle { Success, Error, Warning, Info }

data class PFToastData(
    val id: String = UUID.randomUUID().toString(),
    val style: PFToastStyle,
    val title: String,
    val message: String? = null,
    val durationMillis: Long = 3000L,
)

object PFToast {
    private val _current = MutableStateFlow<PFToastData?>(null)
    val current: StateFlow<PFToastData?> = _current.asStateFlow()

    fun show(style: PFToastStyle, title: String, message: String? = null, durationMillis: Long = 3000L) {
        _current.value = PFToastData(style = style, title = title, message = message, durationMillis = durationMillis)
    }

    fun success(title: String, message: String? = null) = show(PFToastStyle.Success, title, message)
    fun error(title: String, message: String? = null) = show(PFToastStyle.Error, title, message)
    fun warning(title: String, message: String? = null) = show(PFToastStyle.Warning, title, message)
    fun info(title: String, message: String? = null) = show(PFToastStyle.Info, title, message)

    fun dismiss(id: String? = null) {
        if (id == null || _current.value?.id == id) _current.value = null
    }
}

/** Attach once, at the very top of the app's root Box. */
@Composable
fun BoxScope.PFToastHost() {
    val toast by PFToast.current.collectAsStateWithLifecycle()
    val haptics = rememberPFHaptics()

    LaunchedEffect(toast?.id) {
        val t = toast ?: return@LaunchedEffect
        haptics.perform(
            when (t.style) {
                PFToastStyle.Success -> PFHaptic.Success
                PFToastStyle.Error -> PFHaptic.Error
                PFToastStyle.Warning -> PFHaptic.Warning
                PFToastStyle.Info -> PFHaptic.Light
            }
        )
        delay(t.durationMillis)
        PFToast.dismiss(t.id)
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(animationSpec = tween(280)) { -it } + fadeIn(tween(220)),
        exit = slideOutVertically(animationSpec = tween(220)) { -it } + fadeOut(tween(180)),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        toast?.let { t -> PFToastCard(t, onDismiss = { PFToast.dismiss(t.id) }) }
    }
}

@Composable
private fun PFToastCard(toast: PFToastData, onDismiss: () -> Unit) {
    val accent = when (toast.style) {
        PFToastStyle.Success -> PFTheme.colors.success
        PFToastStyle.Error -> PFTheme.colors.danger
        PFToastStyle.Warning -> PFTheme.colors.warning
        PFToastStyle.Info -> PFTheme.colors.accent
    }
    val symbol: ImageVector = when (toast.style) {
        PFToastStyle.Success -> PFIcons.Check
        PFToastStyle.Error -> PFIcons.Error
        PFToastStyle.Warning -> PFIcons.Warning
        PFToastStyle.Info -> PFIcons.Info
    }
    val shape = RoundedCornerShape(PFRadius.lg)

    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.xs)
            .fillMaxWidth()
            .pfElevation(PFElevation.Sheet, shape)
            .background(PFTheme.colors.paper, shape)
            .border(1.dp, accent.copy(alpha = 0.18f), shape)
            .pfPressable(onClick = onDismiss, haptic = null, pressedScale = 0.99f)
            .pointerInput(toast.id) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -6f) onDismiss()
                }
            }
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = toast.title,
                style = PFTheme.type.bodyEmphasis,
                color = PFTheme.colors.onSurface,
                maxLines = 2,
            )
            toast.message?.let {
                Text(
                    text = it,
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                    maxLines = 3,
                )
            }
        }
    }
}
