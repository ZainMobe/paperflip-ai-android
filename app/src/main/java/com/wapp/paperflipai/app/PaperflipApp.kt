package com.wapp.paperflipai.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.component.PFLottie
import com.wapp.paperflipai.designsystem.component.PFToastHost
import com.wapp.paperflipai.designsystem.theme.PFAppearance
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.PaperflipTheme
import com.wapp.paperflipai.feature.auth.AuthFlow
import com.wapp.paperflipai.feature.onboarding.OnboardingFlow

/**
 * The composition root — the Android counterpart of `Paperflip_AIApp` plus
 * `RootView`. Provides the environment, applies the user's appearance
 * choice, and hosts the toast overlay once for the whole app.
 */
@Composable
fun PaperflipApp(environment: AppEnvironment) {
    val appearance by environment.settings.appearance.collectAsStateWithLifecycle()
    val darkTheme = when (appearance) {
        PFAppearance.System -> isSystemInDarkTheme()
        PFAppearance.Light -> false
        PFAppearance.Dark -> true
    }

    CompositionLocalProvider(LocalAppEnvironment provides environment) {
        PaperflipTheme(darkTheme = darkTheme) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(PFTheme.colors.surface)
            ) {
                RootRouter()
                PFToastHost()
            }
        }
    }
}

/**
 * Top-level state machine deciding which experience to render:
 * `launch → onboarding → auth → authenticated`.
 *
 * Driven entirely by [AppEnvironment] state, exactly like
 * `RootRouter.swift`. Transitions use a soft fade plus a slight upward
 * move so screen swaps feel intentional rather than abrupt.
 */
private enum class RootStage { Launch, Onboarding, Auth, Authenticated }

@Composable
private fun RootRouter() {
    val env = LocalAppEnvironment.current
    val isBootstrapping by env.isBootstrapping.collectAsStateWithLifecycle()
    val hasOnboarded by env.settings.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val session by env.auth.session.collectAsStateWithLifecycle()

    val stage = when {
        isBootstrapping -> RootStage.Launch
        !hasOnboarded -> RootStage.Onboarding
        session == null -> RootStage.Auth
        else -> RootStage.Authenticated
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            if (targetState == RootStage.Launch) {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            } else {
                (slideInVertically(tween(420)) { it / 8 } + fadeIn(tween(320))) togetherWith
                    fadeOut(tween(200))
            }
        },
        label = "rootStage",
    ) { current ->
        when (current) {
            RootStage.Launch -> LaunchSplash()
            RootStage.Onboarding -> OnboardingFlow(onFinish = env.settings::markOnboardingCompleted)
            RootStage.Auth -> AuthFlow()
            RootStage.Authenticated -> MainScaffold()
        }
    }
}

/**
 * Shown while services finish their cold start. Mirrors the Android 12+
 * system splash so the hand-off is seamless.
 */
@Composable
private fun LaunchSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        PFLottie(
            resId = R.raw.paperflip_ai_cards_lottie,
            modifier = Modifier.size(220.dp),
        )
    }
}
