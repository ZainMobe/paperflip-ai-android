package com.wapp.paperflipai.core.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizEngineTest {

    private fun seeds(n: Int) = (1..n).map {
        QuizEngine.CardSeed(id = "c$it", front = "Question $it", back = "Distinct answer number $it about topic ${'A' + it}")
    }

    @Test
    fun `small decks get true or false only`() {
        val questions = QuizEngine.makeQuestions(seeds(3), random = Random(1))
        assertEquals(3, questions.size)
        assertTrue(questions.all { it.isTrueFalse })
    }

    @Test
    fun `multiple choice has four distinct options and points at the right one`() {
        repeat(20) { seed ->
            val questions = QuizEngine.makeQuestions(seeds(8), random = Random(seed))
            questions.forEach { question ->
                when (val kind = question.kind) {
                    is QuizQuestion.Kind.MultipleChoice -> {
                        assertEquals(4, kind.options.size)
                        assertEquals(4, kind.options.toSet().size)
                        assertEquals(question.correctAnswer, kind.options[kind.correctIndex])
                    }
                    is QuizQuestion.Kind.TrueFalse ->
                        assertEquals(kind.shown == question.correctAnswer, kind.isTrue)
                }
            }
        }
    }

    @Test
    fun `every third question is true or false in larger decks`() {
        val questions = QuizEngine.makeQuestions(seeds(9), random = Random(3))
        questions.forEachIndexed { index, question ->
            assertEquals("index $index", index % 3 == 2, question.isTrueFalse)
        }
    }

    @Test
    fun `count limits the number of questions`() {
        assertEquals(5, QuizEngine.makeQuestions(seeds(10), count = 5, random = Random(0)).size)
    }

    @Test
    fun `missed cards come first`() {
        val missed = setOf("c7", "c2")
        val questions = QuizEngine.makeQuestions(seeds(10), wrongFirst = missed, random = Random(4))
        assertEquals(missed, questions.take(2).map { it.cardId }.toSet())
    }

    @Test
    fun `blank cards are ignored`() {
        val cards = seeds(4) + QuizEngine.CardSeed("blank", "  ", "answer")
        val questions = QuizEngine.makeQuestions(cards, random = Random(0))
        assertEquals(4, questions.size)
        assertFalse(questions.any { it.cardId == "blank" })
    }

    @Test
    fun `empty input gives no questions`() {
        assertTrue(QuizEngine.makeQuestions(emptyList()).isEmpty())
    }

    @Test
    fun `near duplicates`() {
        assertTrue(QuizEngine.isNearDuplicate("Paris", "paris."))
        assertTrue(QuizEngine.isNearDuplicate("The mitochondria", "the mitochondria of the cell"))
        assertFalse(QuizEngine.isNearDuplicate("Paris", "London"))
        assertFalse(QuizEngine.isNearDuplicate("Oxygen and carbon", "Hydrogen and helium"))
    }
}
