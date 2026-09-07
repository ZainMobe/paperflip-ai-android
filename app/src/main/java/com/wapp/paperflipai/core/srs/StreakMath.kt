package com.wapp.paperflipai.core.srs

import java.util.Calendar

/**
 * Calendar rules behind [StreakTracker], kept pure so they can be unit-tested
 * against a fixed clock and time zone.
 *
 * Everything here uses calendar days, never a fixed 86_400_000 ms, and that
 * is the whole point of the file — see [isYesterday].
 */
internal object StreakMath {

    fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Calendar-day comparison, matching the iOS
     * `Calendar.date(byAdding: .day, value: -1, to: now)`.
     *
     * Subtracting a fixed 86_400_000 ms is NOT equivalent. The day after a
     * spring-forward transition is only 23 hours long, so between 00:00 and
     * 01:00 that arithmetic lands on the day *before* yesterday — and the
     * user's streak is silently wiped. `StreakMathTest` pins this.
     */
    fun isYesterday(last: Long, now: Long): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        return isSameDay(last, yesterday)
    }

    /**
     * The streak as it stands at [now]. The stored number is only rewritten
     * when a session is recorded, so a lapsed streak would otherwise keep
     * being advertised — most visibly on the home-screen widget.
     */
    fun effectiveCount(stored: Int, last: Long?, now: Long): Int = when {
        last == null -> 0
        isSameDay(last, now) || isYesterday(last, now) -> stored
        else -> 0
    }
}
