package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFProject
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Owner-only: invite a collaborator by email with a role — the Android
 * counterpart of `InviteMemberSheet.swift`.
 */
@Composable
fun InviteMemberSheet(
    project: PFProject,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(PFProjectRole.Editor) }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val trimmed = email.trim()
    val canSend = !isSending && trimmed.contains("@")
    val sentTitle = stringResource(R.string.send_invite)

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.invite_teammate)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PFTheme.colors.accentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PFIcons.PersonAdd,
                        contentDescription = null,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = PFTheme.type.bodyEmphasis,
                        color = PFTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.they_ll_see_this_invite_in_their_pending_list),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            PFTextField(
                label = stringResource(R.string.email),
                value = email,
                onValueChange = { email = it; error = null },
                placeholder = "teammate@example.com",
                icon = PFIcons.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            )

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.role),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                    listOf(PFProjectRole.Editor, PFProjectRole.Viewer).forEach { candidate ->
                        RoleCard(
                            role = candidate,
                            selected = role == candidate,
                            onClick = { role = candidate },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            PFErrorBanner(error)

            PFButton(
                title = stringResource(if (isSending) R.string.sending else R.string.send_invite),
                onClick = {
                    scope.launch {
                        isSending = true
                        error = null
                        try {
                            env.remote.inviteProjectMember(project.id, trimmed, role)
                            PFToast.success(sentTitle, trimmed)
                            onSent()
                        } catch (failure: Exception) {
                            error = failure.message
                        } finally {
                            isSending = false
                        }
                    }
                },
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Send,
                isLoading = isSending,
                enabled = canSend,
            )
        }
    }
}

@Composable
private fun RoleCard(
    role: PFProjectRole,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PFRadius.lg)
    val foreground = if (selected) PFTheme.colors.onAccent else PFTheme.colors.onSurface
    Column(
        modifier = modifier
            .heightIn(min = 84.dp)
            .background(if (selected) PFTheme.colors.accent else PFTheme.colors.elevated, shape)
            .border(
                0.7.dp,
                if (selected) androidx.compose.ui.graphics.Color.Transparent else PFTheme.colors.border,
                shape,
            )
            .pfPressable(onClick = onClick, haptic = PFHaptic.Selection, pressedScale = 0.97f)
            .padding(PFTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(role.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(PFTheme.spacing.xs))
            Text(
                text = stringResource(role.displayNameRes),
                style = PFTheme.type.bodyEmphasis,
                color = foreground,
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(
                    imageVector = PFIcons.CheckCircle,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = stringResource(
                if (role == PFProjectRole.Editor) R.string.can_add_edit_delete_decks
                else R.string.can_study_and_view_decks_only
            ),
            style = PFTheme.type.caption,
            color = if (selected) foreground.copy(alpha = 0.85f) else PFTheme.colors.onSurfaceMuted,
        )
    }
}
