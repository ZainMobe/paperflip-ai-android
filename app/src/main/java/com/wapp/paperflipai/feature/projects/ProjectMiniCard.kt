package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFProject
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Compact project tile for the Library's horizontal projects strip — the
 * Android counterpart of `ProjectMiniCard.swift`.
 */
@Composable
fun ProjectMiniCard(
    project: PFProject,
    deckCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Column(
        modifier = modifier
            .width(160.dp)
            .height(124.dp)
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.97f)
            .padding(PFTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PFIcons.Group,
                contentDescription = null,
                tint = PFTheme.colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = project.name,
            style = PFTheme.type.bodyEmphasis,
            color = PFTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (deckCount == 1) "1 deck" else stringResource(R.string.decks, deckCount),
            style = PFTheme.type.caption,
            color = PFTheme.colors.onSurfaceMuted,
        )
    }
}
