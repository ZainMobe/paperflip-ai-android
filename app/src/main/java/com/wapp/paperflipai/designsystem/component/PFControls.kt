package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Pill segmented control — used for Appearance (System/Light/Dark), study
 * filters, and the Library grid/list toggle.
 */
@Composable
fun <T> PFSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String,
    icon: (@Composable (T) -> ImageVector?)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) PFTheme.colors.elevatedHigh else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = PFMotion.snappy(),
                label = "pfSegmentBg",
            )
            val foreground by animateColorAsState(
                targetValue = if (isSelected) PFTheme.colors.accent else PFTheme.colors.onSurfaceMuted,
                animationSpec = PFMotion.snappy(),
                label = "pfSegmentFg",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(background, RoundedCornerShape(PFRadius.md))
                    .pfPressable(
                        onClick = { onSelect(option) },
                        haptic = PFHaptic.Selection,
                        pressedScale = 0.96f,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.invoke(option)?.let {
                    Icon(it, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(PFTheme.spacing.xs))
                }
                Text(
                    text = label(option),
                    style = PFTheme.type.footnoteBold,
                    color = foreground,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Rounded search field used by Library and the deck detail card list. */
@Composable
fun PFSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(PFTheme.colors.elevated, CircleShape)
            .border(0.7.dp, PFTheme.colors.border, CircleShape)
            .padding(horizontal = PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = PFIcons.Search,
            contentDescription = null,
            tint = PFTheme.colors.onSurfaceFaint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = PFTheme.type.callout, color = PFTheme.colors.onSurfaceFaint)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = PFTheme.type.callout.copy(color = PFTheme.colors.onSurface),
                cursorBrush = SolidColor(PFTheme.colors.accent),
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = PFIcons.Close,
                contentDescription = stringResource(R.string.nav_clear_search),
                tint = PFTheme.colors.onSurfaceMuted,
                modifier = Modifier
                    .size(18.dp)
                    .pfPressable(onClick = { onValueChange("") }, pressedScale = 0.85f),
            )
        }
    }
}

/** The one switch, pre-tinted so toggles never show Material defaults. */
@Composable
fun PFSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = PFTheme.colors.onAccent,
            checkedTrackColor = PFTheme.colors.accent,
            checkedBorderColor = PFTheme.colors.accent,
            uncheckedThumbColor = PFTheme.colors.onSurfaceFaint,
            uncheckedTrackColor = PFTheme.colors.elevatedHigh,
            uncheckedBorderColor = PFTheme.colors.border,
        ),
    )
}
