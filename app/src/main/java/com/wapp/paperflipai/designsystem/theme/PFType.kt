package com.wapp.paperflipai.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Type system — the Android counterpart of `PFTypography.swift`.
 *
 * iOS pairs SF Pro Rounded for *display* with SF Pro Text for *body*.
 * Android has no rounded system face, so [PFDisplayFamily] is the single
 * hook for that: drop a rounded TTF (Nunito, Quicksand, Baloo 2…) into
 * `res/font/` and point this at it to get the same warm brand voice.
 * Until then it resolves to the platform sans — still correct, just less
 * distinctive.
 */
val PFDisplayFamily: FontFamily = FontFamily.SansSerif
val PFTextFamily: FontFamily = FontFamily.Default

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun pfStyle(
    size: Int,
    weight: FontWeight,
    family: FontFamily,
    tracking: Float,
    extraLine: Int,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    lineHeight = (size * 1.22f + extraLine).sp,
    lineHeightStyle = Trim,
)

/**
 * The 12 steps of the PaperFlip scale. Sizes match the iOS point values
 * 1:1 — 40/32/26/20/17/15/13/12/11 — because sp and pt land at the same
 * physical size at default font scale.
 */
@Immutable
data class PFTypography(
    /** 40sp, rounded, bold — hero */
    val display: TextStyle = pfStyle(40, FontWeight.Bold, PFDisplayFamily, -0.6f, 4),
    /** 32sp, rounded, bold — screen title */
    val title1: TextStyle = pfStyle(32, FontWeight.Bold, PFDisplayFamily, -0.6f, 3),
    /** 26sp, rounded, semibold — section header */
    val title2: TextStyle = pfStyle(26, FontWeight.SemiBold, PFDisplayFamily, -0.4f, 2),
    /** 20sp, rounded, semibold — card title */
    val title3: TextStyle = pfStyle(20, FontWeight.SemiBold, PFDisplayFamily, -0.2f, 2),
    /** 17sp, semibold — list row title */
    val headline: TextStyle = pfStyle(17, FontWeight.SemiBold, PFTextFamily, 0f, 1),
    /** 17sp, regular — body */
    val body: TextStyle = pfStyle(17, FontWeight.Normal, PFTextFamily, 0f, 2),
    /** 17sp, semibold — body emphasis */
    val bodyEmphasis: TextStyle = pfStyle(17, FontWeight.SemiBold, PFTextFamily, 0f, 2),
    /** 15sp, regular — secondary body */
    val callout: TextStyle = pfStyle(15, FontWeight.Normal, PFTextFamily, 0f, 2),
    /** 13sp, regular — meta */
    val footnote: TextStyle = pfStyle(13, FontWeight.Normal, PFTextFamily, 0f, 1),
    /** 13sp, semibold — meta emphasis */
    val footnoteBold: TextStyle = pfStyle(13, FontWeight.SemiBold, PFTextFamily, 0f, 1),
    /** 12sp, medium — tiny labels / chips */
    val caption: TextStyle = pfStyle(12, FontWeight.Medium, PFTextFamily, 0.2f, 1),
    /** 11sp, semibold, tracked — section eyebrows */
    val overline: TextStyle = pfStyle(11, FontWeight.SemiBold, PFTextFamily, 1.4f, 1),
)

val LocalPFTypography = staticCompositionLocalOf { PFTypography() }
