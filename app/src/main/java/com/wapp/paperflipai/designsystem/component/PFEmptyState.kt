package com.wapp.paperflipai.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Standardised empty / zero-state — the Android counterpart of
 * `PFEmptyState.swift`. Use everywhere a list could be empty (Library,
 * Search, Stats) for a consistent, reassuring tone.
 */
@Composable
fun PFEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    ctaTitle: String? = null,
    onCta: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PFTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PFTheme.colors.accent,
                modifier = Modifier.size(40.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            modifier = Modifier.padding(horizontal = PFTheme.spacing.lg),
        ) {
            Text(
                text = title,
                style = PFTheme.type.title3,
                color = PFTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }

        if (ctaTitle != null && onCta != null) {
            PFButton(
                title = ctaTitle,
                onClick = onCta,
                fullWidth = false,
                modifier = Modifier.padding(top = PFTheme.spacing.xs),
            )
        }
    }
}
