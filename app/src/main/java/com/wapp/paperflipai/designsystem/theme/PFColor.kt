package com.wapp.paperflipai.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens — the Android counterpart of `PFColor.swift`.
 *
 * Always reach for these (never raw hex or Material defaults) so light &
 * dark mode and any future theming stays consistent.
 *
 * Naming describes the ROLE, not the value:
 *  - [surface]        the main background of a screen
 *  - [elevated]       a card / sheet sitting above surface
 *  - [paper]          flashcard background (slightly warm) — the "paper" in PaperFlip
 *  - [accent]         the brand color, used sparingly for emphasis
 *  - [onSurface]      primary text on [surface] / [elevated]
 *  - [onSurfaceMuted] secondary / supporting text
 *  - [onAccent]       text/icon color when sitting on [accent]
 *  - [divider]        thin separator
 *  - [success] / [warning] / [danger]  state colors
 */
@Immutable
data class PFColors(
    // Surfaces
    val surface: Color,
    val elevated: Color,
    val elevatedHigh: Color,
    val paper: Color,

    // Brand
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,

    // Foregrounds
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceFaint: Color,

    // Strokes
    val divider: Color,
    val border: Color,

    // State
    val success: Color,
    val warning: Color,
    val danger: Color,

    // Study response colors (used by swipe gestures)
    val again: Color,
    val hard: Color,
    val good: Color,
    val easy: Color,

    val isDark: Boolean,
)

val PFLightColors = PFColors(
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFF7F7F9),
    elevatedHigh = Color(0xFFFFFFFF),
    paper = Color(0xFFFBF9F4),

    accent = Color(0xFF5B5BD6),
    accentSoft = Color(0xFFEEEEFB),
    onAccent = Color(0xFFFFFFFF),

    onSurface = Color(0xFF0A0A12),
    onSurfaceMuted = Color(0xFF6B6B7A),
    onSurfaceFaint = Color(0xFFA8A8B2),

    divider = Color(0x12000000),
    border = Color(0x1F000000),

    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),

    again = Color(0xFFDC2626),
    hard = Color(0xFFEA580C),
    good = Color(0xFF5B5BD6),
    easy = Color(0xFF16A34A),

    isDark = false,
)

val PFDarkColors = PFColors(
    surface = Color(0xFF0B0B0F),
    elevated = Color(0xFF16161C),
    elevatedHigh = Color(0xFF1E1E26),
    paper = Color(0xFF1A1814),

    accent = Color(0xFF8A8AFF),
    accentSoft = Color(0xFF252543),
    onAccent = Color(0xFF0B0B0F),

    onSurface = Color(0xFFF3F3F8),
    onSurfaceMuted = Color(0xFF9A9AA8),
    onSurfaceFaint = Color(0xFF6A6A78),

    divider = Color(0x14FFFFFF),
    border = Color(0x24FFFFFF),

    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),

    again = Color(0xFFF87171),
    hard = Color(0xFFFB923C),
    good = Color(0xFF8A8AFF),
    easy = Color(0xFF4ADE80),

    isDark = true,
)

val LocalPFColors = staticCompositionLocalOf { PFLightColors }
