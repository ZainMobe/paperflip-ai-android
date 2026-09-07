package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.PFProject
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.feature.projects.ProjectMiniCard

/**
 * The projects strip at the top of the Library — the Android counterpart of
 * `LibraryHome.projectsRow`. Three states: populated, empty-but-Pro, and the
 * Pro upsell for free accounts.
 */
@Composable
fun ProjectsStrip(
    isPro: Boolean,
    projects: List<PFProject>,
    deckCountByProject: Map<String, Int>,
    onOpenProject: (String) -> Unit,
    onNewProject: () -> Unit,
    onJoinWithLink: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenInvites: () -> Unit,
) {
    var showAddMenu by remember { mutableStateOf(false) }

    when {
        isPro && projects.isNotEmpty() -> {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.projects),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .pfPressable(
                                onClick = { showAddMenu = true },
                                haptic = PFHaptic.Light,
                                pressedScale = 0.94f,
                            )
                            .padding(PFTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = PFIcons.Add,
                            contentDescription = null,
                            tint = PFTheme.colors.accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(PFTheme.spacing.xs))
                        Text(
                            text = stringResource(R.string.new_status),
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.accent,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
                ) {
                    projects.forEach { project ->
                        ProjectMiniCard(
                            project = project,
                            deckCount = deckCountByProject[project.id] ?: 0,
                            onClick = { onOpenProject(project.id) },
                        )
                    }
                    NewProjectTile(onClick = onNewProject)
                }
            }
        }

        isPro -> StripCard(
            title = stringResource(R.string.create_your_first_project),
            subtitle = stringResource(R.string.collaborate_on_decks_with_a_team),
            trailingIcon = PFIcons.Add,
            onClick = onNewProject,
        )

        else -> StripCard(
            title = stringResource(R.string.projects_2),
            subtitle = stringResource(R.string.collaborate_on_decks_with_a_team),
            trailingIcon = PFIcons.Chevron,
            proPill = true,
            onClick = onOpenPaywall,
        )
    }

    if (showAddMenu) {
        PFSheet(
            onDismiss = { showAddMenu = false },
            title = stringResource(R.string.projects_2),
        ) {
            PFGroupedCard(modifier = Modifier.padding(horizontal = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(R.string.new_project),
                    icon = PFIcons.Add,
                    showChevron = false,
                    onClick = {
                        showAddMenu = false
                        onNewProject()
                    },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.join_with_link),
                    icon = PFIcons.Link,
                    showChevron = false,
                    onClick = {
                        showAddMenu = false
                        onJoinWithLink()
                    },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.project_invites),
                    icon = PFIcons.PersonAdd,
                    onClick = {
                        showAddMenu = false
                        onOpenInvites()
                    },
                )
            }
        }
    }
}

@Composable
private fun StripCard(
    title: String,
    subtitle: String,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    proPill: Boolean = false,
) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .pfPressable(onClick = onClick, pressedScale = 0.985f)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(PFTheme.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PFIcons.Group,
                contentDescription = null,
                tint = PFTheme.colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = PFTheme.type.bodyEmphasis,
                    color = PFTheme.colors.onSurface,
                )
                if (proPill) {
                    Spacer(Modifier.width(6.dp))
                    PFTagPill(
                        title = stringResource(R.string.pro),
                        icon = PFIcons.Pro,
                        prominent = true,
                    )
                }
            }
            Text(
                text = subtitle,
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = PFTheme.colors.accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NewProjectTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .height(124.dp)
            .border(
                width = 1.5.dp,
                color = PFTheme.colors.accent.copy(alpha = 0.4f),
                shape = RoundedCornerShape(PFRadius.lg),
            )
            .pfPressable(onClick = onClick, haptic = PFHaptic.Light, pressedScale = 0.96f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = PFIcons.Add,
            contentDescription = null,
            tint = PFTheme.colors.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(PFTheme.spacing.sm))
        Text(
            text = stringResource(R.string.new_status),
            style = PFTheme.type.footnoteBold,
            color = PFTheme.colors.accent,
        )
    }
}
