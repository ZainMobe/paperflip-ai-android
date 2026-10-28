package com.wapp.paperflipai.core.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun `finds a url inside other text`() {
        assertEquals(
            "https://en.wikipedia.org/wiki/Photosynthesis",
            UrlExtractor.firstUrl("Source Link Article 1 https://en.wikipedia.org/wiki/Photosynthesis read this"),
        )
    }

    @Test
    fun `strips trailing punctuation and brackets`() {
        assertEquals("https://example.com/a", UrlExtractor.firstUrl("see (https://example.com/a)."))
        assertEquals("http://example.com", UrlExtractor.firstUrl("http://example.com!"))
    }

    @Test
    fun `returns the first of several`() {
        assertEquals("https://a.com/1", UrlExtractor.firstUrl("https://a.com/1 and https://b.com/2"))
    }

    @Test
    fun `keeps query strings`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc123&t=42",
            UrlExtractor.firstUrl("https://www.youtube.com/watch?v=abc123&t=42"),
        )
    }

    @Test
    fun `no url gives null`() {
        assertNull(UrlExtractor.firstUrl("just some words"))
        assertNull(UrlExtractor.firstUrl(""))
    }
}
