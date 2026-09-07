package com.wapp.paperflipai.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.wapp.paperflipai.designsystem.component.PFSuccessBanner
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OtpMode { SignIn, PasswordRecovery }

/**
 * Six-digit OTP entry — the Android counterpart of `OTPVerifyView.swift`.
 * Used for passwordless sign-in (60s resend cooldown) and password recovery
 * (120s, matching the web).
 *
 * Rather than iOS's hidden-field trick, this is one [BasicTextField] whose
 * decoration box draws the six cells, so paste, autofill of the SMS/e-mail
 * code, and the software keyboard all behave natively.
 */
@Composable
fun OtpVerifyScreen(
    email: String,
    mode: OtpMode,
    onBack: () -> Unit,
    onRecoveryVerified: () -> Unit = {},
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()
    val focusRequester = remember { FocusRequester() }
    val newCodeMessage = stringResource(R.string.a_new_code_is_on_the_way)

    var code by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resendStatus by remember { mutableStateOf<String?>(null) }
    var resending by remember { mutableStateOf(false) }

    val initialCooldown = if (mode == OtpMode.SignIn) 60 else 120
    var countdown by remember { mutableIntStateOf(initialCooldown) }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    suspend fun verify() {
        if (code.length != 6 || isWorking) return
        isWorking = true
        error = null
        resendStatus = null
        try {
            when (mode) {
                OtpMode.SignIn -> env.auth.verifyEmailOtp(email, code)
                OtpMode.PasswordRecovery -> {
                    env.auth.verifyPasswordResetOtp(email, code)
                    onRecoveryVerified()
                }
            }
            haptics.perform(PFHaptic.Success)
        } catch (failure: Exception) {
            error = failure.message
            code = ""
            haptics.perform(PFHaptic.Error)
        } finally {
            isWorking = false
        }
    }

    LaunchedEffect(code) {
        if (code.length == 6) verify()
    }

    val title = stringResource(
        if (mode == OtpMode.SignIn) R.string.sign_in else R.string.reset_password
    )

    PFScreen(topBar = { PFTopBar(title = title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(top = PFTheme.spacing.md, bottom = PFTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.check_your_email),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.we_sent_a_6_digit_code_to),
                        style = PFTheme.type.callout,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    Text(
                        text = email,
                        style = PFTheme.type.bodyEmphasis,
                        color = PFTheme.colors.onSurface,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.verification_code),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                OtpCells(
                    code = code,
                    onCodeChange = { code = it.filter(Char::isDigit).take(6) },
                    focusRequester = focusRequester,
                )
            }

            PFErrorBanner(error)
            if (error == null) PFSuccessBanner(resendStatus)

            PFButton(
                title = stringResource(R.string.verify_code),
                onClick = { scope.launch { verify() } },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
                isLoading = isWorking,
                enabled = code.length == 6 && !isWorking,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val canResend = countdown == 0 && !resending
                val resendLabel = when {
                    resending -> stringResource(R.string.sending)
                    countdown > 0 -> stringResource(
                        R.string.resend_code_2,
                        if (countdown >= 60) "%d:%02d".format(countdown / 60, countdown % 60)
                        else "${countdown}s",
                    )
                    else -> stringResource(R.string.resend_code)
                }
                Text(
                    text = resendLabel,
                    style = PFTheme.type.footnoteBold,
                    color = if (canResend) PFTheme.colors.accent else PFTheme.colors.onSurfaceFaint,
                    modifier = Modifier
                        .pfPressable(
                            onClick = {
                                scope.launch {
                                    resending = true
                                    error = null
                                    resendStatus = null
                                    try {
                                        when (mode) {
                                            OtpMode.SignIn -> env.auth.sendEmailOtp(email)
                                            OtpMode.PasswordRecovery -> env.auth.sendPasswordReset(email)
                                        }
                                        resendStatus = newCodeMessage
                                        countdown = initialCooldown
                                        haptics.perform(PFHaptic.Success)
                                    } catch (failure: Exception) {
                                        error = failure.message
                                        haptics.perform(PFHaptic.Error)
                                    } finally {
                                        resending = false
                                    }
                                }
                            },
                            enabled = canResend,
                            haptic = PFHaptic.Selection,
                        )
                        .padding(PFTheme.spacing.xs),
                )
                Text(
                    text = " · ",
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceFaint,
                )
                Text(
                    text = stringResource(R.string.change_email),
                    style = PFTheme.type.footnoteBold,
                    color = PFTheme.colors.onSurfaceMuted,
                    modifier = Modifier
                        .pfPressable(onClick = onBack, haptic = PFHaptic.Selection)
                        .padding(PFTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun OtpCells(
    code: String,
    onCodeChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                repeat(6) { index ->
                    val char = code.getOrNull(index)
                    val isActive = index == code.length.coerceAtMost(5)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.md))
                            .border(
                                width = if (isActive) 1.5.dp else 0.7.dp,
                                color = if (isActive) PFTheme.colors.accent else PFTheme.colors.border,
                                shape = RoundedCornerShape(PFRadius.md),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char?.toString().orEmpty(),
                            style = PFTheme.type.title3,
                            color = PFTheme.colors.onSurface,
                        )
                    }
                }
            }
        },
    )
}
