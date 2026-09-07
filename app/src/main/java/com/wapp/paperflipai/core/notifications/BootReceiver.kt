package com.wapp.paperflipai.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wapp.paperflipai.PaperflipApplication

/**
 * Alarms don't survive a reboot or an app update, so re-apply the user's
 * daily-reminder preference when either happens.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val app = context.applicationContext as? PaperflipApplication ?: return
                app.environment.notifications.applyReminderState()
            }
        }
    }
}
