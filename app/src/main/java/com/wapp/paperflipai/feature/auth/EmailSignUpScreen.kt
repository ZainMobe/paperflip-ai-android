package com.wapp.paperflipai.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.auth.AuthError
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
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Email + password + optional name — the Android counterpart of
 * `EmailSignUpView.swift`. Inline validation puts each error on the field
 * that caused it, exactly like the iOS screen.
 */
@Composable
fun EmailSignUpScreen(onBack: () -> Unit) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var generalError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    val canSubmit = !isWorking && email.contains("@") && password.length >= 8

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
                    text = stringResource(R.string.create_your_account),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.start_building_decks_in_seconds),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                PFTextField(
                    label = stringResource(R.string.your_name),
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = stringResource(R.string.optional),
                    icon = PFIcons.Person,
                    imeAction = ImeAction.Next,
                )
                PFTextField(
                    label = stringResource(R.string.email),
                    value = email,
                    onValueChange = { email = it; emailError = null; generalError = null },
                    placeholder = stringResource(R.string.email_placeholder),
                    icon = PFIcons.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    error = emailError,
                )
                PFTextField(
                    label = stringResource(R.string.password),
                    value = password,
                    onValueChange = { password = it; passwordError = null; generalError = null },
                    placeholder = stringResource(R.string.at_least_8_characters),
                    icon = PFIcons.Password,
                    isSecure = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    error = passwordError,
                    helper = if (passwordError == null) stringResource(R.string.well_never_share_this) else null,
                )
            }

            PFErrorBanner(generalError)

            PFButton(
                title = stringResource(R.string.create_account),
                onClick = {
                    scope.launch {
                        isWorking = true
                        emailError = null
                        passwordError = null
                        generalError = null
                        try {
                            env.auth.signUpWithEmail(
                                email = email.trim(),
                                password = password,
                                fullName = fullName.trim().ifEmpty { null },
                            )
                        } catch (failure: Exception) {
                            haptics.perform(PFHaptic.Error)
                            when (failure) {
                                is AuthError.EmailAlreadyInUse -> emailError = failure.message
                                is AuthError.WeakPassword -> passwordError = failure.message
                                is AuthError.InvalidCredentials ->
                                    emailError = context.getString(R.string.that_email_doesnt_look_right)
                                else -> generalError = failure.message
                            }
                        } finally {
                            isWorking = false
                        }
                    }
                },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
                isLoading = isWorking,
                enabled = canSubmit,
            )

            Text(
                text = stringResource(R.string.by_creating_an_account_you_agree_to_our_terms),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PFTheme.spacing.sm),
            )
        }
    }
}
