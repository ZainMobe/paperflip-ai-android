package com.wapp.paperflipai.feature.notifications

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.wapp.paperflipai.app.IntentInbox
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.app.PFAction
import com.wapp.paperflipai.core.data.NotificationDto
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFIconButton
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.asRelativeLabel
import kotlinx.coroutines.launch

/**
 * The notifications inbox — the Android counterpart of
 * `NotificationsInboxView.swift`. Tapping a row marks it read and follows
 * its link; the toolbar marks everything read at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsInboxScreen(
    onBack: () -> Unit,
    onOpenLink: (PFAction) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    val items = remember { mutableListOf<NotificationDto>().toMutableStateList() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        val userId = env.auth.session.value?.userId ?: return
        isLoading = true
        error = null
        try {
            val fetched = env.remote.fetchNotifications(userId)
            items.clear()
            items.addAll(fetched)
        } catch (failure: Exception) {
            error = failure.message
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val unread = items.count { !it.read }

    PFScreen(
        topBar = {
            PFTopBar(
                title = stringResource(R.string.notifications),
                onBack = onBack,
                actions = {
                    if (unread > 0) {
                        PFIconButton(
                            icon = PFIcons.Check,
                            contentDescription = stringResource(R.string.mark_all_read),
                            onClick = {
                                scope.launch {
                                    val userId = env.auth.session.value?.userId ?: return@launch
                                    runCatching { env.remote.markAllNotificationsRead(userId) }
                                    val updated = items.map { it.copy(read = true) }
                                    items.clear()
                                    items.addAll(updated)
                                }
                            },
                            tint = PFTheme.colors.accent,
                        )
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { scope.launch { load() } },
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .pfReadableWidth(),
                contentPadding = PaddingValues(PFTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                if (items.isEmpty() && !isLoading) {
                    item {
                        PFEmptyState(
                            icon = PFIcons.Notifications,
                            title = stringResource(R.string.you_re_all_caught_up),
                            message = stringResource(R.string.reminders_project_invites_and_other_updates_wi),
                            modifier = Modifier.padding(top = PFTheme.spacing.xl),
                        )
                    }
                } else {
                    items(items, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = {
                                scope.launch {
                                    if (!notification.read) {
                                        runCatching { env.remote.markNotificationRead(notification.id) }
                                        val index = items.indexOfFirst { it.id == notification.id }
                                        if (index >= 0) items[index] = notification.copy(read = true)
                                    }
                                    IntentInbox.actionFor(notification.link)?.let(onOpenLink)
                                }
                            },
                        )
                    }
                }

                item { PFErrorBanner(error) }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationDto, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PFRadius.lg)
    val tint = when {
        notification.type.contains("invite", true) -> PFTheme.colors.accent
        notification.type.contains("streak", true) -> PFTheme.colors.hard
        else -> PFTheme.colors.onSurfaceMuted
    }
    val icon = when {
        notification.type.contains("invite", true) -> PFIcons.PersonAdd
        notification.type.contains("streak", true) -> PFIcons.Streak
        else -> PFIcons.Notifications
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (notification.read) PFTheme.colors.elevated else PFTheme.colors.accentSoft,
                shape,
            )
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.99f)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = PFTheme.type.bodyEmphasis,
                color = PFTheme.colors.onSurface,
            )
            Text(
                text = notification.body,
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurfaceMuted,
            )
            Spacer(Modifier.size(PFTheme.spacing.xs))
            Text(
                text = notification.createdAt.asRelativeLabel(),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceFaint,
            )
        }
        if (!notification.read) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .background(PFTheme.colors.accent, CircleShape)
            )
        }
    }
}
