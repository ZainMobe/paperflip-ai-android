package com.wapp.paperflipai.core.srs

import com.wapp.paperflipai.core.data.PFFlashcard

/**
 * Builds the ordered list of cards to study in a session — the Android
 * counterpart of `StudyQueue.swift`. Pulls from one deck (deck-level study)
 * or across all decks (today's queue).
 *
 * Selection rules:
 *  1. Cards whose due date ≤ now are "due".
 *  2. Brand-new cards (repetitions == 0) are always due.
 *  3. Within due cards, sort by due date ascending so most-overdue come
 *     first. Brand-new cards splice in at a 1:3 ratio so the user isn't
 *     slammed with new material before reviews.
 *  4. Cap session size to keep things bite-sized.
 */
object StudyQueue {

    /** Default cap on cards per session. Tunable per call. */
    const val DEFAULT_LIMIT = 30

    fun dueCards(
        cards: List<PFFlashcard>,
        now: Long = System.currentTimeMillis(),
        limit: Int = DEFAULT_LIMIT,
    ): List<PFFlashcard> {
        val due = cards.filter { isDue(it, now) }
        val (newCards, reviewCards) = due.partition { it.srsRepetitions == 0 }

        // Most overdue first.
        val sortedReviews = reviewCards.sortedBy { it.srsDueDate }

        val queue = ArrayList<PFFlashcard>(due.size)
        val newIterator = newCards.shuffled().iterator()
        sortedReviews.forEachIndexed { index, card ->
            queue.add(card)
            if ((index + 1) % 3 == 0 && newIterator.hasNext()) queue.add(newIterator.next())
        }
        while (newIterator.hasNext()) queue.add(newIterator.next())

        return queue.take(limit)
    }

    /** How many cards are due across a set of cards right now. */
    fun dueCount(cards: List<PFFlashcard>, now: Long = System.currentTimeMillis()): Int =
        cards.count { isDue(it, now) }

    fun isDue(card: PFFlashcard, now: Long = System.currentTimeMillis()): Boolean =
        card.srsRepetitions == 0 || card.srsDueDate <= now

    /** "Mastered" = recalled at least three times in a row with a long interval. */
    fun isMastered(card: PFFlashcard): Boolean =
        card.srsRepetitions >= 3 && card.srsInterval >= 21

    fun isLearning(card: PFFlashcard): Boolean =
        card.srsRepetitions in 1..2 || (card.srsRepetitions >= 3 && card.srsInterval < 21)
}
