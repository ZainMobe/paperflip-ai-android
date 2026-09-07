package com.wapp.paperflipai.core.srs

import com.wapp.paperflipai.core.data.PFFlashcard
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SuperMemo 2 spaced-repetition algorithm — the Android counterpart of
 * `SM2.swift`. Given a card's current state and a quality response (0–5),
 * computes the new state.
 *
 * Reference: P. A. Wozniak, "Optimization of repetition spacing in the
 * practice of learning" (1990) — the classic algorithm used by Anki and
 * most flashcard apps.
 */
enum class SRSResponse(val raw: String) {
    Again("again"), Hard("hard"), Good("good"), Easy("easy");

    /**
     * Quality score (0–5). Drives both the easiness factor and the
     * "did the user remember" branch.
     */
    val quality: Int
        get() = when (this) {
            Again -> 0
            Hard -> 3
            Good -> 4
            Easy -> 5
        }

    /** Did the user successfully recall the card? */
    val didRecall: Boolean get() = quality >= 3
}

/**
 * Snapshot of a card's SRS state. A pure value type so the engine stays
 * testable without touching persistence.
 */
data class SRSState(
    /** SuperMemo "easiness factor". Bound: ≥ 1.3. */
    val ease: Double = 2.5,
    /** Days until next review. */
    val interval: Int = 0,
    /** Number of consecutive correct recalls. Resets on [SRSResponse.Again]. */
    val repetitions: Int = 0,
    /** Epoch millis the card is next due. */
    val dueDate: Long = 0L,
)

object SM2 {
    /** Lower bound on the easiness factor (per the original paper). */
    const val MIN_EASE: Double = 1.3

    private const val DAY_MILLIS = 86_400_000L

    /**
     * Compute the new SRS state given the previous state, the user's
     * response, and the current timestamp (injectable for tests).
     */
    fun update(state: SRSState, response: SRSResponse, now: Long = System.currentTimeMillis()): SRSState {
        // ── Easiness factor ───────────────────────────────────────────
        // EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
        val q = response.quality.toDouble()
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        val ease = max(MIN_EASE, state.ease + delta)

        // ── Repetitions & interval ────────────────────────────────────
        val interval: Int
        val repetitions: Int
        if (response.didRecall) {
            interval = when (state.repetitions) {
                0 -> 1      // first correct review → 1 day
                1 -> 6      // second → 6 days
                else -> max(1, (state.interval * ease).roundToInt())
            }
            repetitions = state.repetitions + 1
        } else {
            // Forgot — restart the spacing schedule.
            repetitions = 0
            interval = 1
        }

        return SRSState(
            ease = ease,
            interval = interval,
            repetitions = repetitions,
            dueDate = now + interval * DAY_MILLIS,
        )
    }

    /** Reads the SRS fields off a card. */
    fun stateOf(card: PFFlashcard) = SRSState(
        ease = card.srsEase,
        interval = card.srsInterval,
        repetitions = card.srsRepetitions,
        dueDate = card.srsDueDate,
    )

    /** Applies a response to a card, returning the updated copy. */
    fun apply(response: SRSResponse, card: PFFlashcard, now: Long = System.currentTimeMillis()): PFFlashcard {
        val next = update(stateOf(card), response, now)
        return card.copy(
            srsEase = next.ease,
            srsInterval = next.interval,
            srsRepetitions = next.repetitions,
            srsDueDate = next.dueDate,
            srsLastReviewedAt = now,
            updatedAt = now,
        )
    }
}
