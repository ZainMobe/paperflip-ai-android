package com.wapp.paperflipai.core.quiz

import java.util.UUID
import kotlin.random.Random

/**
 * Builds quiz questions from a deck's own cards. Counterpart of
 * `QuizEngine.swift`: no network, no cost. Multiple-choice distractors are
 * other cards' answers; true/false shows either the card's own answer or
 * another card's. Near-duplicate answers are never used as distractors, and
 * a deck with fewer than four usable cards gets true/false only.
 */
data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val prompt: String,
    val correctAnswer: String,
    val kind: Kind,
) {
    sealed interface Kind {
        /** Four options; [correctIndex] is the answer. */
        data class MultipleChoice(val options: List<String>, val correctIndex: Int) : Kind
        /** "Is this the answer?" [shown] is displayed; [isTrue] says whether it matches. */
        data class TrueFalse(val shown: String, val isTrue: Boolean) : Kind
    }

    val isTrueFalse: Boolean get() = kind is Kind.TrueFalse
}

data class QuizAnswer(
    val question: QuizQuestion,
    val wasCorrect: Boolean,
    /** What the user picked, for the results screen. */
    val picked: String,
)

object QuizEngine {

    data class CardSeed(val id: String, val front: String, val back: String)

    /**
     * Builds up to [count] questions (null = every card). Cards are
     * shuffled so repeat quizzes feel different; [wrongFirst] cards go to
     * the front, which is how the "retry the ones you missed" round works.
     */
    fun makeQuestions(
        cards: List<CardSeed>,
        count: Int? = null,
        wrongFirst: Set<String> = emptySet(),
        random: Random = Random.Default,
    ): List<QuizQuestion> {
        val usable = cards.filter { it.front.isNotBlank() && it.back.isNotBlank() }
        if (usable.isEmpty()) return emptyList()

        var order = usable.shuffled(random)
        if (wrongFirst.isNotEmpty()) {
            val (missed, rest) = order.partition { it.id in wrongFirst }
            order = missed + rest
        }
        if (count != null) order = order.take(count)

        val canDoMultipleChoice = usable.size >= 4
        return order.mapIndexed { index, card ->
            // Mostly multiple choice, every third question true/false.
            val wantsTrueFalse = !canDoMultipleChoice || index % 3 == 2
            (if (!wantsTrueFalse) multipleChoice(card, usable, random) else null)
                ?: trueFalse(card, usable, random)
        }
    }

    private fun multipleChoice(card: CardSeed, pool: List<CardSeed>, random: Random): QuizQuestion? {
        val distractors = pool
            .filter { it.id != card.id && !isNearDuplicate(it.back, card.back) }
            .shuffled(random)
            .map { it.back }
            .distinct()
            .take(3)
        if (distractors.size < 3) return null
        val options = (distractors + card.back).shuffled(random)
        return QuizQuestion(
            cardId = card.id,
            prompt = card.front,
            correctAnswer = card.back,
            kind = QuizQuestion.Kind.MultipleChoice(options, options.indexOf(card.back)),
        )
    }

    private fun trueFalse(card: CardSeed, pool: List<CardSeed>, random: Random): QuizQuestion {
        val others = pool.filter { it.id != card.id && !isNearDuplicate(it.back, card.back) }
        val showTrue = others.isEmpty() || random.nextBoolean()
        val shown = if (showTrue) card.back else others.random(random).back
        return QuizQuestion(
            cardId = card.id,
            prompt = card.front,
            correctAnswer = card.back,
            kind = QuizQuestion.Kind.TrueFalse(shown, shown == card.back),
        )
    }

    /**
     * Two answers are near-duplicates if, normalised, they are equal, one
     * contains the other, or they share almost all of their words.
     */
    fun isNearDuplicate(a: String, b: String): Boolean {
        val na = normalise(a)
        val nb = normalise(b)
        if (na == nb) return true
        if (na.length > 12 && nb.length > 12 && (na.contains(nb) || nb.contains(na))) return true
        val wa = na.split(' ').filter { it.isNotEmpty() }.toSet()
        val wb = nb.split(' ').filter { it.isNotEmpty() }.toSet()
        if (wa.isEmpty() || wb.isEmpty()) return false
        val overlap = wa.intersect(wb).size.toDouble() / minOf(wa.size, wb.size)
        return overlap >= 0.85
    }

    private fun normalise(value: String): String =
        value.lowercase()
            .filter { it.isLetterOrDigit() || it.isWhitespace() }
            .split(Regex("""\s+"""))
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
