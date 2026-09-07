package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFFieldShape
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Branded text field — the Android counterpart of `PFTextField.swift`.
 * Label, optional leading icon, focus glow, inline error state, and a
 * password reveal toggle (an Android convention iOS doesn't use).
 */
@Composable
fun PFTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    icon: ImageVector? = null,
    isSecure: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    error: String? = null,
    helper: String? = null,
    focusRequester: FocusRequester? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = PFTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var revealed by remember { mutableStateOf(false) }

    val strokeColor by animateColorAsState(
        targetValue = when {
            error != null -> colors.danger
            focused -> colors.accent
            else -> colors.border
        },
        animationSpec = PFMotion.snappy(),
        label = "pfFieldStroke",
    )
    val strokeWidth by animateDpAsState(
        targetValue = if (focused || error != null) 1.5.dp else 0.7.dp,
        animationSpec = PFMotion.snappy(),
        label = "pfFieldStrokeWidth",
    )

    val selectionColors = TextSelectionColors(
        handleColor = colors.accent,
        backgroundColor = colors.accent.copy(alpha = 0.25f),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = PFTheme.type.footnoteBold,
                color = colors.onSurfaceMuted,
            )
            Spacer(Modifier.height(PFTheme.spacing.sm))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(52.dp) else Modifier.heightIn(min = 52.dp))
                .background(colors.elevated, PFFieldShape)
                .border(strokeWidth, strokeColor, PFFieldShape)
                .padding(horizontal = PFTheme.spacing.lg, vertical = if (singleLine) 0.dp else PFTheme.spacing.md),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.Start,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (focused) colors.accent else colors.onSurfaceMuted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.md))
            }

            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = PFTheme.type.body.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.accent),
                    interactionSource = interactionSource,
                    visualTransformation = if (isSecure && !revealed) PasswordVisualTransformation()
                    else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction,
                        autoCorrectEnabled = keyboardType == KeyboardType.Text,
                    ),
                    keyboardActions = keyboardActions,
                    decorationBox = { inner ->
                        Box(
                            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                        ) {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = PFTheme.type.body,
                                    color = colors.onSurfaceFaint,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            if (isSecure) {
                Spacer(Modifier.width(PFTheme.spacing.sm))
                Icon(
                    imageVector = if (revealed) PFIcons.Hide else PFIcons.Reveal,
                    contentDescription = null,
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier
                        .size(22.dp)
                        .pfPressable(onClick = { revealed = !revealed }, pressedScale = 0.9f),
                )
            }
            trailing?.let {
                Spacer(Modifier.width(PFTheme.spacing.sm))
                it()
            }
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(
                modifier = Modifier.padding(top = PFTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = PFIcons.Error,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.xs))
                Text(
                    text = error.orEmpty(),
                    style = PFTheme.type.footnote,
                    color = colors.danger,
                )
            }
        }

        if (error == null && helper != null) {
            Text(
                text = helper,
                style = PFTheme.type.footnote,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = PFTheme.spacing.sm),
            )
        }
    }
}
