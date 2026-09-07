package com.wapp.paperflipai.feature.onboarding

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Three-page introduction shown on first launch — the Android counterpart of
 * `OnboardingFlow.swift`. Skippable; the final page hands off to auth.
 */
private data class OnboardingPage(
    val eyebrowRes: Int,
    val titleRes: Int,
    val bodyRes: Int,
    val illustration: @Composable () -> Unit,
)

private val onboardingPages = listOf(
    OnboardingPage(
        R.string.onboarding_1_eyebrow, R.string.onboarding_1_title, R.string.onboarding_1_body,
    ) { OnboardingSourcesIllustration() },
    OnboardingPage(
        R.string.onboarding_2_eyebrow, R.string.onboarding_2_title, R.string.onboarding_2_body,
    ) { OnboardingStudyIllustration() },
    OnboardingPage(
        R.string.onboarding_3_eyebrow, R.string.onboarding_3_title, R.string.onboarding_3_body,
    ) { OnboardingAnywhereIllustration() },
)

@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == onboardingPages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Top bar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = PFTheme.spacing.lg),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = !isLast, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurfaceMuted,
                    modifier = Modifier
                        .pfPressable(onClick = onFinish, haptic = PFHaptic.Selection, pressedScale = 0.94f)
                        .padding(PFTheme.spacing.sm),
                )
            }
        }

        // ── Pager ─────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            val page = onboardingPages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pfReadableWidth()
                    .padding(horizontal = PFTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                page.illustration()
                Spacer(Modifier.height(PFTheme.spacing.xl))
                Text(
                    text = stringResource(page.eyebrowRes),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.accent,
                )
                Spacer(Modifier.height(PFTheme.spacing.md))
                Text(
                    text = stringResource(page.titleRes),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(PFTheme.spacing.md))
                Text(
                    text = stringResource(page.bodyRes),
                    style = PFTheme.type.body,
                    color = PFTheme.colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = PFTheme.spacing.lg),
                )
            }
        }

        // ── Dots + CTA ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pfReadableWidth()
                .padding(bottom = PFTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.padding(top = PFTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                onboardingPages.indices.forEach { index ->
                    val selected = index == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (selected) 26.dp else 8.dp,
                        animationSpec = PFMotion.snappy(),
                        label = "onboardingDot",
                    )
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .background(
                                if (selected) PFTheme.colors.accent else PFTheme.colors.divider,
                                CircleShape,
                            )
                    )
                }
            }

            val ctaIcon: ImageVector =
                if (isLast) PFIcons.Sparkle else PFIcons.Forward
            PFButton(
                title = stringResource(if (isLast) R.string.get_started else R.string.continue_action),
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                size = PFButtonSize.Lg,
                trailingIcon = ctaIcon,
                modifier = Modifier.padding(horizontal = PFTheme.spacing.lg),
            )
        }
    }
}
