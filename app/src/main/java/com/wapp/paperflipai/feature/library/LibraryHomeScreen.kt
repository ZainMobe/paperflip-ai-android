package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.IntentInbox
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.app.PFAction
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFolder
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFIconButton
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSearchField
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.component.tabBarContentPadding
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.projects.JoinProjectSheet
import com.wapp.paperflipai.feature.projects.NewProjectSheet
import com.wapp.paperflipai.feature.projects.ProjectMiniCard
import kotlinx.coroutines.launch

/**
 * The Library — the Android counterpart of `LibraryHome.swift`. Search,
 * folder chips, grid/list toggle, deck cards with long-press actions,
 * pull-to-refresh, the projects strip, and every empty state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHomeScreen(
    onOpenDeck: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenImport: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenInvites: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    val decks by env.database.decks.collectAsStateWithLifecycle()
    val folders by env.database.folders.collectAsStateWithLifecycle()
    val projects by env.database.projects.collectAsStateWithLifecycle()
    val cards by env.database.cards.collectAsStateWithLifecycle()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()
    val isSyncing by env.sync.isSyncing.collectAsStateWithLifecycle()
    val syncError by env.sync.lastError.collectAsStateWithLifecycle()

    // SyncEngine records why a pull failed but nothing was reading it, so a
    // failed refresh looked identical to a successful one with no new data.
    // Library is where refresh lives, so it is where the failure belongs.
    val syncFailedLabel = stringResource(R.string.sync_failed)
    LaunchedEffect(syncError) {
        syncError?.let { PFToast.warning(syncFailedLabel, it) }
    }
    val session by env.auth.session.collectAsStateWithLifecycle()
    val useGrid by env.settings.libraryGrid.collectAsStateWithLifecycle()
    val pendingAction by IntentInbox.pending.collectAsStateWithLifecycle()

    var searchText by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var actionsDeck by remember { mutableStateOf<PFDeck?>(null) }
    var renameDeck by remember { mutableStateOf<PFDeck?>(null) }
    var folderEditor by remember { mutableStateOf<FolderEditorTarget?>(null) }
    var showNewProject by remember { mutableStateOf(false) }
    var joinToken by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    val deleteFolderTitle = stringResource(R.string.delete_this_folder)
    val deleteFolderMessage = stringResource(R.string.decks_inside_will_move_to_all_they_won_t_be_de)
    val deleteFolderConfirm = stringResource(R.string.delete_folder)
    val cancelFolderLabel = stringResource(R.string.cancel)
    var unreadCount by remember { mutableStateOf(0) }

    val cardCounts = remember(cards) { cards.groupingBy { it.deckId }.eachCount() }
    val folderById = remember(folders) { folders.associateBy { it.id } }
    val deckCountByFolder = remember(decks) { decks.groupingBy { it.folderId.orEmpty() }.eachCount() }
    val deckCountByProject = remember(decks) { decks.groupingBy { it.projectId.orEmpty() }.eachCount() }

    val visibleDecks = remember(decks, selectedFolderId, searchText) {
        decks.filter { deck ->
            (selectedFolderId == null || deck.folderId == selectedFolderId) &&
                (searchText.isBlank() ||
                    deck.title.contains(searchText, ignoreCase = true) ||
                    deck.summary.orEmpty().contains(searchText, ignoreCase = true))
        }
    }

    suspend fun refresh() {
        val userId = session?.userId ?: return
        env.sync.pullAll(userId)
        env.sync.pullProjects(userId)
        runCatching {
            unreadCount = env.remote.fetchNotifications(userId).count { !it.read }
        }
        env.refreshWidget()
    }

    LaunchedEffect(session?.userId) { refresh() }

    // Deep links that belong to the Library.
    LaunchedEffect(pendingAction) {
        when (val action = pendingAction) {
            is PFAction.JoinProject -> {
                joinToken = action.token
                IntentInbox.clear()
            }
            is PFAction.ImportSharedDeck -> {
                IntentInbox.clear()
                onOpenImport()
            }
            else -> Unit
        }
    }

    val pullState = rememberPullToRefreshState()

    PFScreen(
        topBar = {
            PFTopBar(
                title = stringResource(R.string.library_2),
                actions = {
                    Box {
                        PFIconButton(
                            icon = PFIcons.Notifications,
                            contentDescription = if (unreadCount > 0) {
                                stringResource(R.string.notifications_unread, unreadCount)
                            } else {
                                stringResource(R.string.notifications)
                            },
                            onClick = onOpenNotifications,
                            tint = PFTheme.colors.accent,
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .size(9.dp)
                                    .background(PFTheme.colors.danger, CircleShape)
                            )
                        }
                    }
                    PFIconButton(
                        icon = if (useGrid) PFIcons.Grid else PFIcons.List,
                        contentDescription = stringResource(if (useGrid) R.string.grid else R.string.list),
                        onClick = { env.settings.setLibraryGrid(!useGrid) },
                        tint = PFTheme.colors.accent,
                    )
                    PFIconButton(
                        icon = PFIcons.Add,
                        contentDescription = stringResource(R.string.import_action),
                        onClick = onOpenImport,
                        tint = PFTheme.colors.accent,
                    )
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { scope.launch { refresh() } },
            state = pullState,
            modifier = Modifier.padding(padding),
        ) {
            LazyVerticalGrid(
                columns = if (useGrid) GridCells.Adaptive(minSize = 168.dp) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = PFTheme.spacing.lg,
                    end = PFTheme.spacing.lg,
                    bottom = tabBarContentPadding(PFTheme.spacing.lg),
                ),
                horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(if (useGrid) PFTheme.spacing.md else PFTheme.spacing.sm),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                        PFSearchField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = stringResource(R.string.search_decks),
                        )

                        ProjectsStrip(
                            isPro = entitlement.isActive,
                            projects = projects,
                            deckCountByProject = deckCountByProject,
                            onOpenProject = onOpenProject,
                            onNewProject = { showNewProject = true },
                            onJoinWithLink = { joinToken = "" },
                            onOpenPaywall = onOpenPaywall,
                            onOpenInvites = onOpenInvites,
                        )

                        FolderRow(
                            folders = folders,
                            allCount = decks.size,
                            deckCountByFolder = deckCountByFolder,
                            selectedFolderId = selectedFolderId,
                            onSelect = { selectedFolderId = it },
                            onEditFolder = { folderEditor = FolderEditorTarget(it) },
                            onNewFolder = { folderEditor = FolderEditorTarget(null) },
                        )
                    }
                }

                if (decks.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PFEmptyState(
                            icon = PFIcons.Deck,
                            title = stringResource(R.string.no_decks_yet),
                            message = stringResource(R.string.drop_in_a_pdf_youtube_link_or_article_and_we_l),
                            ctaTitle = stringResource(R.string.create_your_first_deck),
                            onCta = onOpenImport,
                            modifier = Modifier.padding(top = PFTheme.spacing.xxl),
                        )
                    }
                } else if (visibleDecks.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PFEmptyState(
                            icon = PFIcons.Search,
                            title = stringResource(
                                if (searchText.isBlank()) R.string.nothing_in_this_folder_yet
                                else R.string.no_matches
                            ),
                            message = stringResource(
                                if (searchText.isBlank()) R.string.move_a_deck_here_from_the_menu_or_import_a_new
                                else R.string.try_a_different_search_term_or_check_the_folde
                            ),
                            modifier = Modifier.padding(top = PFTheme.spacing.xl),
                        )
                    }
                } else {
                    items(visibleDecks, key = { it.id }) { deck ->
                        DeckCard(
                            deck = deck,
                            cardCount = cardCounts[deck.id] ?: 0,
                            folder = folderById[deck.folderId],
                            layout = if (useGrid) DeckCardLayout.Grid else DeckCardLayout.List,
                            onClick = { onOpenDeck(deck.id) },
                            onLongClick = { actionsDeck = deck },
                        )
                    }
                }
            }
        }
    }

    // ── Sheets & dialogs ──────────────────────────────────────────────

    actionsDeck?.let { deck ->
        val deckTitle = deck.title
        val deleteMessage = stringResource(R.string.this_removes_the_deck_and_all_its_cards_from_t)
        val deleteTitle = stringResource(R.string.delete_3, deckTitle)
        val confirmLabel = stringResource(R.string.delete_2)
        val cancelLabel = stringResource(R.string.cancel)
        val deletedToast = stringResource(R.string.delete_deck)

        DeckActionsSheet(
            deck = deck,
            folders = folders,
            projects = projects,
            onDismiss = { actionsDeck = null },
            onRename = {
                actionsDeck = null
                renameDeck = deck
            },
            onMoveToFolder = { folder ->
                actionsDeck = null
                scope.launch { env.repository.moveDeckToFolder(deck, folder?.id) }
            },
            onMoveToProject = { project ->
                actionsDeck = null
                if (!entitlement.isActive) {
                    onOpenPaywall()
                } else {
                    scope.launch {
                        val ok = env.repository.moveDeckToProject(deck, project?.id)
                        if (!ok) PFToast.error("Couldn't move deck")
                    }
                }
            },
            onDelete = {
                actionsDeck = null
                confirmation = PFConfirmation(
                    title = deleteTitle,
                    message = deleteMessage,
                    icon = PFIcons.Delete,
                    confirmTitle = confirmLabel,
                    cancelTitle = cancelLabel,
                    destructive = true,
                ) {
                    env.repository.deleteDeck(deck)
                    PFToast.warning(deletedToast, deckTitle)
                }
            },
        )
    }

    renameDeck?.let { deck ->
        RenameDeckSheet(
            currentTitle = deck.title,
            onDismiss = { renameDeck = null },
            onSave = { title ->
                renameDeck = null
                scope.launch { env.repository.renameDeck(deck, title) }
            },
        )
    }

    folderEditor?.let { target ->
        val userId = session?.userId
        FolderEditorSheet(
            existing = target.folder,
            onDismiss = { folderEditor = null },
            onSave = { name, colorHex ->
                folderEditor = null
                scope.launch {
                    val existing = target.folder
                    if (existing == null) {
                        if (userId != null) env.repository.createFolder(userId, name, colorHex)
                    } else {
                        env.repository.updateFolder(existing, name, colorHex)
                    }
                }
            },
            onDelete = target.folder?.let { folder ->
                {
                    // iOS puts a confirmationDialog here; deleting a folder is
                    // one tap away from reorganising the whole library and has
                    // no undo. The decks survive — say so.
                    folderEditor = null
                    confirmation = PFConfirmation(
                        title = deleteFolderTitle,
                        message = deleteFolderMessage,
                        icon = PFIcons.Delete,
                        confirmTitle = deleteFolderConfirm,
                        cancelTitle = cancelFolderLabel,
                        destructive = true,
                    ) {
                        if (selectedFolderId == folder.id) selectedFolderId = null
                        env.repository.deleteFolder(folder.id)
                    }
                }
            },
        )
    }

    if (showNewProject) {
        NewProjectSheet(onDismiss = { showNewProject = false }, onCreated = { showNewProject = false })
    }

    joinToken?.let { token ->
        JoinProjectSheet(
            initialToken = token,
            onDismiss = { joinToken = null },
        )
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

/** Wrapper so `null` can mean "create" while still being a non-null target. */
data class FolderEditorTarget(val folder: PFFolder?)

// ── Folder row ───────────────────────────────────────────────────────

@Composable
private fun FolderRow(
    folders: List<PFFolder>,
    allCount: Int,
    deckCountByFolder: Map<String, Int>,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit,
    onEditFolder: (PFFolder) -> Unit,
    onNewFolder: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderChip(
            label = stringResource(R.string.all),
            count = allCount,
            isActive = selectedFolderId == null,
            onClick = { onSelect(null) },
            icon = PFIcons.Deck,
        )
        folders.forEach { folder ->
            FolderChip(
                label = folder.name,
                count = deckCountByFolder[folder.id] ?: 0,
                isActive = selectedFolderId == folder.id,
                onClick = { onSelect(folder.id) },
                onLongClick = { onEditFolder(folder) },
                tint = folder.tint(),
            )
        }
        Row(
            modifier = Modifier
                .border(1.dp, PFTheme.colors.border, CircleShape)
                .pfPressable(onClick = onNewFolder, haptic = PFHaptic.Light, pressedScale = 0.96f)
                .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PFIcons.Add,
                contentDescription = null,
                tint = PFTheme.colors.onSurfaceMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(PFTheme.spacing.xs))
            Text(
                text = stringResource(R.string.folder),
                style = PFTheme.type.footnoteBold,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
    }
}
