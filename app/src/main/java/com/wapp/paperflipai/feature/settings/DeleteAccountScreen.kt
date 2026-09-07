package com.wapp.paperflipai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.openPlaySubscriptions

/**
 * Account deletion — the Android counterpart of `DeleteAccountView.swift`
 * (and what Google Play's data-deletion policy requires). Full disclaimer,
 * a link out to Play for the subscription, then a type-to-confirm gate
 * before the destructive button.
 */
private const val REQUIRED_PHRASE = "DELETE"

@Composable
fun DeleteAccountScreen(onBack: () -> Unit) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current

    var confirmInput by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    val matches = confirmInput.trim().equals(REQUIRED_PHRASE, ignoreCase = true)
    val deleteTitle = stringResource(R.string.delete_my_account)
    val deleteMessage = stringResource(R.string.your_decks_projects_and_study_history_will_be)
    val deleteConfirm = stringResource(R.string.delete_forever)
    val keepLabel = stringResource(R.string.keep_my_account)

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.delete_account), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            // ── Header ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PFTheme.colors.danger.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PFIcons.Delete,
                        contentDescription = null,
                        tint = PFTheme.colors.danger,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.deleting_your_account_is_permanent),
                        style = PFTheme.type.title3,
                        color = PFTheme.colors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.this_can_t_be_undone_read_carefully_before_you),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            // ── What gets deleted ─────────────────────────────────────
            Text(
                text = stringResource(R.string.what_gets_deleted),
                style = PFTheme.type.overline,
                color = PFTheme.colors.onSurfaceMuted,
            )
            DisclaimerCard {
                Bullet(stringResource(R.string.your_profile_display_name_and_avatar))
                Bullet(stringResource(R.string.every_deck_and_flashcard_you_ve_created))
                Bullet(stringResource(R.string.folders_projects_and_project_memberships))
                Bullet(stringResource(R.string.study_history_streaks_stats_and_review_schedul))
                Bullet(stringResource(R.string.notifications_push_tokens_and_support_chat_his))
                Bullet(stringResource(R.string.any_projects_you_own_including_for_collaborato))
                Bullet(stringResource(R.string.pending_invites_you_ve_sent))
            }

            Text(
                text = stringResource(R.string.what_s_kept),
                style = PFTheme.type.overline,
                color = PFTheme.colors.onSurfaceMuted,
            )
            DisclaimerCard {
                Text(
                    text = stringResource(R.string.anonymized_usage_data_we_keep_for_security_and),
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }

            // ── Subscription notice ───────────────────────────────────
            Text(
                text = stringResource(R.string.subscription),
                style = PFTheme.type.overline,
                color = PFTheme.colors.onSurfaceMuted,
            )
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = PFIcons.Warning,
                        contentDescription = null,
                        tint = PFTheme.colors.warning,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(PFTheme.spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.deleting_this_account_does_not_cancel_your_sub),
                            style = PFTheme.type.bodyEmphasis,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.delete_account_billing_note_android),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PFTheme.colors.accentSoft, RoundedCornerShape(PFRadius.md))
                        .pfPressable(
                            onClick = { context.openPlaySubscriptions(context.packageName) },
                            pressedScale = 0.98f,
                        )
                        .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PFIcons.OpenExternal,
                        contentDescription = null,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(PFTheme.spacing.xs))
                    Text(
                        text = stringResource(R.string.manage_subscriptions_in_play),
                        style = PFTheme.type.footnoteBold,
                        color = PFTheme.colors.accent,
                    )
                }
            }

            // ── Type-to-confirm ───────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.confirm),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                Row {
                    Text(
                        text = stringResource(R.string.type),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    Text(
                        text = REQUIRED_PHRASE,
                        style = PFTheme.type.footnoteBold,
                        color = PFTheme.colors.danger,
                    )
                    Text(
                        text = stringResource(R.string.below_to_enable_the_delete_button),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
                PFTextField(
                    label = "",
                    value = confirmInput,
                    onValueChange = { confirmInput = it },
                    placeholder = REQUIRED_PHRASE,
                    icon = PFIcons.Delete,
                    imeAction = ImeAction.Done,
                )
            }

            PFErrorBanner(error)

            PFButton(
                title = stringResource(R.string.delete_my_account),
                onClick = {
                    confirmation = PFConfirmation(
                        title = deleteTitle,
                        message = deleteMessage,
                        icon = PFIcons.Delete,
                        confirmTitle = deleteConfirm,
                        cancelTitle = keepLabel,
                        destructive = true,
                    ) {
                        isDeleting = true
                        try {
                            env.auth.deleteAccount()
                            env.onSignedOut()
                        } catch (failure: Exception) {
                            error = failure.message
                        } finally {
                            isDeleting = false
                        }
                    }
                },
                size = PFButtonSize.Lg,
                leadingIcon = PFIcons.Delete,
                isDestructive = true,
                isLoading = isDeleting,
                enabled = matches && !isDeleting,
            )

            PFButton(
                title = stringResource(R.string.keep_my_account),
                onClick = onBack,
                variant = PFButtonVariant.Ghost,
            )
        }
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

@Composable
private fun DisclaimerCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
        content = content,
    )
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .background(PFTheme.colors.danger, CircleShape)
        )
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = text, style = PFTheme.type.footnote, color = PFTheme.colors.onSurfaceMuted)
    }
}
