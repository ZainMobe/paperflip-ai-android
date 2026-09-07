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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.BuildConfig
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.app.Route
import com.wapp.paperflipai.core.PFLinks
import com.wapp.paperflipai.core.monetization.Entitlement
import com.wapp.paperflipai.core.monetization.MockEntitlementStore
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFGroupedCard
import com.wapp.paperflipai.designsystem.component.PFListRow
import com.wapp.paperflipai.designsystem.component.PFRowDivider
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSectionHeader
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.component.tabBarContentPadding
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFAppearance
import com.wapp.paperflipai.designsystem.theme.PFLanguage
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.util.openUrl

/**
 * Tab 5 — account, subscription, app preferences, about and sign-out. The
 * Android counterpart of `SettingsHomeView.swift`, rendered as inset
 * grouped cards rather than a UIKit-style List.
 */
@Composable
fun SettingsHomeScreen(onNavigate: (String) -> Unit) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current

    val session by env.auth.session.collectAsStateWithLifecycle()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()
    val appearance by env.settings.appearance.collectAsStateWithLifecycle()
    val language by env.settings.language.collectAsStateWithLifecycle()

    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    val signOutTitle = stringResource(R.string.sign_out_of_paperflip)
    val signOutMessage = stringResource(R.string.your_decks_stay_synced_you_can_sign_back_in_an)
    val signOutConfirm = stringResource(R.string.sign_out)
    val cancelLabel = stringResource(R.string.cancel)

    PFScreen(topBar = { PFTopBar(title = stringResource(R.string.settings)) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = tabBarContentPadding(PFTheme.spacing.lg)),
        ) {
            // ── Account ───────────────────────────────────────────────
            PFGroupedCard(modifier = Modifier.padding(top = PFTheme.spacing.md)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pfPressable(
                            onClick = { onNavigate(Route.SETTINGS_PROFILE) },
                            pressedScale = 0.99f,
                        )
                        .padding(PFTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(PFTheme.colors.accentSoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = session?.initials ?: "P",
                            style = PFTheme.type.title3,
                            color = PFTheme.colors.accent,
                        )
                    }
                    Spacer(Modifier.width(PFTheme.spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = session?.displayName ?: "PaperFlip",
                            style = PFTheme.type.headline,
                            color = PFTheme.colors.onSurface,
                        )
                        Text(
                            text = session?.email ?: "—",
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                    Text(
                        text = stringResource(R.string.edit),
                        style = PFTheme.type.footnoteBold,
                        color = PFTheme.colors.accent,
                    )
                }
            }

            // ── Subscription ──────────────────────────────────────────
            PFSectionHeader(title = stringResource(R.string.subscription))
            PFGroupedCard {
                PFListRow(
                    title = stringResource(entitlement.displayNameRes),
                    icon = if (entitlement.isActive) PFIcons.Pro else PFIcons.Sparkle,
                    showChevron = false,
                    onClick = {
                        if (entitlement.isActive) onNavigate(Route.SETTINGS_SUBSCRIPTION)
                        else onNavigate(Route.PAYWALL)
                    },
                    trailing = {
                        PFTagPill(
                            title = stringResource(
                                if (entitlement.isActive) R.string.pro else R.string.start_pro
                            ),
                            icon = if (entitlement.isActive) PFIcons.CheckCircle else PFIcons.Trending,
                            tint = if (entitlement.isActive) PFTheme.colors.success else PFTheme.colors.accent,
                            prominent = true,
                        )
                    },
                )
            }

            // ── App ───────────────────────────────────────────────────
            PFSectionHeader(title = stringResource(R.string.app))
            PFGroupedCard {
                if (entitlement.isActive) {
                    PFListRow(
                        title = stringResource(R.string.project_invites),
                        icon = PFIcons.PersonAdd,
                        onClick = { onNavigate(Route.PENDING_INVITES) },
                    )
                    PFRowDivider()
                }
                PFListRow(
                    title = stringResource(R.string.notifications),
                    icon = PFIcons.Notifications,
                    onClick = { onNavigate(Route.SETTINGS_NOTIFICATIONS) },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.appearance),
                    icon = PFIcons.Appearance,
                    onClick = { onNavigate(Route.SETTINGS_APPEARANCE) },
                    trailing = {
                        Text(
                            text = stringResource(appearance.displayNameRes),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.language),
                    icon = PFIcons.Language,
                    onClick = { onNavigate(Route.SETTINGS_LANGUAGE) },
                    trailing = {
                        Text(
                            text = if (language == PFLanguage.System)
                                stringResource(R.string.system) else language.displayName,
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    },
                )
            }

            // ── About ─────────────────────────────────────────────────
            PFSectionHeader(title = stringResource(R.string.about))
            PFGroupedCard {
                PFListRow(
                    title = stringResource(R.string.help_support),
                    icon = PFIcons.Support,
                    onClick = { onNavigate(Route.SUPPORT) },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.terms_of_service),
                    icon = PFIcons.Document,
                    showChevron = false,
                    onClick = { context.openUrl(PFLinks.TERMS) },
                    trailing = {
                        Icon(
                            imageVector = PFIcons.OpenExternal,
                            contentDescription = null,
                            tint = PFTheme.colors.onSurfaceFaint,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.privacy_policy),
                    icon = PFIcons.Password,
                    showChevron = false,
                    onClick = { context.openUrl(PFLinks.PRIVACY) },
                    trailing = {
                        Icon(
                            imageVector = PFIcons.OpenExternal,
                            contentDescription = null,
                            tint = PFTheme.colors.onSurfaceFaint,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.paperflip_ai),
                    icon = PFIcons.Public,
                    showChevron = false,
                    onClick = { context.openUrl(PFLinks.WEBSITE) },
                )
                PFRowDivider()
                PFListRow(
                    title = stringResource(R.string.version),
                    icon = PFIcons.Info,
                    showChevron = false,
                    trailing = {
                        Text(
                            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    },
                )
            }

            // ── Developer (debug builds only) ─────────────────────────
            if (BuildConfig.DEBUG) {
                PFSectionHeader(title = "Developer")
                PFGroupedCard {
                    PFListRow(
                        title = if (entitlement.isActive) "Toggle: turn Pro OFF" else "Toggle: turn Pro ON",
                        icon = PFIcons.Pro,
                        showChevron = false,
                        onClick = {
                            (env.entitlements as? MockEntitlementStore)?.set(
                                if (entitlement.isActive) Entitlement.Free else Entitlement.Pro
                            )
                        },
                    )
                    PFRowDivider()
                    PFListRow(
                        title = "Reset onboarding",
                        icon = PFIcons.Refresh,
                        showChevron = false,
                        onClick = { env.settings.resetOnboarding() },
                    )
                }
            }

            // ── Sign out ──────────────────────────────────────────────
            PFSectionHeader(title = "")
            PFGroupedCard(modifier = Modifier.padding(bottom = PFTheme.spacing.lg)) {
                PFListRow(
                    title = stringResource(R.string.sign_out),
                    icon = PFIcons.SignOut,
                    iconTint = PFTheme.colors.danger,
                    titleColor = PFTheme.colors.danger,
                    showChevron = false,
                    onClick = {
                        confirmation = PFConfirmation(
                            title = signOutTitle,
                            message = signOutMessage,
                            icon = PFIcons.SignOut,
                            confirmTitle = signOutConfirm,
                            cancelTitle = cancelLabel,
                            destructive = true,
                        ) {
                            env.auth.signOut()
                            env.onSignedOut()
                        }
                    },
                )
            }
        }
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

/** Display name for the entitlement, using the shared string catalogue. */
val Entitlement.displayNameRes: Int
    get() = when (this) {
        Entitlement.Free -> R.string.free
        Entitlement.Pro -> R.string.pro_monthly
        Entitlement.Annual -> R.string.pro_annual
        Entitlement.Trialing -> R.string.pro_trial
    }

/** Display name for the appearance mode. */
val PFAppearance.displayNameRes: Int
    get() = when (this) {
        PFAppearance.System -> R.string.system
        PFAppearance.Light -> R.string.light
        PFAppearance.Dark -> R.string.dark
    }
