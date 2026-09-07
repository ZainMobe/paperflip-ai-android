package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.core.data.ProjectJoinPreviewDto
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFSegmentedControl
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * Mirror of the web's `/projects/join/{token}` page — the Android
 * counterpart of `JoinProjectSheet.swift`. Resolves the token to a
 * preview, then lets a Pro user submit a join request with an optional
 * message and a requested role.
 *
 * Entry points: the `paperflip://projects/join/{token}` deep link, and
 * "Join with link" in the Library projects strip.
 */
@Composable
fun JoinProjectSheet(
    initialToken: String,
    onDismiss: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(initialToken) }
    var preview by remember { mutableStateOf<ProjectJoinPreviewDto?>(null) }
    var isLoading by remember { mutableStateOf(initialToken.isNotBlank()) }
    var previewError by remember { mutableStateOf<String?>(null) }

    var message by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(PFProjectRole.Viewer) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var didSucceed by remember { mutableStateOf(false) }

    suspend fun loadPreview(value: String) {
        val extracted = extractJoinToken(value) ?: return
        isLoading = true
        previewError = null
        try {
            preview = env.remote.previewProjectJoin(extracted)
        } catch (failure: Exception) {
            previewError = failure.message
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(initialToken) {
        if (initialToken.isNotBlank()) loadPreview(initialToken)
    }

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.project_link)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            if (preview == null && !isLoading) {
                PFTextField(
                    label = stringResource(R.string.paste_link_or_token),
                    value = token,
                    onValueChange = { token = it },
                    placeholder = stringResource(R.string.paste_a_paperflip_ai_project_link_or_the_join),
                    icon = PFIcons.Link,
                    imeAction = ImeAction.Done,
                )
                PFButton(
                    title = stringResource(R.string.continue_action),
                    onClick = { scope.launch { loadPreview(token) } },
                    size = PFButtonSize.Lg,
                    enabled = extractJoinToken(token) != null,
                )
            }

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
                        .padding(PFTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PFTheme.colors.accent,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(PFTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.resolving_link),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            PFErrorBanner(previewError)

            preview?.let { value ->
                PFCard(elevation = PFElevation.Card, surface = PFTheme.colors.paper) {
                    Text(
                        text = stringResource(R.string.project_access_link),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.accent,
                    )
                    Spacer(Modifier.size(PFTheme.spacing.sm))
                    Text(
                        text = value.project.name,
                        style = PFTheme.type.title2,
                        color = PFTheme.colors.onSurface,
                        maxLines = 3,
                    )
                    value.project.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.size(PFTheme.spacing.xs))
                        Text(
                            text = it,
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                            maxLines = 4,
                        )
                    }
                    value.project.ownerName?.let { owner ->
                        Spacer(Modifier.size(PFTheme.spacing.xs))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                PFIcons.Person, null,
                                tint = PFTheme.colors.onSurfaceFaint,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(PFTheme.spacing.xs))
                            Text(
                                text = stringResource(R.string.owned_by, owner),
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.onSurfaceFaint,
                            )
                        }
                    }
                }

                when {
                    didSucceed -> InfoCard(
                        icon = PFIcons.Send,
                        tint = PFTheme.colors.success,
                        title = stringResource(R.string.request_to_join),
                        body = stringResource(R.string.they_ll_see_this_invite_in_their_pending_list),
                    )
                    !value.project.joinLinkEnabled -> InfoCard(
                        icon = PFIcons.Warning,
                        tint = PFTheme.colors.warning,
                        title = stringResource(R.string.not_set_up_yet),
                        body = stringResource(R.string.tell_the_owner_why_you_d_like_access),
                    )
                    value.alreadyMember -> InfoCard(
                        icon = PFIcons.CheckCircle,
                        tint = PFTheme.colors.success,
                        title = stringResource(R.string.you_re_all_caught_up),
                        body = stringResource(R.string.your_projects),
                    )
                    value.alreadyPending -> InfoCard(
                        icon = PFIcons.Schedule,
                        tint = PFTheme.colors.accent,
                        title = stringResource(R.string.pending),
                        body = stringResource(R.string.pending_requests),
                    )
                    !value.isPro -> InfoCard(
                        icon = PFIcons.Pro,
                        tint = PFTheme.colors.accent,
                        title = stringResource(R.string.project_workspaces),
                        body = stringResource(R.string.collaborate_on_decks_with_a_team),
                    )
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                            Text(
                                text = stringResource(R.string.role),
                                style = PFTheme.type.overline,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                            PFSegmentedControl(
                                options = listOf(PFProjectRole.Viewer, PFProjectRole.Editor),
                                selected = selectedRole,
                                onSelect = { selectedRole = it },
                                label = { stringResource(it.displayNameRes) },
                            )
                            Text(
                                text = stringResource(
                                    if (selectedRole == PFProjectRole.Editor)
                                        R.string.editors_can_add_and_edit_decks_in_the_project
                                    else R.string.viewers_can_study_decks_but_can_t_modify_them
                                ),
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.onSurfaceFaint,
                            )
                        }

                        PFTextField(
                            label = stringResource(R.string.message_optional),
                            value = message,
                            onValueChange = { message = it },
                            placeholder = stringResource(R.string.tell_the_owner_why_you_d_like_access),
                            singleLine = false,
                            minLines = 3,
                            imeAction = ImeAction.Done,
                        )

                        PFErrorBanner(submitError)

                        PFButton(
                            title = stringResource(R.string.request_to_join),
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    submitError = null
                                    try {
                                        env.remote.requestProjectJoin(
                                            token = extractJoinToken(token) ?: token,
                                            message = message.trim().ifEmpty { null },
                                            requestedRole = selectedRole,
                                        )
                                        didSucceed = true
                                    } catch (failure: Exception) {
                                        submitError = failure.message
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            size = PFButtonSize.Lg,
                            trailingIcon = PFIcons.Send,
                            isLoading = isSubmitting,
                            enabled = !isSubmitting,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, tint: Color, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
            Text(text = body, style = PFTheme.type.footnote, color = PFTheme.colors.onSurfaceMuted)
        }
    }
}

/**
 * Extracts a join token from a full URL (paperflip.ai or the paperflip://
 * scheme) or a bare token — the Android counterpart of
 * `LibraryHome.extractJoinToken`.
 */
fun extractJoinToken(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    val path = trimmed.substringAfter("://", trimmed)
    val segments = path.split('/').filter { it.isNotBlank() }
    val joinIndex = segments.indexOfFirst { it.equals("join", ignoreCase = true) }
    if (joinIndex >= 0 && joinIndex + 1 < segments.size) return segments[joinIndex + 1]

    if (trimmed.length >= 16 && trimmed.all { it.isLetterOrDigit() || it == '-' }) return trimmed
    return null
}
