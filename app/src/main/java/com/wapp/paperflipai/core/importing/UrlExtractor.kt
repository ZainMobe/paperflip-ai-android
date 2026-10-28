package com.wapp.paperflipai.core.importing

/**
 * Finds the first http(s) URL inside arbitrary pasted text, so
 * "Source Link Article 1 https://en.wikipedia.org/..." still works.
 * Counterpart of `URLExtractor.swift`.
 */
object UrlExtractor {
    private val pattern = Regex("""https?://[^\s<>"'()]+""", RegexOption.IGNORE_CASE)
    private const val TRAILING = ".,;:!?)]"

    fun firstUrl(text: String): String? {
        val match = pattern.find(text.trim()) ?: return null
        return match.value.trimEnd { it in TRAILING }.ifEmpty { null }
    }
}
