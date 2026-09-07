package com.wapp.paperflipai.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wapp.paperflipai.MainActivity
import com.wapp.paperflipai.R

/**
 * Posts the daily study reminder (and the settings-screen preview).
 * Tapping it deep-links straight into a study session, matching the iOS
 * `paperflip://study` behaviour.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val preview = intent.action == ACTION_PREVIEW
        val title = if (preview) "Preview reminder" else "Time to study"
        val body = if (preview) {
            "This is what your daily reminder will look like."
        } else {
            "A few minutes today keeps your streak alive."
        }
        notify(context, title, body, if (preview) 4201 else 4200)
    }

    private fun notify(context: Context, title: String, body: String, id: Int) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val launch = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("paperflip://study")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationsClient.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    companion object {
        const val ACTION_DAILY = "com.wapp.paperflipai.REMINDER_DAILY"
        const val ACTION_PREVIEW = "com.wapp.paperflipai.REMINDER_PREVIEW"
    }
}
