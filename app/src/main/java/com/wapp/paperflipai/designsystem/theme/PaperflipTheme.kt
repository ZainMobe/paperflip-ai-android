package com.wapp.paperflipai.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The PaperFlip theme.
 *
 * Two layers on purpose:
 *  - [PFTheme] exposes the PaperFlip tokens (`PFTheme.colors.accent`,
 *    `PFTheme.type.title1`, …). Screens use these.
 *  - A derived Material 3 [MaterialTheme] sits underneath so stock M3
 *    components — TopAppBar, ModalBottomSheet, Switch, Slider, TextField —
 *    inherit the brand automatically instead of shipping Material purple.
 */
object PFTheme {
    val colors: PFColors
        @Composable @ReadOnlyComposable get() = LocalPFColors.current

    val type: PFTypography
        @Composable @ReadOnlyComposable get() = LocalPFTypography.current

    val spacing = PFSpacing
    val radius = PFRadius
    val motion = PFMotion
}

/** Shape scale mapped onto the PF radius tokens. */
val PFShapes = Shapes(
    extraSmall = RoundedCornerShape(PFRadius.sm),
    small = RoundedCornerShape(PFRadius.md),
    medium = RoundedCornerShape(PFRadius.lg),
    large = RoundedCornerShape(PFRadius.xl),
    extraLarge = RoundedCornerShape(PFRadius.xxl),
)

val PFCardShape: Shape = RoundedCornerShape(PFRadius.xl)
val PFFieldShape: Shape = RoundedCornerShape(PFRadius.lg)
val PFPillShape: Shape = RoundedCornerShape(PFRadius.pill)

private fun materialSchemeFrom(c: PFColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.accent,
        secondary = c.accent,
        onSecondary = c.onAccent,
        secondaryContainer = c.accentSoft,
        onSecondaryContainer = c.accent,
        tertiary = c.accent,
        onTertiary = c.onAccent,
        background = c.surface,
        onBackground = c.onSurface,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.elevated,
        onSurfaceVariant = c.onSurfaceMuted,
        surfaceContainer = c.elevated,
        surfaceContainerHigh = c.elevatedHigh,
        surfaceContainerHighest = c.elevatedHigh,
        surfaceContainerLow = c.elevated,
        surfaceContainerLowest = c.surface,
        error = c.danger,
        onError = c.onAccent,
        errorContainer = c.danger.copy(alpha = 0.16f),
        onErrorContainer = c.danger,
        outline = c.border,
        outlineVariant = c.divider,
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.accent,
        secondary = c.accent,
        onSecondary = c.onAccent,
        secondaryContainer = c.accentSoft,
        onSecondaryContainer = c.accent,
        tertiary = c.accent,
        onTertiary = c.onAccent,
        background = c.surface,
        onBackground = c.onSurface,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.elevated,
        onSurfaceVariant = c.onSurfaceMuted,
        surfaceContainer = c.elevated,
        surfaceContainerHigh = c.elevatedHigh,
        surfaceContainerHighest = c.elevatedHigh,
        surfaceContainerLow = c.elevated,
        surfaceContainerLowest = c.surface,
        error = c.danger,
        onError = Color.White,
        errorContainer = c.danger.copy(alpha = 0.12f),
        onErrorContainer = c.danger,
        outline = c.border,
        outlineVariant = c.divider,
        scrim = Color.Black,
    )
}

private fun materialTypographyFrom(t: PFTypography) = Typography(
    displayLarge = t.display,
    displayMedium = t.title1,
    displaySmall = t.title2,
    headlineLarge = t.title1,
    headlineMedium = t.title2,
    headlineSmall = t.title3,
    titleLarge = t.title3,
    titleMedium = t.headline,
    titleSmall = t.bodyEmphasis,
    bodyLarge = t.body,
    bodyMedium = t.callout,
    bodySmall = t.footnote,
    labelLarge = t.bodyEmphasis,
    labelMedium = t.footnoteBold,
    labelSmall = t.caption,
)

@Composable
fun PaperflipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PFDarkColors else PFLightColors
    val typography = PFTypography()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Bars are transparent by default at targetSdk 35+; we only
            // need to keep the icon tint in step with the theme.
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalPFColors provides colors,
        LocalPFTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = materialSchemeFrom(colors),
            typography = materialTypographyFrom(typography),
            shapes = PFShapes,
            content = content,
        )
    }
}
