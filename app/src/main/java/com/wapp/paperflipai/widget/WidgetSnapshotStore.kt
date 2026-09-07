package com.wapp.paperflipai.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * The "what should the widget show right now" payload — the Android
 * counterpart of `WidgetSnapshotWriter.swift`.
 *
 * iOS needs an App Group to share this across process boundaries; on
 * Android the widget runs in the same process, so plain SharedPreferences
 * is enough. Written after every sync, after every study session, and
 * whenever the Study tab appears.
 */
object WidgetSnapshotStore {

    private const val PREFS = "paperflip_widget"
    private const val KEY_DUE = "due_today"
    private const val KEY_STREAK = "streak_days"
    private const val KEY_LAST_STUDIED = "last_studied_at"

    data class Snapshot(
        val dueToday: Int = 0,
        val streakDays: Int = 0,
        val lastStudiedAt: Long = 0L,
    )

    fun read(context: Context): Snapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Snapshot(
            dueToday = prefs.getInt(KEY_DUE, 0),
            streakDays = prefs.getInt(KEY_STREAK, 0),
            lastStudiedAt = prefs.getLong(KEY_LAST_STUDIED, 0L),
        )
    }

    /** Writes the payload and asks Glance to re-render every placed widget. */
    suspend fun update(context: Context, dueToday: Int, streakDays: Int, lastStudiedAt: Long?) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_DUE, dueToday)
            .putInt(KEY_STREAK, streakDays)
            .putLong(KEY_LAST_STUDIED, lastStudiedAt ?: 0L)
            .apply()
        runCatching { PaperflipWidget().updateAll(app) }
    }
}
