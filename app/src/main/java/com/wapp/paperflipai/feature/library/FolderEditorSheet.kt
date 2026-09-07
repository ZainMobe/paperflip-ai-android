package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFFolder
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.parseHexColor

/**
 * Create / rename / delete a folder — the Android counterpart of
 * `FolderEditorSheet.swift`. A curated palette rather than a hex picker,
 * so the visual language stays coherent.
 */
private val folderPalette = listOf(
    "Indigo" to "#5B5BD6",
    "Violet" to "#8B5CF6",
    "Pink" to "#EC4899",
    "Red" to "#DC2626",
    "Orange" to "#EA580C",
    "Amber" to "#D97706",
    "Green" to "#16A34A",
    "Teal" to "#0D9488",
    "Blue" to "#2563EB",
    "Gray" to "#6B7280",
)

@Composable
fun FolderEditorSheet(
    existing: PFFolder?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: folderPalette[0].second) }

    val trimmed = name.trim()
    val canSave = trimmed.isNotEmpty() &&
        (existing == null || trimmed != existing.name || colorHex != existing.colorHex)

    PFSheet(
        onDismiss = onDismiss,
        title = stringResource(if (existing == null) R.string.new_folder else R.string.edit_folder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            // Live preview chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FolderChip(
                    label = trimmed.ifEmpty { stringResource(R.string.folder_name) },
                    count = 0,
                    isActive = true,
                    onClick = {},
                    tint = parseHexColor(colorHex) ?: PFTheme.colors.accent,
                )
            }

            PFTextField(
                label = stringResource(R.string.folder_name),
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. School, Work, Spanish",
                icon = PFIcons.Folder,
            )

            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                Text(
                    text = stringResource(R.string.color),
                    style = PFTheme.type.footnoteBold,
                    color = PFTheme.colors.onSurfaceMuted,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
                    userScrollEnabled = false,
                ) {
                    items(folderPalette) { swatch ->
                        val hex = swatch.second
                        val color = parseHexColor(hex) ?: PFTheme.colors.accent
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .pfPressable(
                                    onClick = { colorHex = hex },
                                    haptic = PFHaptic.Selection,
                                    pressedScale = 0.92f,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 52.dp else 44.dp)
                                    .then(
                                        if (selected) Modifier.border(
                                            3.dp, PFTheme.colors.onSurface, CircleShape
                                        ) else Modifier
                                    )
                                    .padding(if (selected) 4.dp else 0.dp)
                                    .background(color, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = PFIcons.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PFButton(
                title = stringResource(if (existing == null) R.string.create else R.string.save_2),
                onClick = { onSave(trimmed, colorHex) },
                enabled = canSave,
            )

            onDelete?.let {
                PFButton(
                    title = stringResource(R.string.delete_folder),
                    onClick = it,
                    variant = PFButtonVariant.Tonal,
                    leadingIcon = PFIcons.Delete,
                    isDestructive = true,
                )
            }
        }
    }
}
