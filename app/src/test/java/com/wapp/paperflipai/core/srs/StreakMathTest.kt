package com.wapp.paperflipai.core.srs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Streaks are the app's retention mechanic, and the ways they break are all
 * calendar edge cases rather than logic errors — so these tests pin a real
 * time zone with real DST transitions rather than working in UTC.
 */
class StreakMathTest {

    private val newYork = TimeZone.getTimeZone("America/New_York")
    private lateinit var original: TimeZone

    @Before fun setUp() {
        original = TimeZone.getDefault()
        TimeZone.setDefault(newYork)
    }

    @After fun tearDown() {
        TimeZone.setDefault(original)
    }

    /** Local wall-clock time in the pinned zone. */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(newYork).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    // ── Same day ──────────────────────────────────────────────────────

    @Test fun `same calendar day is same day regardless of hour`() {
        assertTrue(StreakMath.isSameDay(at(2026, 6, 10, 0, 1), at(2026, 6, 10, 23, 59)))
    }

    @Test fun `one minute either side of midnight is not the same day`() {
        assertFalse(StreakMath.isSameDay(at(2026, 6, 10, 23, 59), at(2026, 6, 11, 0, 1)))
    }

    @Test fun `same day-of-month in different years is not the same day`() {
        assertFalse(StreakMath.isSameDay(at(2025, 6, 10, 12), at(2026, 6, 10, 12)))
    }

    // ── Yesterday ─────────────────────────────────────────────────────

    @Test fun `previous calendar day counts as yesterday`() {
        assertTrue(StreakMath.isYesterday(at(2026, 6, 9, 20), at(2026, 6, 10, 8)))
    }

    @Test fun `two days ago is not yesterday`() {
        assertFalse(StreakMath.isYesterday(at(2026, 6, 8, 20), at(2026, 6, 10, 8)))
    }

    @Test fun `today is not yesterday`() {
        assertFalse(StreakMath.isYesterday(at(2026, 6, 10, 1), at(2026, 6, 10, 23)))
    }

    /**
     * REGRESSION. US DST springs forward at 02:00 on 8 March 2026, so that
     * Sunday is 23 hours long. The original implementation asked
     * `isSameDay(last, now - 86_400_000)`, which from early Monday lands on
     * *Saturday* — so a user who studied on Sunday and opened the app at
     * 00:30 on Monday had their streak silently reset to 1.
     */
    @Test fun `session on a spring-forward Sunday still counts as yesterday early Monday`() {
        val studiedSunday = at(2026, 3, 8, 10)
        val mondayJustAfterMidnight = at(2026, 3, 9, 0, 30)

        assertTrue(StreakMath.isYesterday(studiedSunday, mondayJustAfterMidnight))

        // And the arithmetic that used to be here would have failed:
        val naiveYesterday = mondayJustAfterMidnight - 86_400_000L
        assertFalse(
            "fixed-millisecond day arithmetic must not be reintroduced",
            StreakMath.isSameDay(studiedSunday, naiveYesterday),
        )
    }

    /** The 25-hour day: 1 November 2026, falling back. */
    @Test fun `fall-back day still resolves yesterday correctly`() {
        assertTrue(StreakMath.isYesterday(at(2026, 11, 1, 10), at(2026, 11, 2, 0, 30)))
    }

    // ── Effective count ───────────────────────────────────────────────

    @Test fun `streak survives while the last session is today or yesterday`() {
        val now = at(2026, 6, 10, 9)
        assertEquals(12, StreakMath.effectiveCount(12, at(2026, 6, 10, 8), now))
        assertEquals(12, StreakMath.effectiveCount(12, at(2026, 6, 9, 22), now))
    }

    @Test fun `streak reads zero once a day has been missed`() {
        assertEquals(0, StreakMath.effectiveCount(12, at(2026, 6, 8, 22), at(2026, 6, 10, 9)))
    }

    @Test fun `never having studied reads zero`() {
        assertEquals(0, StreakMath.effectiveCount(12, null, at(2026, 6, 10, 9)))
    }
}
