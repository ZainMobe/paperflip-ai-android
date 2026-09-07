package com.wapp.paperflipai.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Elevation system — the Android counterpart of `PFShadow.swift`.
 *
 * Deliberately *soft, low-contrast* shadows in light mode and *no shadow,
 * subtle border instead* in dark mode. That's the trick to a premium
 * minimalist feel rather than the floaty drop-shadow look.
 */
enum class PFElevation {
    /** Flat — no elevation. Use on items inside a list/sheet. */
    Flat,
    /** Subtle card lift. Default for deck cards, tiles. */
    Card,
    /** Pronounced elevation. Use for modal sheets, popovers, tooltips. */
    Sheet,
    /** Hero elevation. Use sparingly — featured cards, paywall hero. */
    Hero,
}

@Composable
fun Modifier.pfElevation(level: PFElevation, shape: Shape): Modifier {
    if (level == PFElevation.Flat || PFTheme.colors.isDark) return this
    val (elevation, spotAlpha, ambientAlpha) = when (level) {
        PFElevation.Card -> Triple(6f, 0.10f, 0.05f)
        PFElevation.Sheet -> Triple(18f, 0.16f, 0.08f)
        PFElevation.Hero -> Triple(30f, 0.20f, 0.10f)
        PFElevation.Flat -> Triple(0f, 0f, 0f)
    }
    return this.shadow(
        elevation = elevation.dp,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = ambientAlpha),
        spotColor = Color.Black.copy(alpha = spotAlpha),
    )
}
