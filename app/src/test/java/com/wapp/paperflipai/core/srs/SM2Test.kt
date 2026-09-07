package com.wapp.paperflipai.core.srs

import com.wapp.paperflipai.core.card
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * SM-2 is ported from `SM2.swift`; these tests are as much a parity check as
 * a correctness one. The numbers below come from the original Wozniak
 * formulation, so a future refactor that "simplifies" the maths will fail
 * here rather than quietly reshuffling everyone's review schedule.
 */
class SM2Test {

    private val newYork = TimeZone.getTimeZone("America/New_York")
    private lateinit var original: TimeZone

    @Before fun setUp() {
        original = TimeZone.getDefault()
        TimeZone.setDefault(newYork)
    }

    @After fun tearDown() {
        TimeZone.setDefault(original)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(newYork).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun fieldsOf(millis: Long): Triple<Int, Int, Int> =
        Calendar.getInstance(newYork).apply { timeInMillis = millis }
            .let { Triple(it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH), it.get(Calendar.HOUR_OF_DAY)) }

    // ── Quality scale ─────────────────────────────────────────────────

    @Test fun `quality scale matches the iOS mapping`() {
        assertEquals(0, SRSResponse.Again.quality)
        assertEquals(3, SRSResponse.Hard.quality)
        assertEquals(4, SRSResponse.Good.quality)
        assertEquals(5, SRSResponse.Easy.quality)
    }

    @Test fun `only Again counts as a failed recall`() {
        assertFalse(SRSResponse.Again.didRecall)
        assertTrue(SRSResponse.Hard.didRecall)
        assertTrue(SRSResponse.Good.didRecall)
        assertTrue(SRSResponse.Easy.didRecall)
    }

    // ── Easiness factor ───────────────────────────────────────────────

    @Test fun `Good leaves the easiness factor unchanged`() {
        // EF' = EF + (0.1 - 1*(0.08 + 1*0.02)) = EF + 0.0
        val next = SM2.update(SRSState(), SRSResponse.Good, now = at(2026, 6, 10, 9))
        assertEquals(2.5, next.ease, 1e-9)
    }

    @Test fun `Easy raises and Hard lowers the easiness factor`() {
        val now = at(2026, 6, 10, 9)
        assertEquals(2.6, SM2.update(SRSState(), SRSResponse.Easy, now).ease, 1e-9)
        assertEquals(2.36, SM2.update(SRSState(), SRSResponse.Hard, now).ease, 1e-9)
    }

    @Test fun `easiness factor never falls below the 1_3 floor`() {
        var state = SRSState(ease = 1.35, interval = 10, repetitions = 5)
        repeat(5) { state = SM2.update(state, SRSResponse.Again, now = at(2026, 6, 10, 9)) }
        assertEquals(SM2.MIN_EASE, state.ease, 1e-9)
    }

    // ── Interval progression ──────────────────────────────────────────

    @Test fun `first two successful reviews use the fixed 1 and 6 day steps`() {
        val now = at(2026, 6, 10, 9)
        val first = SM2.update(SRSState(), SRSResponse.Good, now)
        assertEquals(1, first.interval)
        assertEquals(1, first.repetitions)

        val second = SM2.update(first, SRSResponse.Good, now)
        assertEquals(6, second.interval)
        assertEquals(2, second.repetitions)
    }

    @Test fun `third review onward multiplies by the NEW easiness factor`() {
        // Deliberately pinned: using the previous ease here is the classic
        // way to get SM-2 subtly wrong, and iOS uses the new one.
        val state = SRSState(ease = 2.5, interval = 6, repetitions = 2)
        val next = SM2.update(state, SRSResponse.Easy, now = at(2026, 6, 10, 9))
        assertEquals(2.6, next.ease, 1e-9)
        assertEquals(16, next.interval) // 6 * 2.6 = 15.6 → 16, not 6 * 2.5 = 15
    }

    @Test fun `Again restarts the schedule without zeroing the interval`() {
        val state = SRSState(ease = 2.5, interval = 40, repetitions = 7)
        val next = SM2.update(state, SRSResponse.Again, now = at(2026, 6, 10, 9))
        assertEquals(0, next.repetitions)
        assertEquals(1, next.interval)
    }

    // ── Due dates ─────────────────────────────────────────────────────

    @Test fun `due date lands the interval number of calendar days later`() {
        val next = SM2.update(
            SRSState(ease = 2.5, interval = 6, repetitions = 2),
            SRSResponse.Good,
            now = at(2026, 6, 10, 21),
        )
        assertEquals(15, next.interval)
        assertEquals(Triple(6, 25, 21), fieldsOf(next.dueDate))
    }

    /**
     * REGRESSION. Scheduling with a fixed 86_400_000 ms would move a card
     * reviewed at 20:00 the evening before a spring-forward to 21:00 the next
     * day, and the drift accumulates over long intervals. Calendar days keep
     * it pinned to the wall clock, matching iOS's
     * `Calendar.date(byAdding: .day)`.
     */
    @Test fun `due date keeps its wall-clock hour across a DST transition`() {
        val saturdayEvening = at(2026, 3, 7, 20)
        val next = SM2.update(SRSState(), SRSResponse.Good, now = saturdayEvening)

        assertEquals(1, next.interval)
        assertEquals(Triple(3, 8, 20), fieldsOf(next.dueDate))
        // The naive version would have produced 21:00.
        assertEquals(Triple(3, 8, 21), fieldsOf(saturdayEvening + 86_400_000L))
    }

    @Test fun `apply writes every SRS field back onto the card`() {
        val now = at(2026, 6, 10, 9)
        val original = card(front = "q", repetitions = 2, interval = 6)
        val updated = SM2.apply(SRSResponse.Good, original, now)

        assertEquals(3, updated.srsRepetitions)
        assertEquals(15, updated.srsInterval)
        assertEquals(now, updated.srsLastReviewedAt)
        assertEquals(now, updated.updatedAt)
        assertTrue(updated.srsDueDate > now)
    }
}
