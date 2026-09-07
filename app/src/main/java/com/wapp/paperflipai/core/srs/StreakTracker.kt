package com.wapp.paperflipai.core.srs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Tracks consecutive days the user has studied — the Android counterpart of
 * `StreakTracker.swift`. Backed by SharedPreferences so the home-screen
 * widget and the reminder receiver can read it without spinning up the app's
 * object graph.
 *
 * Rules:
 *  - First study of the day → increment if yesterday counted, else reset to 1.
 *  - Subsequent studies the same day → no change.
 *  - Last study older than yesterday → streak resets to 1.
 */
class StreakTracker(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _count = MutableStateFlow(prefs.getInt(KEY_COUNT, 0))
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _lastStudyDate = MutableStateFlow(
        prefs.getLong(KEY_DATE, 0L).takeIf { it > 0L }
    )
    val lastStudyDate: StateFlow<Long?> = _lastStudyDate.asStateFlow()

    /** True when the user has already studied today. */
    fun hasStudiedToday(now: Long = System.currentTimeMillis()): Boolean =
        _lastStudyDate.value?.let { isSameDay(it, now) } == true

    /**
     * Records that the user just completed a study session. Returns true if
     * this counted as a streak increment (i.e. the first session of the day)
     * so the UI can play a celebratory animation.
     */
    fun recordStudy(now: Long = System.currentTimeMillis()): Boolean {
        val last = _lastStudyDate.value
        if (last != null && isSameDay(last, now)) return false

        val next = if (last != null && isSameDay(last, now - DAY_MILLIS)) _count.value + 1 else 1
        _count.value = next
        _lastStudyDate.value = now
        prefs.edit().putInt(KEY_COUNT, next).putLong(KEY_DATE, now).apply()
        return true
    }

    fun reset() {
        _count.value = 0
        _lastStudyDate.value = null
        prefs.edit().remove(KEY_COUNT).remove(KEY_DATE).apply()
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val PREFS = "paperflip_streak"
        private const val KEY_COUNT = "paperflip.streak.count.v1"
        private const val KEY_DATE = "paperflip.streak.lastStudyDate.v1"
        private const val DAY_MILLIS = 86_400_000L
    }
}
