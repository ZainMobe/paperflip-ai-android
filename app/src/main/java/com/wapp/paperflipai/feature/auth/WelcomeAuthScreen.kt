package com.wapp.paperflipai.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.PFLinks
import com.wapp.paperflipai.core.auth.AuthError
import com.wapp.paperflipai.util.findActivity
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFLottie
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.openUrl
import kotlinx.coroutines.launch

/**
 * First screen of the auth flow — the Android counterpart of
 * `WelcomeAuthView.swift`.
 *
 * Sign in with Apple is replaced by Google as the primary social option,
 * which is the Android convention; email remains the secondary path.
 */
@Composable
fun WelcomeAuthScreen(onChooseEmail: () -> Unit) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Offering a button that can only fail is worse than not offering it:
    // without a web client id every tap ends at Supabase saying "bad id
    // token", which reads like a bug rather than missing configuration.
    val googleAvailable = env.google.isConfigured && activity != null

    var error by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // ── Hero ──────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
            modifier = Modifier.pfReadableWidth(),
        ) {
            PFLottie(
                resId = R.raw.paperflip_logo_lottie,
                speed = 0.6f,
                modifier = Modifier.size(160.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.welcome_to_paperflip),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.sign_in_to_sync_your_decks_and_study_across_al),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // A touch more space below than above so the hero settles just
        // above optical centre, where the eye expects it.
        Spacer(Modifier.weight(2f))

        // ── CTAs ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
        ) {
            PFErrorBanner(error)

            if (googleAvailable) GoogleSignInButton(enabled = !isWorking) {
                scope.launch {
                    isWorking = true
                    error = null
                    try {
                        // Credential Manager renders a bottom sheet, so it
                        // needs the Activity — the application context throws.
                        val host = activity
                            ?: throw AuthError.Unknown("Couldn't open Google sign-in.")
                        val result = env.google.signIn(host)
                        env.auth.signInWithGoogle(result)
                        // The root router observes the session and swaps screens.
                    } catch (cancelled: AuthError.UserCancelled) {
                        // User dismissed the sheet — say nothing.
                    } catch (failure: Exception) {
                        error = failure.message
                    } finally {
                        isWorking = false
                    }
                }
            }

            PFButton(
                title = stringResource(R.string.continue_with_email),
                onClick = onChooseEmail,
                variant = PFButtonVariant.Outline,
                size = PFButtonSize.Lg,
                leadingIcon = PFIcons.Email,
                enabled = !isWorking,
            )

            LegalFootnote(
                onTerms = { context.openUrl(PFLinks.TERMS) },
                onPrivacy = { context.openUrl(PFLinks.PRIVACY) },
            )
        }
    }
}

/**
 * Filled white button with the multi-colour "G" mark — Google's brand
 * guidelines for "Sign in with Google" require the light surface and the
 * unmodified logo, so this one deliberately sits outside the PF palette.
 */
@Composable
private fun GoogleSignInButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .background(Color.White, shape)
            .border(1.dp, Color(0xFFD9D9D9), shape)
            .pfPressable(onClick = onClick, enabled = enabled),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.google_g_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(
            text = stringResource(R.string.continue_with_google),
            style = PFTheme.type.bodyEmphasis,
            color = Color(0xFF2E2E2E),
        )
    }
}

@Composable
private fun LegalFootnote(onTerms: () -> Unit, onPrivacy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PFTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.by_continuing_you_agree_to_paperflip_s),
            style = PFTheme.type.caption,
            color = PFTheme.colors.onSurfaceFaint,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.terms),
                style = PFTheme.type.caption,
                color = PFTheme.colors.accent,
                modifier = Modifier.pfPressable(onClick = onTerms, pressedScale = 0.94f),
            )
            Text(
                text = stringResource(R.string.and),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceFaint,
            )
            Text(
                text = stringResource(R.string.privacy_policy),
                style = PFTheme.type.caption,
                color = PFTheme.colors.accent,
                modifier = Modifier.pfPressable(onClick = onPrivacy, pressedScale = 0.94f),
            )
        }
    }
}
