package com.wapp.paperflipai.core

import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.data.PFSourceType

/**
 * Minimal builders for the model types. Everything the tests don't care
 * about gets a fixed, boring value so a failure message points at the field
 * under test rather than at incidental noise.
 */

fun card(
    id: String = "card-1",
    deckId: String = "deck-1",
    front: String = "front",
    back: String = "back",
    orderIndex: Int = 0,
    ease: Double = 2.5,
    interval: Int = 0,
    repetitions: Int = 0,
    dueDate: Long = 0L,
    lastReviewedAt: Long? = null,
    updatedAt: Long = 0L,
): PFFlashcard = PFFlashcard(
    id = id,
    deckId = deckId,
    front = front,
    back = back,
    orderIndex = orderIndex,
    createdAt = 0L,
    updatedAt = updatedAt,
    srsEase = ease,
    srsInterval = interval,
    srsRepetitions = repetitions,
    srsDueDate = dueDate,
    srsLastReviewedAt = lastReviewedAt,
)

fun deck(
    id: String = "deck-1",
    userId: String = "user-1",
    title: String = "Deck",
    updatedAt: Long = 0L,
    lastStudiedAt: Long? = null,
    folderId: String? = null,
): PFDeck = PFDeck(
    id = id,
    userId = userId,
    title = title,
    sourceType = PFSourceType.Article,
    folderId = folderId,
    createdAt = 0L,
    updatedAt = updatedAt,
    lastStudiedAt = lastStudiedAt,
)
