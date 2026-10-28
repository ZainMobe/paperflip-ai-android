package com.wapp.paperflipai.core.srs

import com.wapp.paperflipai.core.card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StudyPlanTest {

    private val zone = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 3, 10)
    private val now = StudyPlan.startOfDay(today, zone) + 12 * 3_600_000L
    private fun exam(daysFromToday: Long) = StudyPlan.startOfDay(today.plusDays(daysFromToday), zone)
    private fun unseen(n: Int) = (1..n).map { card(id = "u$it", orderIndex = it) }

    @Test
    fun `no exam date or a past exam gives no plan`() {
        assertNull(StudyPlan.make(null, unseen(3), now, zone))
        assertNull(StudyPlan.make(exam(-1), unseen(3), now, zone))
    }

    @Test
    fun `new cards are spread over the days before the exam`() {
        val plan = StudyPlan.make(exam(6), unseen(10), now, zone)!!
        assertEquals(6, plan.daysLeft)
        assertEquals(10, plan.unseenCards)
        assertEquals(2, plan.newPerDay) // 10 cards over 5 introduction days
        assertEquals(2, plan.dailyTarget)
        assertFalse(plan.isComplete)
    }

    @Test
    fun `exam tomorrow means everything left today`() {
        val plan = StudyPlan.make(exam(1), unseen(7), now, zone)!!
        assertEquals(1, plan.daysLeft)
        assertEquals(7, plan.newPerDay)
        assertTrue(plan.isBehind)
    }

    @Test
    fun `exam today still counts as one day`() {
        assertEquals(1, StudyPlan.make(exam(0), unseen(2), now, zone)!!.daysLeft)
    }

    @Test
    fun `cards learned today keep the pace steady`() {
        val learnedToday = (1..2).map {
            card(id = "t$it", repetitions = 1, interval = 1, dueDate = now + 86_400_000L, lastReviewedAt = now - 60_000L)
        }
        val plan = StudyPlan.make(exam(6), unseen(8) + learnedToday, now, zone)!!
        assertEquals(2, plan.newPerDay)
        assertEquals(2, plan.introducedToday)
        assertEquals(0, plan.newCardsStillToIntroduceToday)
    }

    @Test
    fun `due reviews are counted and a fully seen deck is complete`() {
        val reviewed = (1..3).map { card(id = "r$it", repetitions = 2, interval = 6, dueDate = now - 1) }
        val plan = StudyPlan.make(exam(5), reviewed, now, zone)!!
        assertTrue(plan.isComplete)
        assertEquals(3, plan.dueReviewsToday)
        assertEquals(1f, plan.coverage, 0.0001f)
    }

    @Test
    fun `start of day is midnight in the zone`() {
        assertEquals(1_773_100_800_000L, StudyPlan.startOfDay(today, zone))
    }
}
