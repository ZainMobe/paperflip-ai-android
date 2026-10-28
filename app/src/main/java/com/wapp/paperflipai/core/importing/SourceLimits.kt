package com.wapp.paperflipai.core.importing

/**
 * Source size limits in one place. Counterpart of `PDFLimits.swift`.
 *
 * The server accepts PDFs up to 30 MB. The app shows the plan's limit so a
 * free user picking a big file sees an upgrade card rather than an error.
 */
object SourceLimits {
    const val FREE_PDF_MB = 5
    const val PRO_PDF_MB = 25

    const val FREE_PDF_BYTES: Long = FREE_PDF_MB * 1024L * 1024L
    const val PRO_PDF_BYTES: Long = PRO_PDF_MB * 1024L * 1024L

    /** Absolute cap, whatever the plan. */
    const val HARD_PDF_BYTES: Long = PRO_PDF_BYTES

    /** Pasted, scanned or transcribed text sent as a source. */
    const val MAX_SOURCE_CHARACTERS = 60_000

    /** Below this the generator has nothing to work with. */
    const val MIN_SOURCE_CHARACTERS = 40

    fun pdfLimitBytes(isPro: Boolean): Long = if (isPro) PRO_PDF_BYTES else FREE_PDF_BYTES
}
