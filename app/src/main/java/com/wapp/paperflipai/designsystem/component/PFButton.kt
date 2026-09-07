package com.wapp.paperflipai.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * The one button — the Android counterpart of `PFButton.swift`.
 * Four variants × three sizes covers the entire app. Loading state,
 * optional leading/trailing icons, haptic + spring-press for feedback.
 */
enum class PFButtonVariant {
    /** primary CTA — solid accent fill */
    Filled,
    /** secondary — soft tinted */
    Tonal,
    /** tertiary — bordered, transparent */
    Outline,
    /** text-only — no fill, no border */
    Ghost,
}

enum class PFButtonSize {
    Sm, Md, Lg;

    val height: Dp
        get() = when (this) {
            Sm -> 36.dp; Md -> 48.dp; Lg -> 56.dp
        }
    val horizontalPadding: Dp
        get() = when (this) {
            Sm -> 14.dp; Md -> 20.dp; Lg -> 24.dp
        }
    val iconSize: Dp
        get() = when (this) {
            Sm -> 16.dp; Md -> 19.dp; Lg -> 21.dp
        }
    val radius: Dp
        get() = when (this) {
            Sm -> PFRadius.md; Md -> PFRadius.lg; Lg -> PFRadius.lg
        }
}

@Composable
fun PFButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PFButtonVariant = PFButtonVariant.Filled,
    size: PFButtonSize = PFButtonSize.Md,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    val colors = PFTheme.colors
    val tint = if (isDestructive) colors.danger else colors.accent
    val shape = RoundedCornerShape(size.radius)

    val background = when (variant) {
        PFButtonVariant.Filled -> tint
        PFButtonVariant.Tonal -> if (isDestructive) colors.danger.copy(alpha = 0.12f) else colors.accentSoft
        PFButtonVariant.Outline, PFButtonVariant.Ghost -> Color.Transparent
    }
    val foreground = when (variant) {
        PFButtonVariant.Filled -> colors.onAccent
        else -> tint
    }
    val borderColor = if (variant == PFButtonVariant.Outline) tint.copy(alpha = 0.4f) else Color.Transparent

    val interactive = enabled && !isLoading

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .clip(shape)
            .background(background, shape)
            .then(
                if (variant == PFButtonVariant.Outline)
                    Modifier.border(1.dp, borderColor, shape) else Modifier
            )
            .pfPressable(
                onClick = onClick,
                enabled = interactive,
                haptic = PFHaptic.Light,
            )
            .alpha(if (!enabled) 0.45f else if (isLoading) 0.85f else 1f)
            .padding(horizontal = if (fullWidth) PFTheme.spacing.md else size.horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.iconSize),
                color = foreground,
                strokeWidth = 2.dp,
            )
        } else {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(size.iconSize),
                )
                Spacer(Modifier.width(PFTheme.spacing.sm))
            }
            Text(
                text = title,
                style = if (size == PFButtonSize.Sm) PFTheme.type.footnoteBold else PFTheme.type.bodyEmphasis,
                color = foreground,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            trailingIcon?.let {
                Spacer(Modifier.width(PFTheme.spacing.sm))
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(size.iconSize),
                )
            }
        }
    }
}
