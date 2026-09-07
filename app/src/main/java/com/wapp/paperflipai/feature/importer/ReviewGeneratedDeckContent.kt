package com.wapp.paperflipai.feature.importer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.CardDto
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.core.data.newId
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlinx.coroutines.launch

/**
 * Edit-before-save — the Android counterpart of
 * `ReviewGeneratedDeckView.swift`. The generated deck lives in local state
 * until "Save to library" is tapped, so "Discard" really discards.
 */
data class EditableCard(
    val id: String = newId(),
    val front: String = "",
    val back: String = "",
    val hint: String? = null,
)

@Composable
fun ReviewGeneratedDeckContent(
    dto: DeckDto,
    onSaved: (String) -> Unit,
    onDiscarded: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()

    val folders by env.database.folders.collectAsStateWithLifecycle()
    val projects by env.database.projects.collectAsStateWithLifecycle()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf(dto.title) }
    var titleEditing by remember { mutableStateOf(false) }
    val cards = remember {
        dto.cards.map { EditableCard(it.id ?: newId(), it.front, it.back, it.hint) }
            .toMutableStateList()
    }
    var folderId by remember { mutableStateOf<String?>(dto.folderId) }
    var projectId by remember { mutableStateOf<String?>(dto.projectId) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    val sourceType = PFSourceType.from(dto.sourceType)
    val tint = when (sourceType) {
        PFSourceType.Pdf -> PFTheme.colors.accent
        PFSourceType.Youtube -> PFTheme.colors.danger
        PFSourceType.Article -> PFTheme.colors.warning
    }
    val icon = when (sourceType) {
        PFSourceType.Pdf -> PFIcons.Pdf
        PFSourceType.Youtube -> PFIcons.Video
        PFSourceType.Article -> PFIcons.Article
    }

    val discardTitle = stringResource(R.string.discard_this_deck)
    val discardMessage = stringResource(R.string.generated_card_will_be_lost_this_can_t_be_undo, cards.size, "")
    val discardConfirm = stringResource(R.string.discard)
    val keepEditing = stringResource(R.string.keep_editing)

    PFScreen(
        topBar = {
            PFTopBar(
                title = stringResource(R.string.review),
                onBack = {
                    confirmation = PFConfirmation(
                        title = discardTitle,
                        message = discardMessage,
                        icon = PFIcons.Delete,
                        confirmTitle = discardConfirm,
                        cancelTitle = keepEditing,
                        destructive = true,
                    ) { onDiscarded() }
                },
                navigationIcon = PFIcons.Close,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .pfReadableWidth()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = PFTheme.spacing.lg,
                    end = PFTheme.spacing.lg,
                    bottom = 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
            ) {
                item {
                    // ── Hero ──────────────────────────────────────────
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
                                    .size(56.dp)
                                    .background(tint.copy(alpha = 0.14f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(PFTheme.spacing.sm))
                            PFTagPill(title = sourceType.raw, icon = icon, tint = tint)
                        }

                        if (titleEditing) {
                            PFTextField(
                                label = "",
                                value = title,
                                onValueChange = { title = it },
                                placeholder = stringResource(R.string.deck_title),
                                singleLine = false,
                                imeAction = ImeAction.Done,
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pfPressable(
                                        onClick = { titleEditing = true },
                                        haptic = PFHaptic.Selection,
                                        pressedScale = 0.99f,
                                    ),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = title,
                                    style = PFTheme.type.title1,
                                    color = PFTheme.colors.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = PFIcons.Edit,
                                    contentDescription = null,
                                    tint = PFTheme.colors.onSurfaceFaint,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs)) {
                            Text(
                                text = stringResource(R.string.save_to),
                                style = PFTheme.type.overline,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                                LocationChip(
                                    icon = PFIcons.Group,
                                    label = projects.firstOrNull { it.id == projectId }?.name
                                        ?: stringResource(R.string.personal_library),
                                    active = projectId != null,
                                    onClick = {
                                        if (entitlement.isActive) showProjectPicker = true else onOpenPaywall()
                                    },
                                )
                                LocationChip(
                                    icon = PFIcons.Folder,
                                    label = folders.firstOrNull { it.id == folderId }?.name
                                        ?: stringResource(R.string.no_folder),
                                    active = folderId != null,
                                    onClick = { showFolderPicker = true },
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    PFIcons.Deck, null,
                                    tint = PFTheme.colors.onSurfaceMuted,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(PFTheme.spacing.xs))
                                Text(
                                    text = stringResource(R.string.cards, cards.size),
                                    style = PFTheme.type.footnote,
                                    color = PFTheme.colors.onSurfaceMuted,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    PFIcons.Sparkle, null,
                                    tint = PFTheme.colors.accent,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(PFTheme.spacing.xs))
                                Text(
                                    text = stringResource(R.string.just_generated),
                                    style = PFTheme.type.footnote,
                                    color = PFTheme.colors.accent,
                                )
                            }
                        }
                    }
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
                        Row(
                            modifier = Modifier
                                .pfPressable(
                                    onClick = {
                                        val card = EditableCard()
                                        cards.add(card)
                                        expandedId = card.id
                                    },
                                    pressedScale = 0.94f,
                                )
                                .padding(PFTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                PFIcons.Add, null,
                                tint = PFTheme.colors.accent,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(PFTheme.spacing.xs))
                            Text(
                                text = stringResource(R.string.add),
                                style = PFTheme.type.footnoteBold,
                                color = PFTheme.colors.accent,
                            )
                        }
                    }
                }

                items(cards, key = { it.id }) { card ->
                    val index = cards.indexOfFirst { it.id == card.id }
                    EditableCardRow(
                        index = index,
                        card = card,
                        expanded = expandedId == card.id,
                        onToggle = { expandedId = if (expandedId == card.id) null else card.id },
                        onChange = { updated ->
                            val position = cards.indexOfFirst { it.id == card.id }
                            if (position >= 0) cards[position] = updated
                        },
                        onDelete = {
                            cards.removeAll { it.id == card.id }
                            if (expandedId == card.id) expandedId = null
                        },
                    )
                }
            }

            // ── Sticky save bar ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(PFTheme.colors.surface)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.7.dp)
                        .background(PFTheme.colors.border)
                )
                PFButton(
                    title = stringResource(R.string.save_to_library),
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isEmpty()) {
                            titleEditing = true
                        } else scope.launch {
                            val saved = env.repository.saveDeck(
                                dto = dto.copy(
                                    title = trimmed,
                                    cards = cards.map {
                                        CardDto(it.id, it.front, it.back, it.hint)
                                    },
                                ),
                                folderId = folderId,
                                projectId = projectId,
                            )
                            runCatching {
                                env.remote.createDeck(
                                    dto.copy(
                                        title = trimmed,
                                        folderId = folderId,
                                        projectId = projectId,
                                        cards = cards.map { CardDto(it.id, it.front, it.back, it.hint) },
                                    )
                                )
                            }
                            env.refreshWidget()
                            onSaved(saved.id)
                        }
                    },
                    size = PFButtonSize.Lg,
                    leadingIcon = PFIcons.Check,
                    modifier = Modifier
                        .padding(horizontal = PFTheme.spacing.lg)
                        .padding(top = PFTheme.spacing.md, bottom = PFTheme.spacing.lg),
                )
            }
        }
    }

    if (showFolderPicker) {
        PFSheet(onDismiss = { showFolderPicker = false }, title = stringResource(R.string.folder)) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(R.string.no_folder),
                    icon = if (folderId == null) PFIcons.Check else null,
                    showChevron = false,
                    onClick = { folderId = null; showFolderPicker = false },
                )
                folders.forEach { folder ->
                    PFRowDivider()
                    PFListRow(
                        title = folder.name,
                        icon = if (folderId == folder.id) PFIcons.Check else null,
                        showChevron = false,
                        onClick = { folderId = folder.id; showFolderPicker = false },
                    )
                }
            }
        }
    }

    if (showProjectPicker) {
        PFSheet(onDismiss = { showProjectPicker = false }, title = stringResource(R.string.save_location)) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(R.string.personal_library),
                    subtitle = stringResource(R.string.only_you_see_this_deck),
                    icon = if (projectId == null) PFIcons.Check else PFIcons.Person,
                    showChevron = false,
                    onClick = { projectId = null; showProjectPicker = false },
                )
                if (projects.isEmpty()) {
                    PFRowDivider()
                    PFListRow(
                        title = stringResource(R.string.create_a_project_from_the_library_tab_to_share),
                        showChevron = false,
                        enabled = false,
                    )
                } else {
                    projects.forEach { project ->
                        PFRowDivider()
                        PFListRow(
                            title = project.name,
                            icon = if (projectId == project.id) PFIcons.Check else PFIcons.Group,
                            showChevron = false,
                            onClick = { projectId = project.id; showProjectPicker = false },
                        )
                    }
                }
            }
        }
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

@Composable
private fun LocationChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val foreground = if (active) PFTheme.colors.accent else PFTheme.colors.onSurfaceMuted
    Row(
        modifier = Modifier
            .background(
                if (active) PFTheme.colors.accentSoft else PFTheme.colors.elevated,
                CircleShape,
            )
            .border(0.7.dp, PFTheme.colors.border, CircleShape)
            .pfPressable(onClick = onClick, pressedScale = 0.96f)
            .padding(horizontal = PFTheme.spacing.md, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = label, style = PFTheme.type.footnoteBold, color = foreground, maxLines = 1)
        Spacer(Modifier.width(PFTheme.spacing.xs))
        Icon(PFIcons.Expand, contentDescription = null, tint = foreground, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun EditableCardRow(
    index: Int,
    card: EditableCard,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (EditableCard) -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(PFRadius.lg)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = PFMotion.snappy(),
        label = "reviewCardChevron",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(
                width = if (expanded) 1.5.dp else 0.7.dp,
                color = if (expanded) PFTheme.colors.accent.copy(alpha = 0.4f) else PFTheme.colors.border,
                shape = shape,
            )
            .padding(PFTheme.spacing.lg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pfPressable(onClick = onToggle, haptic = PFHaptic.Selection, pressedScale = 0.995f),
            verticalAlignment = Alignment.Top,
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
                    text = card.front.ifEmpty { stringResource(R.string.new_card) },
                    style = PFTheme.type.bodyEmphasis,
                    color = if (card.front.isEmpty()) PFTheme.colors.onSurfaceFaint
                    else PFTheme.colors.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                )
                if (!expanded && card.back.isNotEmpty()) {
                    Text(
                        text = card.back,
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                        maxLines = 1,
                    )
                }
            }
            if (expanded) {
                Icon(
                    imageVector = PFIcons.Delete,
                    contentDescription = null,
                    tint = PFTheme.colors.danger,
                    modifier = Modifier
                        .size(28.dp)
                        .background(PFTheme.colors.danger.copy(alpha = 0.10f), CircleShape)
                        .pfPressable(onClick = onDelete, pressedScale = 0.9f)
                        .padding(6.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.sm))
            }
            Icon(
                imageVector = PFIcons.Expand,
                contentDescription = null,
                tint = PFTheme.colors.onSurfaceFaint,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = PFTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
            ) {
                PFTextField(
                    label = stringResource(R.string.front),
                    value = card.front,
                    onValueChange = { onChange(card.copy(front = it)) },
                    placeholder = stringResource(R.string.question_placeholder),
                    singleLine = false,
                    minLines = 2,
                    imeAction = ImeAction.Next,
                )
                PFTextField(
                    label = stringResource(R.string.back),
                    value = card.back,
                    onValueChange = { onChange(card.copy(back = it)) },
                    placeholder = stringResource(R.string.answer_placeholder),
                    singleLine = false,
                    minLines = 2,
                    imeAction = ImeAction.Done,
                )
            }
        }
    }
}
