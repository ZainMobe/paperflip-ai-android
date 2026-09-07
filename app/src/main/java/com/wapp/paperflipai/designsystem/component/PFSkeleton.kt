package com.wapp.paperflipai.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.modifier.pfShimmer
import com.wapp.paperflipai.designsystem.theme.PFCardShape
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Skeleton-loading shapes — the Android counterpart of `PFSkeleton.swift`.
 * Compose several to recreate the silhouette of the content being loaded;
 * far more polished than a spinner.
 */
@Composable
fun PFSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    radius: Dp = PFRadius.sm,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(PFTheme.colors.onSurface.copy(alpha = 0.06f), shape)
            .pfShimmer(),
    )
}

/** Skeleton silhouette of a deck card. Reused on Library while data loads. */
@Composable
fun PFDeckCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, PFCardShape)
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PFSkeleton(width = 60.dp, height = 18.dp, radius = PFRadius.pill)
            Spacer(Modifier.weight(1f))
            PFSkeleton(width = 24.dp, height = 24.dp, radius = PFRadius.sm)
        }
        PFSkeleton(height = 22.dp)
        PFSkeleton(width = 180.dp, height = 14.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            modifier = Modifier.padding(top = PFTheme.spacing.xs),
        ) {
            PFSkeleton(width = 70.dp, height = 14.dp, radius = PFRadius.pill)
            PFSkeleton(width = 50.dp, height = 14.dp, radius = PFRadius.pill)
        }
    }
}
