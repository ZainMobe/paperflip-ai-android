package com.wapp.paperflipai.feature.importer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.CardDto
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.core.data.newId
import com.wapp.paperflipai.core.importing.CardTextParser
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * "Paste ready-made cards": Quizlet or Anki text exports become a deck with
 * no AI call and no monthly credit. Parsed live with a preview, then the
 * normal review step. Counterpart of `PasteCardsImportView.swift`.
 */
@Composable
fun PasteCardsScreen(
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    var review by remember { mutableStateOf<DeckDto?>(null) }
    var text by remember { mutableStateOf("") }
    var deckTitle by remember { mutableStateOf("") }

    AnimatedContent(
        targetState = review,
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
        label = "pasteStep",
    ) { current ->
        if (current == null) {
            PasteCardsInput(
                text = text,
                onTextChange = { text = it },
                deckTitle = deckTitle,
                onDeckTitleChange = { deckTitle = it },
                onBack = onBack,
                onContinue = { review = it },
            )
        } else {
            ReviewGeneratedDeckContent(
                dto = current,
                onSaved = onOpenDeck,
                onDiscarded = { review = null },
                onOpenPaywall = onOpenPaywall,
            )
        }
    }
}

@Composable
private fun PasteCardsInput(
    text: String,
    onTextChange: (String) -> Unit,
    deckTitle: String,
    onDeckTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: (DeckDto) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val clipboard = LocalClipboardManager.current
    val parsed = remember(text) { CardTextParser.parse(text) }
    val defaultTitle = stringResource(R.string.paste_default_deck_title)
    val summary = stringResource(R.string.paste_deck_summary, parsed.cards.size)

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.paste_title_short), onBack = onBack) },
        bottomBar = {
            if (parsed.cards.isNotEmpty()) {
                PFButton(
                    title = stringResource(R.string.paste_review_n_cards, parsed.cards.size),
                    onClick = {
                        val userId = env.auth.session.value?.userId.orEmpty()
                        onContinue(
                            DeckDto(
                                id = newId(),
                                userId = userId,
                                title = deckTitle.trim().ifEmpty { defaultTitle },
                                summary = summary,
                                sourceType = PFSourceType.Text.raw,
                                cards = parsed.cards.map { CardDto(id = newId(), front = it.front, back = it.back) },
                            )
                        )
                    },
                    size = PFButtonSize.Lg,
                    trailingIcon = PFIcons.Forward,
                    modifier = Modifier
                        .background(PFTheme.colors.surface)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.md),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Row(modifier = Modifier.padding(top = PFTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PFTheme.colors.onSurface.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PFIcons.Paste, contentDescription = null, tint = PFTheme.colors.onSurface, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.paste_title), style = PFTheme.type.title2, color = PFTheme.colors.onSurface)
                    Text(stringResource(R.string.paste_subtitle), style = PFTheme.type.callout, color = PFTheme.colors.onSurfaceMuted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                ImportTextEditor(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = stringResource(R.string.paste_placeholder),
                    accessibilityLabel = stringResource(R.string.paste_accessibility),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                    MiniChip(
                        icon = PFIcons.Paste,
                        label = stringResource(R.string.paste),
                        onClick = { clipboard.getText()?.text?.let(onTextChange) },
                    )
                    if (text.isNotEmpty()) {
                        MiniChip(
                            icon = PFIcons.Close,
                            label = stringResource(R.string.clear),
                            muted = true,
                            onClick = { onTextChange("") },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs)) {
                    HowToLine(stringResource(R.string.paste_howto_quizlet))
                    HowToLine(stringResource(R.string.paste_howto_anki))
                    HowToLine(stringResource(R.string.paste_howto_other))
                }
            }

            if (text.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    ImportSectionLabel(
                        title = stringResource(R.string.preview),
                        trailing = if (parsed.cards.isEmpty()) stringResource(R.string.paste_no_cards_yet)
                        else stringResource(R.string.paste_n_cards, parsed.cards.size),
                        trailingColor = if (parsed.cards.isEmpty()) PFTheme.colors.warning else null,
                    )
                    if (parsed.cards.isEmpty()) {
                        Text(
                            text = stringResource(R.string.paste_format_help),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    } else {
                        PFTextField(
                            label = stringResource(R.string.paste_deck_name),
                            value = deckTitle,
                            onValueChange = onDeckTitleChange,
                            placeholder = stringResource(R.string.paste_deck_name_placeholder),
                            icon = PFIcons.Edit,
                        )
                        parsed.cards.take(5).forEach { card -> PreviewRow(card) }
                        if (parsed.cards.size > 5) {
                            Text(
                                text = stringResource(R.string.paste_and_n_more, parsed.cards.size - 5),
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.onSurfaceFaint,
                            )
                        }
                        if (parsed.skippedLines > 0) {
                            Text(
                                text = stringResource(R.string.paste_skipped_lines, parsed.skippedLines),
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.warning,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            PFIcons.Info,
            contentDescription = null,
            tint = PFTheme.colors.onSurfaceMuted,
            modifier = Modifier.size(14.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(PFTheme.spacing.xs))
        Text(text = text, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun PreviewRow(card: CardTextParser.ParsedCard) {
    val shape = RoundedCornerShape(PFRadius.md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.md),
    ) {
        Text(card.front, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface, maxLines = 2)
        Text(card.back, style = PFTheme.type.footnote, color = PFTheme.colors.onSurfaceMuted, maxLines = 2)
    }
}
