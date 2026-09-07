package com.wapp.paperflipai.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses a "#RRGGBB" (or "RRGGBB") string into a [Color].
 * Returns null for anything unparseable so callers can fall back to a token.
 */
fun parseHexColor(hex: String?): Color? {
    val cleaned = hex?.trim()?.removePrefix("#") ?: return null
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return if (cleaned.length == 6) Color(0xFF000000 or value) else Color(value)
}
