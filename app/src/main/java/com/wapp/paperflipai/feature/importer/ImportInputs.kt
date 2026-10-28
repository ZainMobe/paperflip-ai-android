package com.wapp.paperflipai.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/*
 * Small building blocks shared by the new import sources (notes, photo,
 * lecture, paste cards). Kept together so the four screens look like one
 * family and the existing SourceInputScreen stays readable.
 */

/** A tall, multi-line editor with a placeholder, styled like PFTextField. */
@Composable
internal fun ImportTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
    accent: Color = PFTheme.colors.accent,
    minHeight: Dp = 200.dp,
    maxHeight: Dp = 420.dp,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(PFRadius.lg)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .background(PFTheme.colors.elevated, shape)
            .border(
                width = if (focused) 1.5.dp else 0.7.dp,
                color = if (focused) accent.copy(alpha = 0.6f) else PFTheme.colors.border,
                shape = shape,
            )
            .padding(PFTheme.spacing.md),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = PFTheme.type.body,
                color = PFTheme.colors.onSurfaceFaint,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = PFTheme.type.body.copy(color = PFTheme.colors.onSurface),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight - 24.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = accessibilityLabel },
        )
    }
}

/** Big square-ish choice tile: icon in a tinted circle above a label. */
@Composable
internal fun RowScope.ImportActionTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(PFRadius.xl)
    Column(
        modifier = Modifier
            .weight(1f)
            .background(PFTheme.colors.elevated, shape)
            .border(1.5.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, enabled = enabled, pressedScale = 0.98f)
            .padding(vertical = PFTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            text = title,
            style = PFTheme.type.footnoteBold,
            color = if (enabled) PFTheme.colors.onSurface else PFTheme.colors.onSurfaceFaint,
        )
    }
}

/** Overline label with an optional trailing caption. */
@Composable
internal fun ImportSectionLabel(title: String, trailing: String? = null, trailingColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = PFTheme.type.overline,
            color = PFTheme.colors.onSurfaceMuted,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                style = PFTheme.type.caption,
                color = trailingColor ?: PFTheme.colors.onSurfaceFaint,
            )
        }
    }
}
