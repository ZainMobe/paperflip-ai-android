package com.wapp.paperflipai.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFolder
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.parseHexColor

/**
 * Source icon / tint / label helpers shared by every deck surface — the
 * Android counterpart of the `extension PFDeck` block in `DeckCardView.swift`.
 */
val PFSourceType.icon: ImageVector
    get() = when (this) {
        PFSourceType.Pdf -> PFIcons.Pdf
        PFSourceType.Youtube -> PFIcons.Video
        PFSourceType.Article -> PFIcons.Article
        PFSourceType.Text -> PFIcons.Notes
        PFSourceType.Photo -> PFIcons.DocumentScan
        PFSourceType.Audio -> PFIcons.Waveform
    }

val PFSourceType.label: String
    get() = when (this) {
        PFSourceType.Pdf -> "PDF"
        PFSourceType.Youtube -> "YOUTUBE"
        PFSourceType.Article -> "ARTICLE"
        PFSourceType.Text -> "NOTES"
        PFSourceType.Photo -> "PHOTO"
        PFSourceType.Audio -> "AUDIO"
    }

@Composable
fun PFSourceType.tint(): Color = when (this) {
    PFSourceType.Pdf -> PFTheme.colors.accent
    PFSourceType.Youtube -> PFTheme.colors.danger
    PFSourceType.Article -> PFTheme.colors.warning
    PFSourceType.Text -> PFTheme.colors.success
    PFSourceType.Photo -> PFTheme.colors.info
    PFSourceType.Audio -> PFTheme.colors.hard
}

val PFDeck.icon: ImageVector get() = sourceType.icon
val PFDeck.sourceLabel: String get() = sourceType.label

@Composable
fun PFDeck.tint(): Color = sourceType.tint()

@Composable
fun PFFolder?.tint(): Color = parseHexColor(this?.colorHex) ?: PFTheme.colors.accent
