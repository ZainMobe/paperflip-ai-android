package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFolder
import com.wapp.paperflipai.core.data.PFProject
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFSectionHeader
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.theme.PFTheme

/**
 * Long-press actions for a deck — the Android counterpart of the iOS
 * context menu in `LibraryHome.swift`. A bottom sheet rather than a popup
 * menu, because the folder and project pickers need room to breathe.
 */
@Composable
fun DeckActionsSheet(
    deck: PFDeck,
    folders: List<PFFolder>,
    projects: List<PFProject>,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMoveToFolder: (PFFolder?) -> Unit,
    onMoveToProject: (PFProject?) -> Unit,
    onTogglePublic: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    var showFolders by remember { mutableStateOf(false) }
    var showProjects by remember { mutableStateOf(false) }

    PFSheet(onDismiss = onDismiss, title = deck.title) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
        ) {
            PFGroupedCard {
                PFListRow(
                    title = stringResource(R.string.rename),
                    icon = PFIcons.Edit,
                    showChevron = false,
                    onClick = onRename,
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.move_to_folder),
                    subtitle = folders.firstOrNull { it.id == deck.folderId }?.name
                        ?: stringResource(R.string.no_folder),
                    icon = PFIcons.Folder,
                    onClick = { showFolders = !showFolders },
                )
                if (showFolders) {
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.none),
                        icon = if (deck.folderId == null) PFIcons.Check else null,
                        showChevron = false,
                        onClick = { onMoveToFolder(null) },
                    )
                    folders.forEach { folder ->
                        PFListRow(
                            title = folder.name,
                            icon = if (deck.folderId == folder.id) PFIcons.Check else null,
                            showChevron = false,
                            onClick = { onMoveToFolder(folder) },
                        )
                    }
                }
                PFRowDivider()
                PFListRow(
                    title = stringResource(
                        if (deck.projectId == null) R.string.move_to_project else R.string.change_project
                    ),
                    subtitle = projects.firstOrNull { it.id == deck.projectId }?.name
                        ?: stringResource(R.string.none_personal),
                    icon = PFIcons.Group,
                    onClick = { showProjects = !showProjects },
                )
                if (showProjects) {
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.none_personal),
                        icon = if (deck.projectId == null) PFIcons.Check else null,
                        showChevron = false,
                        onClick = { onMoveToProject(null) },
                    )
                    projects.forEach { project ->
                        PFListRow(
                            title = project.name,
                            icon = if (deck.projectId == project.id) PFIcons.Check else null,
                            showChevron = false,
                            onClick = { onMoveToProject(project) },
                        )
                    }
                }
                onTogglePublic?.let {
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.public_deck),
                        subtitle = stringResource(R.string.generate_a_link_friends_can_import),
                        icon = PFIcons.Public,
                        showChevron = false,
                        onClick = it,
                    )
                }
            }

            PFSectionHeader(title = stringResource(R.string.danger_zone))

            PFGroupedCard {
                PFListRow(
                    title = stringResource(R.string.delete_deck),
                    icon = PFIcons.Delete,
                    iconTint = PFTheme.colors.danger,
                    titleColor = PFTheme.colors.danger,
                    showChevron = false,
                    onClick = onDelete,
                )
            }
        }
    }
}
