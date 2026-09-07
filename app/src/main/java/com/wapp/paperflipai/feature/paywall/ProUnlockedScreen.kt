package com.wapp.paperflipai.feature.paywall

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.study.ConfettiBurst
import com.wapp.paperflipai.feature.study.ConfettiIntensity

/**
 * The celebration after an upgrade — the Android counterpart of
 * `ProUnlockedView.swift`.
 */
private data class UnlockedPerk(val icon: ImageVector, val titleRes: Int)

private val unlockedPerks = listOf(
    UnlockedPerk(PFIcons.Sparkle, R.string.unlimited_deck_generation),
    UnlockedPerk(PFIcons.Deck, R.string.up_to_50_cards_per_deck),
    UnlockedPerk(PFIcons.Group, R.string.project_workspaces),
    UnlockedPerk(PFIcons.Public, R.string.public_deck_sharing),
    UnlockedPerk(PFIcons.Grid, R.string.home_screen_widgets),
)

@Composable
fun ProUnlockedScreen(onDone: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "proPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "proPulseScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to PFTheme.colors.accentSoft,
                    0.6f to PFTheme.colors.surface,
                    1f to PFTheme.colors.surface,
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size((132 * pulse).dp)
                    .background(PFTheme.colors.accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PFIcons.Pro,
                    contentDescription = null,
                    tint = PFTheme.colors.accent,
                    modifier = Modifier.size(56.dp),
                )
            }

            PFTagPill(
                title = stringResource(R.string.paperflip_pro),
                icon = PFIcons.Sparkle,
                prominent = true,
            )

            Text(
                text = stringResource(R.string.welcome_to_pro),
                style = PFTheme.type.title1,
                color = PFTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.you_ve_unlocked_the_full_paperflip_experience),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PFTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.what_s_unlocked),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                unlockedPerks.forEach { perk ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PFTheme.colors.success.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PFIcons.Check,
                                contentDescription = null,
                                tint = PFTheme.colors.success,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(PFTheme.spacing.md))
                        Text(
                            text = stringResource(perk.titleRes),
                            style = PFTheme.type.body,
                            color = PFTheme.colors.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            PFButton(
                title = stringResource(R.string.let_s_go),
                onClick = onDone,
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
            )
        }

        ConfettiBurst(intensity = ConfettiIntensity.Epic, modifier = Modifier.fillMaxSize())
    }
}
