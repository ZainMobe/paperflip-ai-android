package com.wapp.paperflipai.feature.importer

import com.wapp.paperflipai.core.data.DeckDto

/**
 * Hands a ready-made deck (an Anki import) from the Import hub to the
 * review route. Navigation arguments can't carry a whole deck, and this is
 * a one-shot hand-off, so a single in-memory slot is enough: the hub puts,
 * the review screen takes once.
 */
object ImportDraft {
    @Volatile
    private var pending: DeckDto? = null

    fun put(deck: DeckDto) {
        pending = deck
    }

    fun take(): DeckDto? = pending.also { pending = null }
}
