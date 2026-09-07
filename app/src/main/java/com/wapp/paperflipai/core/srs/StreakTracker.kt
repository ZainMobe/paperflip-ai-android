package com.wapp.paperflipai.core.srs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks consecutive days the user has studied — the Android counterpart of
 * `StreakTracker.swift`. Backed by SharedPreferences so the home-screen
 * widget and the reminder receiver can read it without spinning up the app's
 * object graph.
 *
 * Rules:
 *  - First study of the day → increment if yesterday counted, else reset to 1.
 *  - Subsequent studies the same day → no change.
 *  - Last study older than yesterday → the streak is over; [count] reads 0
 *    and the next session starts a fresh streak at 1.
 */
class StreakTracker(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _lastStudyDate = MutableStateFlow(
        prefs.getLong(KEY_DATE, 0L).takeIf { it > 0L }
    )
    val lastStudyDate: StateFlow<Long?> = _lastStudyDate.asStateFlow()

    /**
     * The streak as it stands *right now*.
     *
     * The stored number is only rewritten when a session is recorded, so a
     * user who studied 12 days and then stopped would otherwise keep seeing
     * "12" — on the Stats screen and, more conspicuously, on the home-screen
     * widget — indefinitely. A streak whose last session is older than
     * yesterday is over, so it reads 0 until they study again. Nothing is
     * mutated here: [recordStudy] is still the only writer.
     */
    private val _count = MutableStateFlow(
        computeCount(prefs.getInt(KEY_COUNT, 0), _lastStudyDate.value, System.currentTimeMillis())
    )
    val count: StateFlow<Int> = _count.asStateFlow()

    /** True when the user has already studied today. */
    fun hasStudiedToday(now: Long = System.currentTimeMillis()): Boolean =
        _lastStudyDate.value?.let { isSameDay(it, now) } == true

    /**
     * Re-evaluates the streak against the current clock. Cheap, and worth
     * calling whenever the app comes back to the foreground — a session left
     * open across midnight would otherwise still be showing yesterday's
     * number.
     */
    fun refresh(now: Long = System.currentTimeMillis()) {
        _count.value = computeCount(prefs.getInt(KEY_COUNT, 0), _lastStudyDate.value, now)
    }

    /**
     * Records that the user just completed a study session. Returns true if
     * this counted as a streak increment (i.e. the first session of the day)
     * so the UI can play a celebratory animation.
     */
    fun recordStudy(now: Long = System.currentTimeMillis()): Boolean {
        val last = _lastStudyDate.value
        if (last != null && isSameDay(last, now)) return false

        val stored = prefs.getInt(KEY_COUNT, 0)
        val next = if (last != null && isYesterday(last, now)) stored + 1 else 1

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

    // ── Day arithmetic ────────────────────────────────────────────────
    //
    // Delegated to [StreakMath] so the calendar rules can be unit-tested
    // against a fixed clock and time zone. They are not obvious: see the
    // spring-forward case documented there.

    private fun computeCount(stored: Int, last: Long?, now: Long): Int =
        StreakMath.effectiveCount(stored, last, now)

    private fun isSameDay(a: Long, b: Long): Boolean = StreakMath.isSameDay(a, b)

    private fun isYesterday(last: Long, now: Long): Boolean = StreakMath.isYesterday(last, now)

    companion object {
        private const val PREFS = "paperflip_streak"
        private const val KEY_COUNT = "paperflip.streak.count.v1"
        private const val KEY_DATE = "paperflip.streak.lastStudyDate.v1"
    }
}
