package com.wapp.paperflipai.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFProjectInviteStatus
import com.wapp.paperflipai.core.data.PFProjectJoinRequestStatus
import com.wapp.paperflipai.core.data.PFProjectRole
import com.wapp.paperflipai.core.data.ProfileDto
import com.wapp.paperflipai.core.data.ProjectInviteDto
import com.wapp.paperflipai.core.data.ProjectJoinRequestDto
import com.wapp.paperflipai.core.data.ProjectMemberDto
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFIconButton
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.library.DeckCard
import com.wapp.paperflipai.feature.library.DeckCardLayout
import com.wapp.paperflipai.util.asShortDate
import kotlinx.coroutines.launch

/**
 * Hero, members, invites, join requests and decks — the Android
 * counterpart of `ProjectDetailView.swift`. Owners get edit / invite /
 * delete; everyone else gets leave.
 *
 * Membership is fetched live from the backend rather than the local
 * cache, which only holds the project list.
 */
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    val snapshot by env.database.snapshot.collectAsStateWithLifecycle()
    val session by env.auth.session.collectAsStateWithLifecycle()
    val project = snapshot.projects.firstOrNull { it.id == projectId }
    val decks = remember(snapshot.decks, projectId) {
        snapshot.decks.filter { it.projectId == projectId }
    }
    val cardCounts = remember(snapshot.cards) { snapshot.cards.groupingBy { it.deckId }.eachCount() }

    val members = remember(projectId) { mutableListOf<ProjectMemberDto>().toMutableStateList() }
    var memberProfiles by remember(projectId) { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    val sentInvites = remember(projectId) { mutableListOf<ProjectInviteDto>().toMutableStateList() }
    val joinRequests = remember(projectId) { mutableListOf<ProjectJoinRequestDto>().toMutableStateList() }
    var joinProfiles by remember(projectId) { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }

    var isLoadingMembers by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    val currentUserId = session?.userId
    val isOwner = project != null && currentUserId == project.ownerUserId
    val myRole = members.firstOrNull { it.userId == currentUserId }?.role
    val canAddDecks = isOwner || myRole == PFProjectRole.Editor.raw

    suspend fun loadMembers() {
        isLoadingMembers = true
        loadError = null
        try {
            val result = env.remote.fetchProjectMembers(projectId)
            members.clear()
            members.addAll(
                result.members.sortedWith(
                    compareBy({ PFProjectRole.from(it.role).sortKey }, { it.joinedAt })
                )
            )
            memberProfiles = result.profiles
            env.database.replaceMembers(
                projectId,
                result.members.map { it.toModel(result.profiles[it.userId]) },
            )
        } catch (failure: Exception) {
            loadError = failure.message
        } finally {
            isLoadingMembers = false
        }
    }

    suspend fun loadOwnerExtras() {
        runCatching {
            val invites = env.remote.fetchProjectInvites(projectId)
            sentInvites.clear()
            sentInvites.addAll(invites)
        }
        runCatching {
            val result = env.remote.fetchProjectJoinRequests(projectId)
            joinRequests.clear()
            joinRequests.addAll(result.requests)
            joinProfiles = result.profiles
        }
    }

    LaunchedEffect(projectId, isOwner) {
        loadMembers()
        if (isOwner) loadOwnerExtras()
    }

    if (project == null) {
        PFScreen(topBar = { PFTopBar(title = "", onBack = onBack) }) { padding ->
            PFEmptyState(
                icon = PFIcons.Group,
                title = stringResource(R.string.that_didn_t_work),
                message = stringResource(R.string.no_projects_yet),
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val pendingInvites = sentInvites.filter {
        it.status == PFProjectInviteStatus.Pending.raw && it.expiresAt > System.currentTimeMillis()
    }
    val pendingRequests = joinRequests.filter {
        it.status == PFProjectJoinRequestStatus.Pending.raw
    }

    val deleteTitle = stringResource(R.string.delete_3, project.name)
    val deleteMessage = stringResource(R.string.all_members_lose_access_decks_in_this_project)
    val deleteConfirm = stringResource(R.string.delete_project)
    val leaveTitle = stringResource(R.string.leave, project.name)
    val leaveMessage = stringResource(R.string.you_ll_lose_access_to_this_project_s_decks_and)
    val leaveConfirm = stringResource(R.string.leave_project)
    val cancelLabel = stringResource(R.string.cancel)

    // Destructive-action confirmations. iOS puts a confirmationDialog in front
    // of each of these; without one a single tap on a small button silently
    // removes someone's access, and there is no undo.
    val removeMemberTitle = stringResource(R.string.remove_member)
    val removeLabel = stringResource(R.string.remove)
    val revokeInviteTitle = stringResource(R.string.revoke_invite)
    val revokeLabel = stringResource(R.string.revoke)
    val declineRequestTitle = stringResource(R.string.decline_request)
    val declineLabel = stringResource(R.string.decline)
    val removeAccessTemplate = stringResource(R.string.will_lose_access_to_and_its_decks)
    val revokeTemplate = stringResource(R.string.won_t_be_able_to_accept_this_invitation_anymor)
    val declineTemplate = stringResource(R.string.they_won_t_be_added_to_they_can_request_again)
    val someoneLabel = stringResource(R.string.member_fallback)
    val actionFailedLabel = stringResource(R.string.couldnt_complete_that)
    val deletedToast = stringResource(R.string.project_deleted)
    val leftToast = stringResource(R.string.left_project)

    PFScreen(
        topBar = {
            PFTopBar(
                title = project.name,
                onBack = onBack,
                actions = {
                    PFIconButton(
                        icon = PFIcons.More,
                        contentDescription = stringResource(R.string.nav_more),
                        onClick = { showMenu = true },
                        tint = PFTheme.colors.accent,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .pfReadableWidth(),
            contentPadding = PaddingValues(
                start = PFTheme.spacing.lg,
                end = PFTheme.spacing.lg,
                bottom = PFTheme.spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            // ── Hero ──────────────────────────────────────────────────
            item {
                PFCard(
                    elevation = PFElevation.Card,
                    surface = PFTheme.colors.paper,
                    modifier = Modifier.padding(top = PFTheme.spacing.md),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                        PFTagPill(
                            title = stringResource(R.string.project),
                            icon = PFIcons.Group,
                            prominent = true,
                        )
                        if (isOwner) {
                            PFTagPill(
                                title = stringResource(R.string.owner),
                                icon = PFIcons.Pro,
                                tint = PFTheme.colors.warning,
                            )
                        }
                    }
                    Spacer(Modifier.size(PFTheme.spacing.md))
                    Text(
                        text = project.name,
                        style = PFTheme.type.title1,
                        color = PFTheme.colors.onSurface,
                    )
                    project.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.size(PFTheme.spacing.xs))
                        Text(
                            text = it,
                            style = PFTheme.type.callout,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                    Spacer(Modifier.size(PFTheme.spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                        Text(
                            text = stringResource(R.string.decks, decks.size),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                        Text(
                            text = stringResource(R.string.members, members.size),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            }

            // ── Members ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.members_2),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    if (isOwner) {
                        Text(
                            text = stringResource(R.string.invite),
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.accent,
                            modifier = Modifier
                                .pfPressable(onClick = { showInvite = true }, pressedScale = 0.94f)
                                .padding(PFTheme.spacing.xs),
                        )
                    }
                }
            }

            if (isLoadingMembers && members.isEmpty()) {
                item { LoadingRow(stringResource(R.string.loading_members)) }
            } else if (loadError != null) {
                item {
                    Text(
                        text = loadError.orEmpty(),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.danger,
                    )
                }
            } else {
                items(members, key = { it.userId }) { member ->
                    MemberRow(
                        member = member,
                        profile = memberProfiles[member.userId],
                        isCurrentUser = member.userId == currentUserId,
                        isProjectOwner = member.userId == project.ownerUserId,
                        canManage = isOwner && member.userId != project.ownerUserId,
                        onChangeRole = { newRole ->
                            scope.launch {
                                runCatching {
                                    env.remote.updateProjectMemberRole(projectId, member.userId, newRole)
                                    loadMembers()
                                }.onFailure { PFToast.error(actionFailedLabel, it.message) }
                            }
                        },
                        onRemove = {
                            confirmation = PFConfirmation(
                                title = removeMemberTitle,
                                message = removeAccessTemplate.format(member.displayName ?: member.email ?: someoneLabel, project.name),
                                icon = PFIcons.Delete,
                                confirmTitle = removeLabel,
                                cancelTitle = cancelLabel,
                                destructive = true,
                            ) {
                                runCatching {
                                    env.remote.removeProjectMember(projectId, member.userId)
                                    loadMembers()
                                }.onFailure { PFToast.error(actionFailedLabel, it.message) }
                            }
                        },
                    )
                }
            }

            // ── Pending invites (owner) ───────────────────────────────
            if (isOwner && pendingInvites.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.pending_invites),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                        modifier = Modifier.padding(top = PFTheme.spacing.md),
                    )
                }
                items(pendingInvites, key = { it.id }) { invite ->
                    PFCard(elevation = PFElevation.Card, surface = PFTheme.colors.paper) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = invite.invitedEmail,
                                    style = PFTheme.type.bodyEmphasis,
                                    color = PFTheme.colors.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(PFProjectRole.from(invite.role).displayNameRes),
                                    style = PFTheme.type.footnote,
                                    color = PFTheme.colors.onSurfaceMuted,
                                )
                            }
                            PFTagPill(
                                title = stringResource(R.string.pending),
                                icon = PFIcons.Schedule,
                                tint = PFTheme.colors.onSurfaceMuted,
                            )
                        }
                        Spacer(Modifier.size(PFTheme.spacing.sm))
                        Text(
                            text = stringResource(R.string.expires, invite.expiresAt.asShortDate()),
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceFaint,
                        )
                        Spacer(Modifier.size(PFTheme.spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                            PFButton(
                                title = stringResource(R.string.resend),
                                onClick = {
                                    scope.launch {
                                        processingId = invite.id
                                        runCatching { env.remote.resendProjectInvite(invite.id) }
                                        .onFailure { PFToast.error(actionFailedLabel, it.message) }
                                        loadOwnerExtras()
                                        processingId = null
                                    }
                                },
                                variant = PFButtonVariant.Outline,
                                size = PFButtonSize.Sm,
                                fullWidth = false,
                                enabled = processingId != invite.id,
                            )
                            PFButton(
                                title = stringResource(R.string.revoke),
                                onClick = {
                                    confirmation = PFConfirmation(
                                        title = revokeInviteTitle,
                                        message = revokeTemplate.format(invite.invitedEmail),
                                        icon = PFIcons.Delete,
                                        confirmTitle = revokeLabel,
                                        cancelTitle = cancelLabel,
                                        destructive = true,
                                    ) {
                                        processingId = invite.id
                                        val result =
                                            runCatching { env.remote.revokeProjectInvite(invite.id) }
                                        processingId = null
                                        if (result.isSuccess) {
                                            sentInvites.removeAll { it.id == invite.id }
                                        } else {
                                            PFToast.error(
                                                actionFailedLabel,
                                                result.exceptionOrNull()?.message,
                                            )
                                        }
                                    }
                                },
                                variant = PFButtonVariant.Outline,
                                size = PFButtonSize.Sm,
                                isDestructive = true,
                                fullWidth = false,
                                enabled = processingId != invite.id,
                            )
                        }
                    }
                }
            }

            // ── Join requests (owner) ─────────────────────────────────
            if (isOwner && pendingRequests.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.pending_requests),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                        modifier = Modifier.padding(top = PFTheme.spacing.md),
                    )
                }
                items(pendingRequests, key = { it.id }) { request ->
                    val profile = joinProfiles[request.requesterUserId]
                    val requesterName = profile?.fullName?.takeIf { it.isNotBlank() }
                        ?: profile?.email
                        ?: stringResource(R.string.someone)
                    PFCard(elevation = PFElevation.Card, surface = PFTheme.colors.paper) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PFTheme.colors.accentSoft, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PFIcons.PersonAdd,
                                    contentDescription = null,
                                    tint = PFTheme.colors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(PFTheme.spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = requesterName,
                                    style = PFTheme.type.bodyEmphasis,
                                    color = PFTheme.colors.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(R.string.wants_to_join, project.name),
                                    style = PFTheme.type.footnote,
                                    color = PFTheme.colors.onSurfaceMuted,
                                    maxLines = 1,
                                )
                            }
                            PFTagPill(
                                title = stringResource(
                                    PFProjectRole.from(request.requestedRole).displayNameRes
                                ),
                                tint = PFTheme.colors.accent,
                            )
                        }
                        request.message?.takeIf { it.isNotBlank() }?.let { note ->
                            Spacer(Modifier.size(PFTheme.spacing.sm))
                            Text(
                                text = "“$note”",
                                style = PFTheme.type.footnote,
                                color = PFTheme.colors.onSurfaceMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.md))
                                    .padding(PFTheme.spacing.sm),
                            )
                        }
                        Spacer(Modifier.size(PFTheme.spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                            PFButton(
                                title = stringResource(R.string.decline),
                                onClick = {
                                    confirmation = PFConfirmation(
                                        title = declineRequestTitle,
                                        message = declineTemplate.format(project.name),
                                        icon = PFIcons.Close,
                                        confirmTitle = declineLabel,
                                        cancelTitle = cancelLabel,
                                        destructive = true,
                                    ) {
                                        processingId = request.id
                                        val result = runCatching {
                                            env.remote.declineProjectJoinRequest(request.id)
                                        }
                                        processingId = null
                                        if (result.isSuccess) {
                                            joinRequests.removeAll { it.id == request.id }
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
                                enabled = processingId != request.id,
                            )
                            PFButton(
                                title = stringResource(R.string.approve),
                                onClick = {
                                    scope.launch {
                                        processingId = request.id
                                        val result = runCatching {
                                            env.remote.approveProjectJoinRequest(request.id)
                                            loadMembers()
                                        }
                                        processingId = null
                                        // The row must not disappear unless the person
                                        // was actually approved.
                                        if (result.isSuccess) {
                                            joinRequests.removeAll { it.id == request.id }
                                        } else {
                                            PFToast.error(
                                                actionFailedLabel,
                                                result.exceptionOrNull()?.message,
                                            )
                                        }
                                    }
                                },
                                trailingIcon = PFIcons.Check,
                                isLoading = processingId == request.id,
                                fullWidth = false,
                                enabled = processingId != request.id,
                            )
                        }
                    }
                }
            }

            // ── Decks ─────────────────────────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.decks_2),
                    style = PFTheme.type.overline,
                    color = PFTheme.colors.onSurfaceMuted,
                    modifier = Modifier.padding(top = PFTheme.spacing.md),
                )
            }
            if (decks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            if (canAddDecks) R.string.no_decks_here_yet_tap_new_deck_to_create_one_i
                            else R.string.no_decks_in_this_project_yet
                        ),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
                            .padding(PFTheme.spacing.md),
                    )
                }
            } else {
                items(decks, key = { it.id }) { deck ->
                    DeckCard(
                        deck = deck,
                        cardCount = cardCounts[deck.id] ?: 0,
                        folder = snapshot.folders.firstOrNull { it.id == deck.folderId },
                        layout = DeckCardLayout.List,
                        onClick = { onOpenDeck(deck.id) },
                        onLongClick = { onOpenDeck(deck.id) },
                    )
                }
            }
        }
    }

    // ── Overflow menu ─────────────────────────────────────────────────
    if (showMenu) {
        PFSheet(onDismiss = { showMenu = false }, title = project.name) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                if (isOwner) {
                    PFListRow(
                        title = stringResource(R.string.invite_teammate),
                        icon = PFIcons.PersonAdd,
                        showChevron = false,
                        onClick = { showMenu = false; showInvite = true },
                    )
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.edit_project),
                        icon = PFIcons.Edit,
                        showChevron = false,
                        onClick = { showMenu = false; showEdit = true },
                    )
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.delete_project),
                        icon = PFIcons.Delete,
                        iconTint = PFTheme.colors.danger,
                        titleColor = PFTheme.colors.danger,
                        showChevron = false,
                        onClick = {
                            showMenu = false
                            confirmation = PFConfirmation(
                                title = deleteTitle,
                                message = deleteMessage,
                                icon = PFIcons.Delete,
                                confirmTitle = deleteConfirm,
                                cancelTitle = cancelLabel,
                                destructive = true,
                            ) {
                                runCatching { env.remote.deleteProject(projectId) }
                                env.database.deleteProject(projectId)
                                PFToast.warning(deletedToast, project.name)
                                onBack()
                            }
                        },
                    )
                } else {
                    PFListRow(
                        title = stringResource(R.string.leave_project),
                        icon = PFIcons.SignOut,
                        iconTint = PFTheme.colors.danger,
                        titleColor = PFTheme.colors.danger,
                        showChevron = false,
                        onClick = {
                            showMenu = false
                            confirmation = PFConfirmation(
                                title = leaveTitle,
                                message = leaveMessage,
                                icon = PFIcons.SignOut,
                                confirmTitle = leaveConfirm,
                                cancelTitle = cancelLabel,
                                destructive = true,
                            ) {
                                currentUserId?.let {
                                    runCatching { env.remote.removeProjectMember(projectId, it) }
                                }
                                env.database.deleteProject(projectId)
                                PFToast.info(leftToast, project.name)
                                onBack()
                            }
                        },
                    )
                }
            }
        }
    }

    if (showEdit) {
        EditProjectSheet(project = project, onDismiss = { showEdit = false })
    }

    if (showInvite) {
        InviteMemberSheet(
            project = project,
            onDismiss = { showInvite = false },
            onSent = {
                showInvite = false
                scope.launch { loadOwnerExtras() }
            },
        )
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

@Composable
private fun LoadingRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = PFTheme.colors.accent,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = text, style = PFTheme.type.footnote, color = PFTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun MemberRow(
    member: ProjectMemberDto,
    profile: ProfileDto?,
    isCurrentUser: Boolean,
    isProjectOwner: Boolean,
    canManage: Boolean,
    onChangeRole: (PFProjectRole) -> Unit,
    onRemove: () -> Unit,
) {
    var showActions by remember { mutableStateOf(false) }
    val role = PFProjectRole.from(member.role)
    val name = profile?.fullName?.takeIf { it.isNotBlank() }
        ?: profile?.email
        ?: stringResource(R.string.member_fallback)
    val initials = name.trim().take(2).uppercase()
    val shape = RoundedCornerShape(PFRadius.lg)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .then(
                if (canManage) Modifier.pfPressable(
                    onClick = { showActions = true },
                    pressedScale = 0.99f,
                ) else Modifier
            )
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initials, style = PFTheme.type.footnoteBold, color = PFTheme.colors.accent)
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurface,
                    maxLines = 1,
                )
                if (isCurrentUser) {
                    Spacer(Modifier.width(PFTheme.spacing.xs))
                    Text(
                        text = stringResource(R.string.you),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }
            profile?.email?.let {
                Text(
                    text = it,
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                    maxLines = 1,
                )
            }
        }
        RolePill(role = if (isProjectOwner) PFProjectRole.Owner else role)
    }

    if (showActions) {
        PFSheet(onDismiss = { showActions = false }, title = name) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                listOf(PFProjectRole.Editor, PFProjectRole.Viewer).forEach { candidate ->
                    PFListRow(
                        title = stringResource(candidate.displayNameRes),
                        icon = if (role == candidate) PFIcons.Check else candidate.icon,
                        showChevron = false,
                        onClick = {
                            showActions = false
                            onChangeRole(candidate)
                        },
                    )
                    PFRowDivider()
                }
                PFListRow(
                    title = stringResource(R.string.remove_from_project),
                    icon = PFIcons.Delete,
                    iconTint = PFTheme.colors.danger,
                    titleColor = PFTheme.colors.danger,
                    showChevron = false,
                    onClick = {
                        showActions = false
                        onRemove()
                    },
                )
            }
        }
    }
}
