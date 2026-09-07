package com.wapp.paperflipai.designsystem.component

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
import com.wapp.paperflipai.designsystem.theme.PFTheme
import java.util.Locale

/**
 * Compact label chip — the Android counterpart of `PFTagPill.swift`.
 * Source tags ("PDF", "YouTube"), folder labels, plan labels ("PRO"),
 * filter chips.
 */
@Composable
fun PFTagPill(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = PFTheme.colors.accent,
    prominent: Boolean = false,
    uppercase: Boolean = true,
) {
    val foreground = if (prominent) PFTheme.colors.onAccent else tint
    Row(
        modifier = modifier
            .background(if (prominent) tint else tint.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, if (prominent) Color.Transparent else tint.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = PFTheme.spacing.md, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(PFTheme.spacing.xs))
        }
        Text(
            text = if (uppercase) title.uppercase(Locale.getDefault()) else title,
            style = PFTheme.type.caption,
            color = foreground,
            maxLines = 1,
        )
    }
}
