package com.wapp.paperflipai.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4-point spacing grid — the Android counterpart of `PFSpacing.swift`.
 * Always reach for these instead of raw dp values so spacing scales
 * coherently and stays easy to retune globally.
 */
object PFSpacing {
    /** 2dp — only for icon nudges */
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    /** 16dp — most common gap between items */
    val lg: Dp = 16.dp
    /** 24dp — between sections */
    val xl: Dp = 24.dp
    /** 32dp — major separations */
    val xxl: Dp = 32.dp
    /** 48dp — top-of-screen breathing room */
    val xxxl: Dp = 48.dp
    /** 64dp — hero spacing */
    val huge: Dp = 64.dp

    /** Standard horizontal screen padding. */
    val screen: Dp = lg
}

object PFRadius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    /** Effectively a pill / fully-rounded shape. */
    val pill: Dp = 999.dp
}
