package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFProjectInviteStatus
import com.wapp.paperflipai.core.data.ProjectInviteDto
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.util.asShortDate
import kotlinx.coroutines.launch

/**
 * Inbox of project invites the current user has received — the Android
 * counterpart of `PendingInvitesView.swift`. Accept or decline each; on
 * accept the project list is re-pulled so it shows up in the Library.
 */
@Composable
fun PendingInvitesScreen(
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    val invites = remember { mutableListOf<ProjectInviteDto>().toMutableStateList() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }
    val actionFailedLabel = stringResource(R.string.couldnt_complete_that)

    suspend fun load() {
        val email = env.auth.session.value?.email ?: return
        isLoading = true
        error = null
        try {
            val fetched = env.remote.fetchPendingInvites(email)
                .filter {
                    it.status == PFProjectInviteStatus.Pending.raw &&
                        it.expiresAt > System.currentTimeMillis()
                }
                .sortedByDescending { it.createdAt }
            invites.clear()
            invites.addAll(fetched)
        } catch (failure: Exception) {
            error = failure.message
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    PFScreen(
        topBar = {
            PFTopBar(title = stringResource(R.string.project_invites), onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .pfReadableWidth(),
            contentPadding = PaddingValues(PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            if (isLoading && invites.isEmpty()) {
                item {
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
                            text = stringResource(R.string.loading_invites),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            } else if (invites.isEmpty()) {
                item {
                    PFEmptyState(
                        icon = PFIcons.Email,
                        title = stringResource(R.string.no_invites_right_now),
                        message = stringResource(R.string.when_someone_invites_you_to_a_project_it_shows),
                        modifier = Modifier.padding(top = PFTheme.spacing.xl),
                    )
                }
            } else {
                items(invites, key = { it.id }) { invite ->
                    val role = PFProjectRole.from(invite.role)
                    val isProcessing = processingId == invite.id
                    PFCard(elevation = PFElevation.Card, surface = PFTheme.colors.paper) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PFTheme.colors.accentSoft, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PFIcons.Email,
                                    contentDescription = null,
                                    tint = PFTheme.colors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(PFTheme.spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = invite.projectName ?: stringResource(R.string.project_fallback),
                                    style = PFTheme.type.bodyEmphasis,
                                    color = PFTheme.colors.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.invited_you,
                                        invite.inviterName ?: stringResource(R.string.unknown),
                                    ),
                                    style = PFTheme.type.footnote,
                                    color = PFTheme.colors.onSurfaceMuted,
                                    maxLines = 1,
                                )
                            }
                            PFTagPill(
                                title = stringResource(role.displayNameRes),
                                icon = role.icon,
                                tint = role.tint(),
                            )
                        }

                        Spacer(Modifier.size(PFTheme.spacing.sm))

                        Text(
                            text = stringResource(R.string.expires, invite.expiresAt.asShortDate()),
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceFaint,
                        )

                        Spacer(Modifier.size(PFTheme.spacing.md))

                        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                            PFButton(
                                title = stringResource(R.string.decline),
                                onClick = {
                                    scope.launch {
                                        processingId = invite.id
                                        val result =
                                            runCatching { env.remote.declineProjectInvite(invite.id) }
                                        processingId = null
                                        if (result.isSuccess) {
                                            invites.removeAll { it.id == invite.id }
                                        } else {
                                            PFToast.error(
                                                actionFailedLabel,
                                                result.exceptionOrNull()?.message,
                                            )
                                        }
                                    }
                                },
                                variant = PFButtonVariant.Ghost,
                                fullWidth = false,
                                enabled = !isProcessing,
                            )
                            PFButton(
                                title = stringResource(R.string.accept),
                                onClick = {
                                    scope.launch {
                                        processingId = invite.id
                                        val result = runCatching {
                                            env.remote.acceptProjectInvite(invite.id)
                                            env.auth.session.value?.userId?.let {
                                                env.sync.pullProjects(it)
                                            }
                                        }
                                        processingId = null
                                        // Previously the invite was removed and the
                                        // project opened even when accept threw —
                                        // navigating into a project the user has no
                                        // access to, with the invite gone from the list.
                                        if (result.isSuccess) {
                                            invites.removeAll { it.id == invite.id }
                                            onOpenProject(invite.projectId)
                                        } else {
                                            PFToast.error(
                                                actionFailedLabel,
                                                result.exceptionOrNull()?.message,
                                            )
                                        }
                                    }
                                },
                                trailingIcon = PFIcons.Check,
                                isLoading = isProcessing,
                                fullWidth = false,
                                enabled = !isProcessing,
                            )
                        }
                    }
                }
            }

            item { PFErrorBanner(error) }
        }
    }
}
