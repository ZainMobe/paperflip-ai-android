package com.wapp.paperflipai.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Email + password sign-in — the Android counterpart of
 * `EmailSignInView.swift`. Includes the passwordless OTP option, the
 * forgot-password path, and a route into sign-up.
 */
@Composable
fun EmailSignInScreen(
    onBack: () -> Unit,
    onChooseSignUp: () -> Unit,
    onCodeSent: (String) -> Unit,
    onForgotPassword: (String) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    val canSubmit = !isWorking && email.contains("@") && password.isNotEmpty()

    PFScreen(topBar = { PFTopBar(title = "", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Column(
                modifier = Modifier.padding(top = PFTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.welcome_back),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.sign_in_to_your_paperflip_account),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                PFTextField(
                    label = stringResource(R.string.email),
                    value = email,
                    onValueChange = { email = it; error = null },
                    placeholder = stringResource(R.string.email_placeholder),
                    icon = PFIcons.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )
                PFTextField(
                    label = stringResource(R.string.password),
                    value = password,
                    onValueChange = { password = it; error = null },
                    placeholder = stringResource(R.string.password_placeholder),
                    icon = PFIcons.Password,
                    isSecure = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    helper = stringResource(R.string.at_least_8_characters_period),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = PFTheme.type.footnoteBold,
                        color = PFTheme.colors.accent,
                        modifier = Modifier
                            .pfPressable(
                                onClick = { onForgotPassword(email) },
                                haptic = PFHaptic.Selection,
                                pressedScale = 0.94f,
                            )
                            .padding(PFTheme.spacing.xs),
                    )
                }
            }

            PFErrorBanner(error)

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                PFButton(
                    title = stringResource(R.string.sign_in),
                    onClick = {
                        scope.launch {
                            isWorking = true
                            error = null
                            try {
                                env.auth.signInWithEmail(email.trim(), password)
                            } catch (failure: Exception) {
                                error = failure.message
                                haptics.perform(PFHaptic.Error)
                            } finally {
                                isWorking = false
                            }
                        }
                    },
                    size = PFButtonSize.Lg,
                    isLoading = isWorking,
                    enabled = canSubmit,
                )

                PFButton(
                    title = stringResource(R.string.email_me_a_code),
                    onClick = {
                        scope.launch {
                            isWorking = true
                            error = null
                            try {
                                env.auth.sendEmailOtp(email.trim())
                                haptics.perform(PFHaptic.Success)
                                onCodeSent(email.trim())
                            } catch (failure: Exception) {
                                error = failure.message
                                haptics.perform(PFHaptic.Error)
                            } finally {
                                isWorking = false
                            }
                        }
                    },
                    variant = PFButtonVariant.Ghost,
                    leadingIcon = PFIcons.Email,
                    enabled = email.contains("@") && !isWorking,
                )
            }

            Spacer(Modifier.height(PFTheme.spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.new_to_paperflip),
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                Spacer(Modifier.padding(horizontal = 2.dp))
                Text(
                    text = stringResource(R.string.create_an_account),
                    style = PFTheme.type.footnoteBold,
                    color = PFTheme.colors.accent,
                    modifier = Modifier
                        .pfPressable(
                            onClick = onChooseSignUp,
                            haptic = PFHaptic.Selection,
                            pressedScale = 0.94f,
                        )
                        .padding(PFTheme.spacing.xs),
                )
            }
        }
    }
}
