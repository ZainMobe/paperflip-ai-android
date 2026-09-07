package com.wapp.paperflipai.core

/**
 * Canonical external URLs — the Android counterpart of `Links.swift`.
 * Centralised so Settings, the paywall and email footers can't drift.
 */
object PFLinks {
    const val WEBSITE = "https://www.paperflip.ai"
    const val TERMS = "https://www.paperflip.ai/terms"
    const val PRIVACY = "https://www.paperflip.ai/privacy"
    const val SUPPORT = "https://www.paperflip.ai/support"
    const val SUPPORT_EMAIL = "support@paperflip.ai"

    fun sharedDeck(deckId: String) = "$WEBSITE/share/$deckId"
    fun projectJoin(token: String) = "$WEBSITE/projects/join/$token"
}
