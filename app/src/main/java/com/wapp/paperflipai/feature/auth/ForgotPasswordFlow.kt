package com.wapp.paperflipai.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlinx.coroutines.launch

/**
 * Three-step recovery: email → OTP → new password — the Android counterpart
 * of `ForgotPasswordFlow.swift`. Verifying the recovery code signs the user
 * in transiently so the password update at step 3 succeeds; afterwards they
 * are signed out so they have to use the new password.
 */
private enum class RecoveryStep { Email, Otp, NewPassword, Done }

@Composable
fun ForgotPasswordFlow(
    initialEmail: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()

    var step by remember { mutableStateOf(RecoveryStep.Email) }
    var email by remember { mutableStateOf(initialEmail) }
    var error by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
        label = "recoveryStep",
    ) { current ->
        when (current) {
            RecoveryStep.Email -> RecoveryEmailStep(
                email = email,
                onEmailChange = { email = it; error = null },
                error = error,
                isWorking = isWorking,
                onBack = onBack,
                onSend = {
                    scope.launch {
                        isWorking = true
                        error = null
                        try {
                            env.auth.sendPasswordReset(email.trim())
                            haptics.perform(PFHaptic.Success)
                            step = RecoveryStep.Otp
                        } catch (failure: Exception) {
                            error = failure.message
                            haptics.perform(PFHaptic.Error)
                        } finally {
                            isWorking = false
                        }
                    }
                },
            )

            RecoveryStep.Otp -> OtpVerifyScreen(
                email = email.trim(),
                mode = OtpMode.PasswordRecovery,
                onBack = { step = RecoveryStep.Email },
                onRecoveryVerified = { step = RecoveryStep.NewPassword },
            )

            RecoveryStep.NewPassword -> NewPasswordStep(
                error = error,
                isWorking = isWorking,
                onBack = { step = RecoveryStep.Otp },
                onSave = { newPassword ->
                    scope.launch {
                        isWorking = true
                        error = null
                        try {
                            env.auth.updatePassword(newPassword)
                            // Sign out so they land back on auth and have to
                            // sign in fresh with the new password.
                            env.auth.signOut()
                            haptics.perform(PFHaptic.Success)
                            step = RecoveryStep.Done
                        } catch (failure: Exception) {
                            error = failure.message
                            haptics.perform(PFHaptic.Error)
                        } finally {
                            isWorking = false
                        }
                    }
                },
            )

            RecoveryStep.Done -> RecoveryDoneStep(onFinish = onDone)
        }
    }
}

// ── 1. Email ─────────────────────────────────────────────────────────

@Composable
private fun RecoveryEmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    error: String?,
    isWorking: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
) {
    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.reset_password), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.reset_your_password),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.enter_your_email_and_we_ll_send_you_a_6_digit),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }

            PFTextField(
                label = stringResource(R.string.email),
                value = email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.email_placeholder),
                icon = PFIcons.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            )

            PFErrorBanner(error)

            PFButton(
                title = stringResource(R.string.send_code),
                onClick = onSend,
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
                isLoading = isWorking,
                enabled = email.contains("@") && !isWorking,
            )
        }
    }
}

// ── 3. New password ──────────────────────────────────────────────────

@Composable
private fun NewPasswordStep(
    error: String?,
    isWorking: Boolean,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
) {
    var newPassword by remember { mutableStateOf("") }
    val strength = passwordStrength(newPassword)

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.set_new_password), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.set_new_password),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.choose_a_strong_password_for_your_account),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs)) {
                PFTextField(
                    label = stringResource(R.string.new_password),
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = stringResource(R.string.at_least_8_characters),
                    icon = PFIcons.Password,
                    isSecure = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (index < strength.score) strength.color()
                                    else PFTheme.colors.divider,
                                    CircleShape,
                                )
                        )
                    }
                }
                if (newPassword.isNotEmpty()) {
                    Text(
                        text = stringResource(strength.labelRes),
                        style = PFTheme.type.caption,
                        color = strength.color(),
                    )
                }
            }

            PFErrorBanner(error)

            PFButton(
                title = stringResource(R.string.update_password),
                onClick = { onSave(newPassword) },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Check,
                isLoading = isWorking,
                enabled = newPassword.length >= 8 && !isWorking,
            )
        }
    }
}

private enum class PasswordStrength(val score: Int, val labelRes: Int) {
    TooShort(0, R.string.too_short),
    Weak(1, R.string.weak),
    Fair(2, R.string.fair),
    Good(3, R.string.good),
    Strong(4, R.string.strong);

    @Composable
    fun color(): Color = when (this) {
        TooShort -> PFTheme.colors.onSurfaceFaint
        Weak -> PFTheme.colors.danger
        Fair -> PFTheme.colors.hard
        Good -> PFTheme.colors.warning
        Strong -> PFTheme.colors.success
    }
}

private fun passwordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() || it in "!@#$%^&*" }) score++
    return PasswordStrength.entries.firstOrNull { it.score == score } ?: PasswordStrength.Strong
}

// ── 4. Done ──────────────────────────────────────────────────────────

@Composable
private fun RecoveryDoneStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface)
            .navigationBarsPadding()
            .pfReadableWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(120.dp)
                .pfElevation(PFElevation.Card, CircleShape)
                .background(PFTheme.colors.success.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PFIcons.Check,
                contentDescription = null,
                tint = PFTheme.colors.success,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(PFTheme.spacing.xl))
        Text(
            text = stringResource(R.string.password_updated),
            style = PFTheme.type.title2,
            color = PFTheme.colors.onSurface,
        )
        Spacer(Modifier.height(PFTheme.spacing.sm))
        Text(
            text = stringResource(R.string.sign_in_with_your_new_password),
            style = PFTheme.type.callout,
            color = PFTheme.colors.onSurfaceMuted,
        )
        Spacer(Modifier.weight(1f))
        PFButton(
            title = stringResource(R.string.back_to_sign_in),
            onClick = onFinish,
            size = PFButtonSize.Lg,
            trailingIcon = PFIcons.Forward,
            modifier = Modifier
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xl),
        )
    }
}
