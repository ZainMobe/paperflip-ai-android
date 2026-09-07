package com.wapp.paperflipai.designsystem.component

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Inline status banners — the recurring `errorBanner` / `successBanner`
 * helpers the iOS screens each declare privately, pulled up into the design
 * system so every screen renders them identically.
 */
@Composable
fun PFInlineBanner(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = PFTheme.colors.danger,
    icon: ImageVector = PFIcons.Error,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(PFRadius.md))
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = text, style = PFTheme.type.footnote, color = tint)
    }
}

@Composable
fun PFErrorBanner(error: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        PFInlineBanner(text = error.orEmpty(), tint = PFTheme.colors.danger)
    }
}

@Composable
fun PFSuccessBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        PFInlineBanner(
            text = message.orEmpty(),
            tint = PFTheme.colors.success,
            icon = PFIcons.CheckCircle,
        )
    }
}

@Composable
fun PFInfoBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        PFInlineBanner(
            text = message.orEmpty(),
            tint = PFTheme.colors.accent,
            icon = PFIcons.Info,
        )
    }
}
