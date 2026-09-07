package com.wapp.paperflipai.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * A single row in the deck detail card list — the Android counterpart of
 * `FlashcardRow.swift`. The front is always visible; tapping expands the
 * row to reveal the answer inline, with the chevron rotating to match.
 */
@Composable
fun FlashcardRow(
    card: PFFlashcard,
    index: Int,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = PFMotion.snappy(),
        label = "flashcardChevron",
    )
    val shape = RoundedCornerShape(PFRadius.lg)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(
                onClick = { expanded = !expanded },
                onLongClick = onLongClick,
                haptic = PFHaptic.Selection,
                pressedScale = 0.995f,
            )
            .padding(PFTheme.spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(PFTheme.colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.accent,
                )
            }
            Spacer(Modifier.width(PFTheme.spacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = card.front,
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurface,
                )
                if (!expanded) {
                    Text(
                        text = card.back,
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                imageVector = PFIcons.Expand,
                contentDescription = null,
                tint = PFTheme.colors.onSurfaceFaint,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Box(
                    Modifier
                        .padding(vertical = PFTheme.spacing.md)
                        .fillMaxWidth()
                        .height(0.7.dp)
                        .background(PFTheme.colors.divider)
                )
                Text(
                    text = stringResource(R.string.answer),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.accent,
                )
                Text(
                    text = card.back,
                    style = PFTheme.type.body,
                    color = PFTheme.colors.onSurface,
                )
                card.hint?.let { hint ->
                    Row(
                        modifier = Modifier.padding(top = PFTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = PFIcons.Idea,
                            contentDescription = null,
                            tint = PFTheme.colors.warning,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(PFTheme.spacing.xs))
                        Text(
                            text = hint,
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.warning,
                        )
                    }
                }
            }
        }
    }
}
