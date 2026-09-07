package com.wapp.paperflipai.feature.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFCard
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.openPlaySubscriptions
import kotlinx.coroutines.launch

/**
 * What Pro users see from the Subscription row — the Android counterpart of
 * `ManageSubscriptionView.swift`. Current plan, a deep link into Google
 * Play (only Play can cancel a Play subscription), and Restore purchases.
 */
@Composable
fun ManageSubscriptionScreen(
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()
    var isRestoring by remember { mutableStateOf(false) }
    val restoredLabel = stringResource(R.string.restore_purchases)

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.subscription_2), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            PFCard(elevation = PFElevation.Card, surface = PFTheme.colors.paper) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(PFTheme.colors.accentSoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PFIcons.Pro,
                            contentDescription = null,
                            tint = PFTheme.colors.accent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(PFTheme.spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.you_re_on_paperflip_pro),
                            style = PFTheme.type.title3,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = stringResource(entitlement.displayNameRes),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                    PFTagPill(title = stringResource(R.string.pro), prominent = true)
                }
            }

            PFGroupedCard {
                PFListRow(
                    title = stringResource(R.string.unlimited_deck_generation),
                    icon = PFIcons.Sparkle,
                    showChevron = false,
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.up_to_50_cards_per_deck),
                    icon = PFIcons.Card,
                    showChevron = false,
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.project_workspaces),
                    icon = PFIcons.Group,
                    showChevron = false,
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.public_deck_sharing),
                    icon = PFIcons.Public,
                    showChevron = false,
                )
            }

            PFGroupedCard {
                PFListRow(
                    title = stringResource(R.string.manage_in_play_store),
                    subtitle = stringResource(R.string.change_plan_update_payment_or_cancel),
                    icon = PFIcons.OpenExternal,
                    onClick = { context.openPlaySubscriptions(context.packageName) },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(
                        if (isRestoring) R.string.restoring else R.string.restore_purchases
                    ),
                    icon = PFIcons.Refresh,
                    showChevron = false,
                    enabled = !isRestoring,
                    onClick = {
                        scope.launch {
                            isRestoring = true
                            runCatching { env.entitlements.restorePurchases() }
                            isRestoring = false
                            PFToast.info(restoredLabel)
                        }
                    },
                )
            }

            Text(
                text = stringResource(R.string.billing_note_android),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceFaint,
            )
        }
    }
}
