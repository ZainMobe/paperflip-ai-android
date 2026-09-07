package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.AppEnvironment
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.srs.StudyQueue
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFIconButton
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import com.wapp.paperflipai.util.asRelativeLabel
import com.wapp.paperflipai.util.daysFromNow
import kotlinx.coroutines.launch

/**
 * The screen behind every deck card — the Android counterpart of
 * `DeckDetailView.swift`. Hero with the Study CTA, the full card list with
 * expandable rows, an edit mode for reordering and deleting, and the
 * overflow menu for rename / move / share / delete.
 */
@Composable
fun DeckDetailScreen(
    deckId: String,
    onBack: () -> Unit,
    onStudy: (String) -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    val snapshot by env.database.snapshot.collectAsStateWithLifecycle()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()

    val deck = snapshot.decks.firstOrNull { it.id == deckId }
    val cards = remember(snapshot.cards, deckId) {
        snapshot.cards.filter { it.deckId == deckId }.sortedBy { it.orderIndex }
    }
    val folder = snapshot.folders.firstOrNull { it.id == deck?.folderId }

    var editing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showMoveFolder by remember { mutableStateOf(false) }
    var showMoveProject by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showCaughtUp by remember { mutableStateOf(false) }
    var isToggling by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    if (deck == null) {
        PFScreen(topBar = { PFTopBar(title = "", onBack = onBack) }) { padding ->
            PFEmptyState(
                icon = PFIcons.Deck,
                title = stringResource(R.string.that_didn_t_work),
                message = stringResource(R.string.we_couldn_t_find_that_deck),
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val dueCount = remember(cards) { StudyQueue.dueCount(cards) }
    val deleteTitle = stringResource(R.string.delete_3, deck.title)
    val deleteMessage = stringResource(R.string.this_deck_and_all_cards_will_be_removed_from_t, cards.size)
    val confirmLabel = stringResource(R.string.delete_deck)
    val cancelLabel = stringResource(R.string.cancel)

    PFScreen(
        topBar = {
            PFTopBar(
                title = deck.title,
                onBack = onBack,
                actions = {
                    PFIconButton(
                        icon = if (editing) PFIcons.Check else PFIcons.Edit,
                        contentDescription = stringResource(if (editing) R.string.done else R.string.edit),
                        onClick = { editing = !editing },
                        tint = PFTheme.colors.accent,
                    )
                    PFIconButton(
                        icon = PFIcons.More,
                        contentDescription = stringResource(R.string.nav_more),
                        onClick = { showMenu = true },
                        tint = PFTheme.colors.accent,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .pfReadableWidth(),
            contentPadding = PaddingValues(
                start = PFTheme.spacing.lg,
                end = PFTheme.spacing.lg,
                bottom = PFTheme.spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
        ) {
            item {
                DeckHero(
                    deck = deck,
                    folderName = folder?.name,
                    folderTint = folder.tint(),
                    cardCount = cards.size,
                    onStudy = {
                        if (dueCount == 0) showCaughtUp = true else onStudy(deck.id)
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PFTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.cards_2),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.total, cards.size),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.onSurfaceFaint,
                    )
                }
            }

            if (cards.isEmpty()) {
                item {
                    PFEmptyState(
                        icon = PFIcons.Card,
                        title = stringResource(R.string.no_cards_yet),
                        message = stringResource(R.string.this_deck_is_empty_cards_will_appear_here_once),
                    )
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    val index = cards.indexOfFirst { it.id == card.id }
                    if (editing) {
                        EditableCardRow(
                            card = card,
                            index = index,
                            canMoveUp = index > 0,
                            canMoveDown = index < cards.lastIndex,
                            onMoveUp = { scope.launch { reorder(env, cards, index, index - 1, deck.id) } },
                            onMoveDown = { scope.launch { reorder(env, cards, index, index + 1, deck.id) } },
                            onDelete = { scope.launch { env.repository.deleteCard(deck, card.id) } },
                        )
                    } else {
                        FlashcardRow(card = card, index = index)
                    }
                }
            }
        }
    }

    // ── Overflow menu ─────────────────────────────────────────────────
    if (showMenu) {
        PFSheet(onDismiss = { showMenu = false }, title = deck.title) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(
                        if (deck.isPublic) R.string.sharing_options else R.string.share_deck
                    ),
                    icon = if (deck.isPublic) PFIcons.Public else PFIcons.Share,
                    showChevron = false,
                    onClick = {
                        showMenu = false
                        if (entitlement.isActive) showShare = true else onOpenPaywall()
                    },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.rename_deck),
                    icon = PFIcons.Edit,
                    showChevron = false,
                    onClick = { showMenu = false; showRename = true },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.move_to_folder),
                    icon = PFIcons.Folder,
                    subtitle = folder?.name ?: stringResource(R.string.no_folder),
                    showChevron = false,
                    onClick = { showMenu = false; showMoveFolder = true },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(
                        if (deck.projectId == null) R.string.move_to_project else R.string.change_project
                    ),
                    icon = PFIcons.Group,
                    showChevron = false,
                    onClick = {
                        showMenu = false
                        if (entitlement.isActive) showMoveProject = true else onOpenPaywall()
                    },
                )
                deck.projectId?.let { projectId ->
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.project),
                        subtitle = snapshot.projects.firstOrNull { it.id == projectId }?.name,
                        icon = PFIcons.Group,
                        onClick = { showMenu = false; onOpenProject(projectId) },
                    )
                }
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.delete_deck),
                    icon = PFIcons.Delete,
                    iconTint = PFTheme.colors.danger,
                    titleColor = PFTheme.colors.danger,
                    showChevron = false,
                    onClick = {
                        showMenu = false
                        confirmation = PFConfirmation(
                            title = deleteTitle,
                            message = deleteMessage,
                            icon = PFIcons.Delete,
                            confirmTitle = confirmLabel,
                            cancelTitle = cancelLabel,
                            destructive = true,
                        ) {
                            env.repository.deleteDeck(deck)
                            onBack()
                        }
                    },
                )
            }
        }
    }

    if (showRename) {
        RenameDeckSheet(
            currentTitle = deck.title,
            onDismiss = { showRename = false },
            onSave = {
                showRename = false
                scope.launch { env.repository.renameDeck(deck, it) }
            },
        )
    }

    if (showMoveFolder) {
        PFSheet(onDismiss = { showMoveFolder = false }, title = stringResource(R.string.move_to_folder)) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(R.string.none),
                    icon = if (deck.folderId == null) PFIcons.Check else null,
                    showChevron = false,
                    onClick = {
                        showMoveFolder = false
                        scope.launch { env.repository.moveDeckToFolder(deck, null) }
                    },
                )
                snapshot.folders.forEach { candidate ->
                    PFRowDivider()
                    PFListRow(
                        title = candidate.name,
                        icon = if (deck.folderId == candidate.id) PFIcons.Check else null,
                        showChevron = false,
                        onClick = {
                            showMoveFolder = false
                            scope.launch { env.repository.moveDeckToFolder(deck, candidate.id) }
                        },
                    )
                }
            }
        }
    }

    if (showMoveProject) {
        PFSheet(onDismiss = { showMoveProject = false }, title = stringResource(R.string.move_to_project)) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(R.string.none_personal_deck),
                    icon = if (deck.projectId == null) PFIcons.Check else null,
                    showChevron = false,
                    onClick = {
                        showMoveProject = false
                        scope.launch { env.repository.moveDeckToProject(deck, null) }
                    },
                )
                snapshot.projects.forEach { project ->
                    PFRowDivider()
                    PFListRow(
                        title = project.name,
                        icon = if (deck.projectId == project.id) PFIcons.Check else null,
                        showChevron = false,
                        onClick = {
                            showMoveProject = false
                            scope.launch { env.repository.moveDeckToProject(deck, project.id) }
                        },
                    )
                }
            }
        }
    }

    if (showShare) {
        ShareDeckSheet(
            deck = deck,
            cardCount = cards.size,
            isToggling = isToggling,
            onToggle = { value ->
                scope.launch {
                    isToggling = true
                    val ok = env.repository.setDeckPublic(deck, value)
                    isToggling = false
                    if (!ok) PFToast.error("Couldn't update sharing")
                }
            },
            onDismiss = { showShare = false },
        )
    }

    if (showCaughtUp) {
        AllCaughtUpSheet(
            cards = cards,
            onDismiss = { showCaughtUp = false },
            onStudyAnyway = {
                showCaughtUp = false
                onStudy(deck.id)
            },
        )
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

private suspend fun reorder(
    env: AppEnvironment,
    cards: List<PFFlashcard>,
    from: Int,
    to: Int,
    deckId: String,
) {
    if (to !in cards.indices) return
    val reordered = cards.toMutableList().apply { add(to, removeAt(from)) }
    reordered.forEachIndexed { index, card ->
        if (card.orderIndex != index) env.database.upsertCard(card.copy(orderIndex = index))
    }
    env.database.deck(deckId)?.let { deck -> env.repository.touchDeck(deck) }
}

// ── Hero ─────────────────────────────────────────────────────────────

@Composable
private fun DeckHero(
    deck: PFDeck,
    folderName: String?,
    folderTint: Color,
    cardCount: Int,
    onStudy: () -> Unit,
) {
    val shape = RoundedCornerShape(PFRadius.xxl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PFTheme.spacing.md)
            .pfElevation(PFElevation.Card, shape)
            .background(PFTheme.colors.paper, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(deck.tint().copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = deck.icon,
                    contentDescription = null,
                    tint = deck.tint(),
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(PFTheme.spacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs)) {
                PFTagPill(title = deck.sourceLabel, icon = deck.icon, tint = deck.tint())
                folderName?.let { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(folderTint, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = name,
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
            Text(
                text = deck.title,
                style = PFTheme.type.title1,
                color = PFTheme.colors.onSurface,
            )
            deck.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
            MetaCell(
                icon = PFIcons.Deck,
                value = cardCount.toString(),
                label = stringResource(R.string.cards_3),
            )
            MetaCell(
                icon = PFIcons.Schedule,
                value = deck.lastStudiedAt?.asRelativeLabel() ?: "—",
                label = stringResource(R.string.last_studied),
            )
        }

        PFButton(
            title = stringResource(R.string.study_now),
            onClick = onStudy,
            size = PFButtonSize.Lg,
            trailingIcon = PFIcons.Forward,
            modifier = Modifier.padding(top = PFTheme.spacing.xs),
        )
    }
}

@Composable
private fun MetaCell(
    icon: ImageVector,
    value: String,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PFTheme.colors.accent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Column {
            Text(text = value, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
            Text(text = label, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
        }
    }
}

// ── Edit mode row ────────────────────────────────────────────────────

@Composable
private fun EditableCardRow(
    card: PFFlashcard,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${index + 1}", style = PFTheme.type.caption, color = PFTheme.colors.accent)
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = card.front,
                style = PFTheme.type.bodyEmphasis,
                color = PFTheme.colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = card.back,
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurfaceMuted,
                maxLines = 1,
            )
        }
        PFIconButton(
            icon = PFIcons.MoveUp,
            contentDescription = null,
            onClick = onMoveUp,
            enabled = canMoveUp,
            tint = PFTheme.colors.onSurfaceMuted,
        )
        PFIconButton(
            icon = PFIcons.MoveDown,
            contentDescription = null,
            onClick = onMoveDown,
            enabled = canMoveDown,
            tint = PFTheme.colors.onSurfaceMuted,
        )
        PFIconButton(
            icon = PFIcons.Delete,
            contentDescription = stringResource(R.string.delete_2),
            onClick = onDelete,
            tint = PFTheme.colors.danger,
        )
    }
}

// ── All caught up ────────────────────────────────────────────────────

@Composable
private fun AllCaughtUpSheet(
    cards: List<PFFlashcard>,
    onDismiss: () -> Unit,
    onStudyAnyway: () -> Unit,
) {
    val nextDue = remember(cards) {
        cards.filter { it.srsRepetitions > 0 }.minOfOrNull { it.srsDueDate }
    }
    val message = when {
        nextDue == null -> stringResource(R.string.study_any_deck_today_to_start_one)
        else -> {
            val days = daysFromNow(nextDue)
            when {
                days <= 0 -> stringResource(R.string.nothing_to_review_right_now_come_back_tomorrow)
                days == 1 -> stringResource(R.string.tomorrow)
                else -> stringResource(R.string.in_days, days)
            }
        }
    }

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.all_caught_up)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(PFTheme.colors.success.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PFIcons.CheckCircle,
                    contentDescription = null,
                    tint = PFTheme.colors.success,
                    modifier = Modifier.size(42.dp),
                )
            }
            Text(
                text = message,
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
                modifier = Modifier.padding(horizontal = PFTheme.spacing.lg),
            )
            PFButton(
                title = stringResource(R.string.study_anyway),
                onClick = onStudyAnyway,
                size = PFButtonSize.Lg,
                leadingIcon = PFIcons.Deck,
            )
        }
    }
}
