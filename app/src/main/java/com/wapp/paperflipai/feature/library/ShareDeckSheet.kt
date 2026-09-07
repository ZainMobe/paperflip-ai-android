package com.wapp.paperflipai.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.PFLinks
import com.wapp.paperflipai.core.data.PFDeck
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFSwitch
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.copyToClipboard
import com.wapp.paperflipai.util.shareText

/**
 * Public-link sharing for a deck — the Android counterpart of the private
 * `ShareDeckSheet` in `DeckDetailView.swift`.
 */
@Composable
fun ShareDeckSheet(
    deck: PFDeck,
    cardCount: Int,
    isToggling: Boolean,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shareUrl = PFLinks.sharedDeck(deck.id.lowercase())
    val copiedLabel = stringResource(R.string.copy_link)

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.share_deck)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PFTheme.colors.accentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (deck.isPublic) PFIcons.Public else PFIcons.Share,
                        contentDescription = null,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = deck.title,
                        style = PFTheme.type.headline,
                        color = PFTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.cards, cardCount),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            PFCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.public_deck),
                            style = PFTheme.type.bodyEmphasis,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.anyone_with_the_link_can_import_a_copy),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                    if (isToggling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PFTheme.colors.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        PFSwitch(checked = deck.isPublic, onCheckedChange = onToggle)
                    }
                }
            }

            if (deck.isPublic) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.share_link),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    PFCard {
                        Text(
                            text = shareUrl,
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    PFTheme.colors.elevatedHigh,
                                    RoundedCornerShape(PFRadius.md),
                                )
                                .padding(PFTheme.spacing.md),
                        )
                        Spacer(Modifier.size(PFTheme.spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                            PillAction(
                                icon = PFIcons.Copy,
                                label = stringResource(R.string.copy_link),
                                filled = false,
                                onClick = {
                                    context.copyToClipboard("PaperFlip", shareUrl)
                                    PFToast.success(copiedLabel)
                                },
                            )
                            PillAction(
                                icon = PFIcons.Share,
                                label = stringResource(R.string.share),
                                filled = true,
                                onClick = {
                                    context.shareText(
                                        "${context.getString(R.string.check_out_this_paperflip_deck)}\n$shareUrl",
                                        deck.title,
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PFTheme.colors.accentSoft, RoundedCornerShape(PFRadius.md))
                        .padding(PFTheme.spacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = PFIcons.Info,
                        contentDescription = null,
                        tint = PFTheme.colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(PFTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.toggle_on_to_generate_a_public_link_pro_users),
                        style = PFTheme.type.footnote,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (filled) PFTheme.colors.accent else PFTheme.colors.accentSoft
    val foreground = if (filled) PFTheme.colors.onAccent else PFTheme.colors.accent
    Row(
        modifier = Modifier
            .background(background, CircleShape)
            .pfPressable(onClick = onClick, pressedScale = 0.95f)
            .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(text = label, style = PFTheme.type.bodyEmphasis, color = foreground)
    }
}
