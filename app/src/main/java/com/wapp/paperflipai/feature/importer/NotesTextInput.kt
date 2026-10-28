package com.wapp.paperflipai.feature.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.importing.SourceLimits
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * "From notes": paste or type text and the generator writes the cards.
 * Counterpart of `NotesTextInput.swift`.
 */
@Composable
internal fun NotesTextInput(
    text: String,
    onTextChange: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val trimmedCount = text.trim().length
    val isLongEnough = trimmedCount >= SourceLimits.MIN_SOURCE_CHARACTERS

    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
        ImportTextEditor(
            value = text,
            onValueChange = { onTextChange(it.take(SourceLimits.MAX_SOURCE_CHARACTERS)) },
            placeholder = stringResource(R.string.notes_placeholder),
            accessibilityLabel = stringResource(R.string.notes_accessibility),
            accent = PFTheme.colors.success,
            minHeight = 220.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniChip(
                icon = PFIcons.Paste,
                label = stringResource(R.string.paste),
                onClick = {
                    clipboard.getText()?.text?.let { pasted ->
                        val combined = if (text.isEmpty()) pasted else text + "\n" + pasted
                        onTextChange(combined.take(SourceLimits.MAX_SOURCE_CHARACTERS))
                    }
                },
            )
            if (text.isNotEmpty()) {
                Spacer(Modifier.width(PFTheme.spacing.sm))
                MiniChip(
                    icon = PFIcons.Close,
                    label = stringResource(R.string.clear),
                    muted = true,
                    onClick = { onTextChange("") },
                )
            }
            Spacer(Modifier.weight(1f))
            if (text.isNotEmpty()) {
                Text(
                    text = if (isLongEnough) stringResource(R.string.n_characters, trimmedCount)
                    else stringResource(
                        R.string.n_more_characters,
                        SourceLimits.MIN_SOURCE_CHARACTERS - trimmedCount,
                    ),
                    style = PFTheme.type.caption,
                    color = if (isLongEnough) PFTheme.colors.onSurfaceFaint else PFTheme.colors.warning,
                )
            }
        }
    }
}
