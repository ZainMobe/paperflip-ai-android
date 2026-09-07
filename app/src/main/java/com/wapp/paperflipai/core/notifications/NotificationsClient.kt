package com.wapp.paperflipai.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Local notifications — the Android counterpart of
 * `NotificationsClient.swift`. Owns the channels, the POST_NOTIFICATIONS
 * permission state, and the daily study reminder alarm.
 *
 * The reminder uses an inexact repeating alarm on purpose: it keeps the app
 * off the exact-alarm permission list (which Play scrutinises) and a study
 * nudge does not need to-the-second precision.
 */
class NotificationsClient(
    private val context: Context,
    private val settings: SettingsStore,
) {
    private val app = context.applicationContext

    private val _isAuthorized = MutableStateFlow(hasPermission())
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    init {
        createChannels()
    }

    // ── Permission ────────────────────────────────────────────────────

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        app, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    /** Re-reads the system state. Call whenever a settings screen appears. */
    fun refreshAuthorization() {
        _isAuthorized.value = hasPermission() && NotificationManagerCompat.from(app).areNotificationsEnabled()
        // Self-heal the alarm chain: a force-stop clears pending alarms, and
        // permission granted after the fact would otherwise leave the
        // reminder switched on in Settings but never firing.
        applyReminderState()
    }

    /** Intent that opens this app's notification settings page. */
    fun systemSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, app.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    // ── Channels ──────────────────────────────────────────────────────

    private fun createChannels() {
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                app.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = app.getString(R.string.channel_reminders_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UPDATES,
                app.getString(R.string.channel_updates_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = app.getString(R.string.channel_updates_desc) }
        )
    }

    // ── Daily reminder ────────────────────────────────────────────────

    /** Applies the stored reminder preference to the alarm manager. */
    fun applyReminderState() {
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val pending = reminderPendingIntent()

        alarms.cancel(pending)
        if (!settings.reminderEnabled.value || !hasPermission()) return

        val minuteOfDay = settings.reminderMinuteOfDay.value
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        // Deliberately a one-shot that ReminderReceiver re-arms after every
        // fire, not setRepeating(INTERVAL_DAY): a fixed 24-hour period drifts
        // by an hour at each DST change and never recovers, so a 20:00
        // reminder slowly becomes a 19:00 one. Recomputing the calendar time
        // each day keeps it pinned to the wall clock the user chose.
        //
        // setAndAllowWhileIdle still fires in Doze and needs no exact-alarm
        // permission; a study nudge does not need to-the-second precision.
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            pending,
        )
    }

    fun setReminder(enabled: Boolean, minuteOfDay: Int) {
        settings.setReminder(enabled, minuteOfDay)
        applyReminderState()
    }

    /** Fires a sample notification five seconds from now. */
    fun sendPreview() {
        if (!hasPermission()) return
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(app, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_PREVIEW
        }
        val pending = PendingIntent.getBroadcast(
            app, REQUEST_PREVIEW, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5_000L, pending)
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(app, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY
        }
        return PendingIntent.getBroadcast(
            app, REQUEST_DAILY, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "paperflip.reminders"
        const val CHANNEL_UPDATES = "paperflip.updates"
        private const val REQUEST_DAILY = 1001
        private const val REQUEST_PREVIEW = 1002
    }
}
