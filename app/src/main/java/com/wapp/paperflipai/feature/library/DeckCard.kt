package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFolder
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import com.wapp.paperflipai.util.asRelativeLabel

/**
 * Visual representation of a deck — the Android counterpart of
 * `DeckCardView.swift`. Two layouts share the same data:
 *  - [DeckCardLayout.Grid] — square-ish, for the adaptive grid
 *  - [DeckCardLayout.List] — full-width row
 *
 * Long-press opens the actions menu, matching the iOS context menu.
 */
enum class DeckCardLayout { Grid, List }

@Composable
fun DeckCard(
    deck: PFDeck,
    cardCount: Int,
    folder: PFFolder?,
    layout: DeckCardLayout,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = buildString {
        append(deck.title)
        append(", ")
        append(cardCount)
        append(" cards")
        folder?.let { append(", in ${it.name}") }
    }

    when (layout) {
        DeckCardLayout.Grid -> DeckCardGrid(
            deck, cardCount, folder, onClick, onLongClick,
            modifier.semantics { contentDescription = description },
        )
        DeckCardLayout.List -> DeckCardList(
            deck, cardCount, folder, onClick, onLongClick,
            modifier.semantics { contentDescription = description },
        )
    }
}

@Composable
private fun DeckCardGrid(
    deck: PFDeck,
    cardCount: Int,
    folder: PFFolder?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    val shape: Shape = RoundedCornerShape(PFRadius.xl)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 180.dp)
            .pfElevation(PFElevation.Card, shape)
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, onLongClick = onLongClick, pressedScale = 0.98f)
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceBadge(deck, size = 36.dp, glyph = 18.dp)
            Spacer(Modifier.weight(1f))
            folder?.let { FolderDot(it) }
        }

        Text(
            text = deck.title,
            style = PFTheme.type.headline,
            color = PFTheme.colors.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs)) {
            if (deck.isPublic) PublicPill()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PFIcons.Deck,
                    contentDescription = null,
                    tint = PFTheme.colors.onSurfaceMuted,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.cards, cardCount),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun DeckCardList(
    deck: PFDeck,
    cardCount: Int,
    folder: PFFolder?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    val shape: Shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, onLongClick = onLongClick, pressedScale = 0.99f)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceBadge(deck, size = 52.dp, glyph = 24.dp)
        Spacer(Modifier.width(PFTheme.spacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
        ) {
            Text(
                text = deck.title,
                style = PFTheme.type.headline,
                color = PFTheme.colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.cards, cardCount),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                deck.lastStudiedAt?.let {
                    Dot()
                    Text(
                        text = it.asRelativeLabel(),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
                folder?.let {
                    Dot()
                    FolderDot(it)
                }
            }
        }

        Icon(
            imageVector = PFIcons.Chevron,
            contentDescription = null,
            tint = PFTheme.colors.onSurfaceFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SourceBadge(deck: PFDeck, size: Dp, glyph: Dp) {
    val tint = deck.tint()
    Box(
        modifier = Modifier
            .size(size)
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(PFRadius.md)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(deck.icon, contentDescription = null, tint = tint, modifier = Modifier.size(glyph))
    }
}

@Composable
private fun FolderDot(folder: PFFolder) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(folder.tint(), CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = folder.name,
            style = PFTheme.type.caption,
            color = PFTheme.colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Dot() {
    Text(
        text = " · ",
        style = PFTheme.type.caption,
        color = PFTheme.colors.onSurfaceFaint,
    )
}

@Composable
private fun PublicPill() {
    Row(
        modifier = Modifier
            .background(PFTheme.colors.accentSoft, CircleShape)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = PFIcons.Public,
            contentDescription = null,
            tint = PFTheme.colors.accent,
            modifier = Modifier.size(10.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = stringResource(R.string.public_label),
            style = PFTheme.type.caption,
            color = PFTheme.colors.accent,
        )
    }
}
