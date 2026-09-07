package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wapp.paperflipai.R
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Rename a deck — the Android counterpart of `RenameDeckSheet.swift`.
 */
@Composable
fun RenameDeckSheet(
    currentTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(currentTitle) }
    val trimmed = draft.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != currentTitle

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.rename_deck)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Text(
                text = stringResource(R.string.give_your_deck_a_clear_memorable_name),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )
            PFTextField(
                label = stringResource(R.string.deck_name),
                value = draft,
                onValueChange = { draft = it },
                placeholder = stringResource(R.string.atomic_habits_ch_1),
                icon = PFIcons.Deck,
            )
            PFButton(
                title = stringResource(R.string.save_2),
                onClick = { onSave(trimmed) },
                enabled = canSave,
            )
        }
    }
}
