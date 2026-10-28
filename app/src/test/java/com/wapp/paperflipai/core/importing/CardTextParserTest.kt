package com.wapp.paperflipai.core.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTextParserTest {

    @Test
    fun `quizlet tab export`() {
        val result = CardTextParser.parse("cell\tbasic unit of life\natom\tsmallest unit of matter")
        assertEquals("tab", result.separatorName)
        assertEquals(2, result.cards.size)
        assertEquals(CardTextParser.ParsedCard("cell", "basic unit of life"), result.cards[0])
        assertEquals(0, result.skippedLines)
    }

    @Test
    fun `dash separated lines`() {
        val result = CardTextParser.parse("Paris - capital of France\nBerlin – capital of Germany")
        assertEquals("dash", result.separatorName)
        assertEquals("capital of Germany", result.cards[1].back)
    }

    @Test
    fun `lines without the separator are skipped and counted`() {
        val result = CardTextParser.parse("a\tb\nno separator here\nc\td")
        assertEquals(2, result.cards.size)
        assertEquals(1, result.skippedLines)
    }

    @Test
    fun `blank lines, comments and windows line endings are ignored`() {
        val result = CardTextParser.parse("# exported from Anki\r\n\r\nq1\ta1\r\nq2\ta2\r\n")
        assertEquals(2, result.cards.size)
        assertEquals(0, result.skippedLines)
    }

    @Test
    fun `only the first separator splits, the answer keeps the rest`() {
        val result = CardTextParser.parse("colours, red, green, blue")
        assertEquals("colours", result.cards.single().front)
        assertEquals("red, green, blue", result.cards.single().back)
    }

    @Test
    fun `quoted csv values are unquoted`() {
        val result = CardTextParser.parse("\"term\";\"a \"\"quoted\"\" answer\"")
        assertEquals("term", result.cards.single().front)
        assertEquals("a \"quoted\" answer", result.cards.single().back)
    }

    @Test
    fun `empty text gives nothing`() {
        val result = CardTextParser.parse("   \n  ")
        assertTrue(result.cards.isEmpty())
        assertEquals(0, result.skippedLines)
    }
}
