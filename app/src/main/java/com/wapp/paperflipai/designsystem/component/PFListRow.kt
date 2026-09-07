package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * A single settings-style row: tinted icon bubble, title, optional
 * subtitle, and a trailing slot (chevron by default). Rows stack inside a
 * [PFGroupedCard] to make an iOS-style inset grouped list that still reads
 * as Material.
 */
@Composable
fun PFListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = PFTheme.colors.accent,
    titleColor: Color = PFTheme.colors.onSurface,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.pfPressable(
                    onClick = onClick,
                    enabled = enabled,
                    pressedScale = 0.99f,
                ) else Modifier
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(PFRadius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(it, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(PFTheme.spacing.md))
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = PFTheme.type.body,
                color = if (enabled) titleColor else titleColor.copy(alpha = 0.4f),
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }

        when {
            trailing != null -> {
                Spacer(Modifier.width(PFTheme.spacing.sm))
                trailing()
            }
            showChevron && onClick != null -> {
                Icon(
                    imageVector = PFIcons.Chevron,
                    contentDescription = null,
                    tint = PFTheme.colors.onSurfaceFaint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Groups rows on an elevated card with hairline separators between them. */
@Composable
fun PFGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.xl)),
        content = content,
    )
}

/** Hairline separator sized to sit between two [PFListRow]s. */
@Composable
fun PFRowDivider(modifier: Modifier = Modifier, inset: Boolean = true) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (inset) PFTheme.spacing.lg else 0.dp)
            .height(0.7.dp)
            .background(PFTheme.colors.divider),
    )
}
