package com.wapp.paperflipai.feature.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSectionHeader
import com.wapp.paperflipai.designsystem.component.PFSwitch
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.formatMinuteOfDay

/**
 * Settings → Notifications — the Android counterpart of
 * `NotificationSettingsView.swift`. Handles the POST_NOTIFICATIONS
 * permission dance and the daily reminder time.
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current

    val isAuthorized by env.notifications.isAuthorized.collectAsStateWithLifecycle()
    val reminderEnabled by env.settings.reminderEnabled.collectAsStateWithLifecycle()
    val reminderMinute by env.settings.reminderMinuteOfDay.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        env.notifications.refreshAuthorization()
        if (granted) env.notifications.applyReminderState()
    }

    LaunchedEffect(Unit) { env.notifications.refreshAuthorization() }

    val statusTint = when {
        isAuthorized -> PFTheme.colors.success
        else -> PFTheme.colors.accent
    }

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.notifications), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xxl),
        ) {
            // ── Status ────────────────────────────────────────────────
            PFGroupedCard(modifier = Modifier.padding(top = PFTheme.spacing.md)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PFTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(statusTint.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isAuthorized) PFIcons.Notifications else PFIcons.NotificationsOff,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(PFTheme.spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (isAuthorized) R.string.notifications_on else R.string.not_set_up_yet
                            ),
                            style = PFTheme.type.bodyEmphasis,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = stringResource(
                                if (isAuthorized) R.string.pick_a_time_below_to_schedule_your_daily_remin
                                else R.string.choose_whether_paperflip_can_send_you_reminder
                            ),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            }

            if (isAuthorized) {
                // ── Daily reminder ────────────────────────────────────
                PFSectionHeader(title = stringResource(R.string.daily_reminder))
                PFGroupedCard {
                    PFListRow(
                        title = stringResource(R.string.remind_me_daily),
                        icon = PFIcons.Notifications,
                        showChevron = false,
                        trailing = {
                            PFSwitch(
                                checked = reminderEnabled,
                                onCheckedChange = {
                                    env.notifications.setReminder(it, reminderMinute)
                                },
                            )
                        },
                    )
                    if (reminderEnabled) {
                        PFRowDivider()
                        PFListRow(
                            title = stringResource(R.string.time),
                            icon = PFIcons.Schedule,
                            onClick = { showTimePicker = true },
                            trailing = {
                                Text(
                                    text = formatMinuteOfDay(reminderMinute),
                                    style = PFTheme.type.bodyEmphasis,
                                    color = PFTheme.colors.accent,
                                )
                            },
                        )
                    }
                }

                PFGroupedCard(modifier = Modifier.padding(top = PFTheme.spacing.lg)) {
                    PFListRow(
                        title = stringResource(R.string.send_a_preview_now),
                        icon = PFIcons.Send,
                        showChevron = false,
                        onClick = { env.notifications.sendPreview() },
                        trailing = {
                            Text(
                                text = stringResource(R.string.in_5s),
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.onSurfaceFaint,
                            )
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.sends_a_sample_notification_5_seconds_from_now),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                    modifier = Modifier.padding(top = PFTheme.spacing.sm, start = PFTheme.spacing.xs),
                )
            } else {
                PFGroupedCard(modifier = Modifier.padding(top = PFTheme.spacing.lg)) {
                    PFListRow(
                        title = stringResource(R.string.allow_notifications),
                        icon = PFIcons.Notifications,
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    )
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.open_system_settings),
                        icon = PFIcons.Settings,
                        showChevron = false,
                        onClick = {
                            runCatching { context.startActivity(env.notifications.systemSettingsIntent()) }
                        },
                        trailing = {
                            Icon(
                                imageVector = PFIcons.OpenExternal,
                                contentDescription = null,
                                tint = PFTheme.colors.onSurfaceFaint,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.we_ll_only_use_this_to_remind_you_to_study_not),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                    modifier = Modifier.padding(top = PFTheme.spacing.sm, start = PFTheme.spacing.xs),
                )
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePicker(
            initialMinuteOfDay = reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { minute ->
                showTimePicker = false
                env.notifications.setReminder(reminderEnabled, minute)
            },
        )
    }
}
