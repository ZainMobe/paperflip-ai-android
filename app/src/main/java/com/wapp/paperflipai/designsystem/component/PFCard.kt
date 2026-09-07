package com.wapp.paperflipai.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation

/**
 * Generic content container — the Android counterpart of `PFCard.swift`.
 * Rounded corners, elevated surface, hairline border, optional tap action
 * with spring-press feedback.
 */
@Composable
fun PFCard(
    modifier: Modifier = Modifier,
    padding: Dp = PFTheme.spacing.lg,
    radius: Dp = PFRadius.xl,
    elevation: PFElevation = PFElevation.Card,
    surface: Color = PFTheme.colors.elevated,
    borderColor: Color = PFTheme.colors.border,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var base = modifier
        .fillMaxWidth()
        .pfElevation(elevation, shape)
        .clip(shape)
        .background(surface, shape)
        .border(0.7.dp, borderColor, shape)

    if (onClick != null) {
        base = base.pfPressable(
            onClick = onClick,
            pressedScale = 0.985f,
            onLongClick = onLongClick,
        )
    }

    Column(modifier = base.padding(padding), content = content)
}
