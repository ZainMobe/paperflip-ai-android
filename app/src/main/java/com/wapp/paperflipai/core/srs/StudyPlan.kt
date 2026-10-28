package com.wapp.paperflipai.core.srs

import com.wapp.paperflipai.core.data.PFFlashcard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

/**
 * Spreads a deck's unseen cards evenly over the days left before an exam,
 * so the daily queue is "due reviews + a steady slice of new cards" rather
 * than every new card at once. The exam day itself is kept for review.
 * SM-2 keeps scheduling the reviews as usual. Counterpart of
 * `StudyPlan.swift`; pure Kotlin so it is unit-testable.
 */
data class StudyPlan(
    val examDate: Long,
    /** Whole days from today to the exam, at least 1. */
    val daysLeft: Int,
    val totalCards: Int,
    val unseenCards: Int,
    /** New cards to introduce per study day to be ready in time. */
    val newPerDay: Int,
    /** New cards already introduced today (first seen today). */
    val introducedToday: Int,
    val dueReviewsToday: Int,
) {
    val newCardsStillToIntroduceToday: Int get() = max(0, newPerDay - introducedToday)
    val dailyTarget: Int get() = dueReviewsToday + newCardsStillToIntroduceToday
    val isComplete: Boolean get() = unseenCards == 0
    val isBehind: Boolean get() = daysLeft <= 1 && unseenCards > 0

    /** Share of the material seen at least once, 0..1. */
    val coverage: Float
        get() = if (totalCards == 0) 0f else (totalCards - unseenCards).toFloat() / totalCards

    companion object {
        /** Null when there is no exam date or the exam has passed. */
        fun make(
            examDate: Long?,
            cards: List<PFFlashcard>,
            now: Long = System.currentTimeMillis(),
            zone: ZoneId = ZoneId.systemDefault(),
        ): StudyPlan? {
            examDate ?: return null
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val examDay = Instant.ofEpochMilli(examDate).atZone(zone).toLocalDate()
            if (examDay.isBefore(today)) return null

            val days = max(1, ChronoUnit.DAYS.between(today, examDay).toInt())
            // Introduce new cards on the days before the exam; with the
            // exam tomorrow, everything left is due today.
            val introductionDays = max(1, days - 1)

            val unseen = cards.count { it.srsRepetitions == 0 }
            val introducedToday = cards.count { card ->
                card.srsRepetitions >= 1 && card.srsInterval <= 1 &&
                    card.srsLastReviewedAt?.let { sameDay(it, today, zone) } == true
            }
            val dueReviews = cards.count { it.srsRepetitions > 0 && it.srsDueDate <= now }

            // Spread what is left, plus what was done today, so the pace
            // does not shrink as the user studies.
            val perDay = ceil((unseen + introducedToday).toDouble() / introductionDays).toInt()

            return StudyPlan(
                examDate = examDate,
                daysLeft = days,
                totalCards = cards.size,
                unseenCards = unseen,
                newPerDay = perDay,
                introducedToday = introducedToday,
                dueReviewsToday = dueReviews,
            )
        }

        private fun sameDay(millis: Long, day: LocalDate, zone: ZoneId): Boolean =
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() == day

        /** Start of [day] in [zone], as epoch millis. Exam dates are stored this way. */
        fun startOfDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
            day.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
