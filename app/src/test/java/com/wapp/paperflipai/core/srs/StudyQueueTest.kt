package com.wapp.paperflipai.core.srs

import com.wapp.paperflipai.core.card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The queue decides what the user actually sees each day, so the rules that
 * matter are: nothing scheduled for the future leaks in, brand-new cards are
 * always available, and new material is rationed rather than dumped.
 */
class StudyQueueTest {

    private val now = 1_800_000_000_000L // arbitrary fixed clock

    private fun review(id: String, due: Long, reps: Int = 3) =
        card(id = id, repetitions = reps, interval = 10, dueDate = due)

    private fun fresh(id: String) = card(id = id, repetitions = 0, dueDate = 0L)

    // ── Due-ness ──────────────────────────────────────────────────────

    @Test fun `a brand-new card is always due`() {
        assertTrue(StudyQueue.isDue(fresh("n1"), now))
    }

    @Test fun `a card due in the past is due, one due in the future is not`() {
        assertTrue(StudyQueue.isDue(review("r1", now - 1), now))
        assertFalse(StudyQueue.isDue(review("r1", now + 1), now))
    }

    @Test fun `a card due exactly now is due`() {
        assertTrue(StudyQueue.isDue(review("r1", now), now))
    }

    @Test fun `dueCount counts only due cards`() {
        val cards = listOf(
            fresh("n1"),
            review("r1", now - 1000),
            review("r2", now + 1000),
            review("r3", now + 999_999),
        )
        assertEquals(2, StudyQueue.dueCount(cards, now))
    }

    // ── Ordering ──────────────────────────────────────────────────────

    @Test fun `reviews come back most overdue first`() {
        val cards = listOf(
            review("recent", now - 1_000),
            review("ancient", now - 900_000),
            review("middling", now - 60_000),
        )
        val order = StudyQueue.dueCards(cards, now).map { it.id }
        assertEquals(listOf("ancient", "middling", "recent"), order)
    }

    @Test fun `new cards are spliced in after every third review`() {
        val reviews = (1..6).map { review("r$it", now - (10_000L * (7 - it))) }
        val news = (1..2).map { fresh("n$it") }

        val queue = StudyQueue.dueCards(reviews + news, now)

        // Positions 3 and 7 (0-indexed) are the new-card slots.
        assertEquals(8, queue.size)
        assertTrue(queue[3].srsRepetitions == 0)
        assertTrue(queue[7].srsRepetitions == 0)
        assertEquals(6, queue.count { it.srsRepetitions > 0 })
    }

    @Test fun `leftover new cards are appended rather than dropped`() {
        val queue = StudyQueue.dueCards(listOf(review("r1", now - 1)) + (1..4).map { fresh("n$it") }, now)
        assertEquals(5, queue.size)
        assertEquals(4, queue.count { it.srsRepetitions == 0 })
    }

    @Test fun `an all-new deck still produces a full queue`() {
        val queue = StudyQueue.dueCards((1..10).map { fresh("n$it") }, now)
        assertEquals(10, queue.size)
    }

    // ── Session cap ───────────────────────────────────────────────────

    @Test fun `the session is capped at the requested limit`() {
        val cards = (1..100).map { review("r$it", now - it * 1_000L) }
        assertEquals(StudyQueue.DEFAULT_LIMIT, StudyQueue.dueCards(cards, now).size)
        assertEquals(5, StudyQueue.dueCards(cards, now, limit = 5).size)
    }

    @Test fun `nothing due yields an empty queue rather than an error`() {
        val cards = (1..5).map { review("r$it", now + 100_000) }
        assertTrue(StudyQueue.dueCards(cards, now).isEmpty())
        assertTrue(StudyQueue.dueCards(emptyList(), now).isEmpty())
    }

    // ── Mastery buckets (drive the Stats screen) ──────────────────────

    @Test fun `mastery requires both repetitions and a long interval`() {
        assertTrue(StudyQueue.isMastered(card(repetitions = 3, interval = 21)))
        assertFalse(StudyQueue.isMastered(card(repetitions = 3, interval = 20)))
        assertFalse(StudyQueue.isMastered(card(repetitions = 2, interval = 60)))
    }

    @Test fun `learning and mastered buckets do not overlap`() {
        val samples = listOf(
            card(repetitions = 0, interval = 0),
            card(repetitions = 1, interval = 1),
            card(repetitions = 2, interval = 6),
            card(repetitions = 3, interval = 15),
            card(repetitions = 3, interval = 21),
            card(repetitions = 9, interval = 200),
        )
        samples.forEach {
            assertFalse(
                "a card must not be both learning and mastered",
                StudyQueue.isLearning(it) && StudyQueue.isMastered(it),
            )
        }
    }
}
