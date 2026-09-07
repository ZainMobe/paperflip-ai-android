package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * A single chip in the Library's horizontally-scrolling folder row — the
 * Android counterpart of `FolderChip.swift`. "All" renders with a fixed icon
 * and no backing folder.
 */
@Composable
fun FolderChip(
    label: String,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = PFTheme.colors.accent,
    onLongClick: (() -> Unit)? = null,
) {
    val foreground = if (isActive) PFTheme.colors.onAccent else tint
    Row(
        modifier = modifier
            .background(if (isActive) tint else tint.copy(alpha = 0.10f), CircleShape)
            .border(
                1.dp,
                if (isActive) Color.Transparent else tint.copy(alpha = 0.18f),
                CircleShape,
            )
            .pfPressable(
                onClick = onClick,
                onLongClick = onLongClick,
                haptic = PFHaptic.Selection,
                pressedScale = 0.96f,
            )
            .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = foreground, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(PFTheme.spacing.xs))
        }
        Text(text = label, style = PFTheme.type.footnoteBold, color = foreground, maxLines = 1)
        Spacer(Modifier.width(PFTheme.spacing.xs))
        Text(
            text = count.toString(),
            style = PFTheme.type.caption,
            color = foreground,
            modifier = Modifier
                .background(
                    if (isActive) PFTheme.colors.onAccent.copy(alpha = 0.18f)
                    else tint.copy(alpha = 0.18f),
                    CircleShape,
                )
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}
