package com.wapp.paperflipai.feature.paywall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.PFLinks
import com.wapp.paperflipai.core.monetization.PaywallProduct
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFIconButton
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import com.wapp.paperflipai.util.openUrl
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The upgrade screen — the Android counterpart of `PaywallView.swift`.
 * Reads like a polished store page: brand hero, scannable feature list,
 * two-plan comparison with the annual plan pre-selected and its savings
 * badge, one big CTA, restore link, legal footer.
 */
private data class PaywallFeature(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)

private val paywallFeatures = listOf(
    PaywallFeature(PFIcons.Sparkle, R.string.unlimited_decks, R.string.generate_as_many_as_you_want_no_monthly_cap),
    PaywallFeature(PFIcons.Deck, R.string.up_to_50_cards_per_deck, R.string.get_the_full_picture_of_any_source),
    PaywallFeature(PFIcons.Study, R.string.spaced_repetition, R.string.sm_2_algorithm_schedules_each_card_for_the_rig),
    PaywallFeature(PFIcons.Grid, R.string.home_screen_widgets, R.string.widgets_blurb_android),
    PaywallFeature(PFIcons.Share, R.string.export_to_anki_csv, R.string.your_data_stays_yours),
)

@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    onPurchased: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val products by env.entitlements.availableProducts.collectAsStateWithLifecycle()
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()

    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var isPurchasing by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    val restoredLabel = stringResource(R.string.purchases_restored)
    val nothingToRestoreLabel = stringResource(R.string.nothing_to_restore)
    val couldntRestoreLabel = stringResource(R.string.couldnt_restore)
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(products) {
        if (selectedProductId == null) {
            selectedProductId = products.firstOrNull { it.cadence == PaywallProduct.Cadence.Annual }?.id
                ?: products.firstOrNull()?.id
        }
    }

    val selected = products.firstOrNull { it.id == selectedProductId }
    val monthlyCents = products.firstOrNull { it.cadence == PaywallProduct.Cadence.Monthly }?.monthlyEquivCents

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to PFTheme.colors.accentSoft,
                    0.5f to PFTheme.colors.surface,
                    1f to PFTheme.colors.surface,
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(top = PFTheme.spacing.xxl, bottom = PFTheme.spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            // ── Hero ──────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(96.dp)
                        .pfElevation(PFElevation.Card, CircleShape),
                )
                PFTagPill(
                    title = stringResource(R.string.paperflip_pro),
                    icon = PFIcons.Sparkle,
                    prominent = true,
                )
                Text(
                    text = stringResource(R.string.learn_faster_generate_freely),
                    style = PFTheme.type.title1,
                    color = PFTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.unlock_unlimited_decks_larger_card_counts_and),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = PFTheme.spacing.md),
                )
            }

            // ── Features ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                paywallFeatures.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PFTheme.colors.accentSoft, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = PFTheme.colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(PFTheme.spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(feature.titleRes),
                                style = PFTheme.type.bodyEmphasis,
                                color = PFTheme.colors.onSurface,
                            )
                            Text(
                                text = stringResource(feature.bodyRes),
                                style = PFTheme.type.footnote,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                        }
                    }
                }
            }

            // ── Plans ─────────────────────────────────────────────────
            if (products.isEmpty()) {
                Text(
                    text = stringResource(R.string.subscription_plans_couldn_t_be_loaded),
                    style = PFTheme.type.callout,
                    color = PFTheme.colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    products.forEach { product ->
                        val savings = if (
                            product.cadence == PaywallProduct.Cadence.Annual && monthlyCents != null
                        ) {
                            val percent = (
                                (monthlyCents - product.monthlyEquivCents) * 100f / monthlyCents
                                ).roundToInt()
                            if (percent > 0) percent else null
                        } else {
                            null
                        }
                        PlanCard(
                            product = product,
                            savingsPercent = savings,
                            selected = selectedProductId == product.id,
                            onClick = { selectedProductId = product.id },
                        )
                    }
                }
            }

            PFErrorBanner(error)

            if (products.isNotEmpty()) {
                PFButton(
                    title = selected?.let {
                        stringResource(R.string.start_pro_2, it.priceLabel)
                    } ?: stringResource(R.string.start_pro),
                    onClick = {
                        val product = selected
                        if (product != null) scope.launch {
                            isPurchasing = true
                            error = null
                            try {
                                env.entitlements.purchase(product)
                                onPurchased()
                            } catch (failure: Exception) {
                                error = failure.message
                            } finally {
                                isPurchasing = false
                            }
                        }
                    },
                    size = PFButtonSize.Lg,
                    trailingIcon = PFIcons.Forward,
                    isLoading = isPurchasing,
                    enabled = selected != null && !isPurchasing,
                )
            }

            PFButton(
                title = stringResource(if (isRestoring) R.string.restoring else R.string.restore_purchases),
                onClick = {
                    scope.launch {
                        isRestoring = true
                        val result = runCatching { env.entitlements.restorePurchases() }
                        isRestoring = false
                        // Silence here is the worst outcome: a subscriber who
                        // reinstalled cannot tell "no purchase found" apart from
                        // "the network failed".
                        result.fold(
                            onSuccess = {
                                if (env.entitlements.entitlement.value.isActive) {
                                    PFToast.success(restoredLabel)
                                } else {
                                    PFToast.info(nothingToRestoreLabel)
                                }
                            },
                            onFailure = { PFToast.error(couldntRestoreLabel, it.message) },
                        )
                    }
                },
                variant = PFButtonVariant.Ghost,
                enabled = !isRestoring,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.billing_note_android),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.terms),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.accent,
                        modifier = Modifier.pfPressable(
                            onClick = { context.openUrl(PFLinks.TERMS) },
                            pressedScale = 0.94f,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.privacy_policy),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.accent,
                        modifier = Modifier.pfPressable(
                            onClick = { context.openUrl(PFLinks.PRIVACY) },
                            pressedScale = 0.94f,
                        ),
                    )
                }
            }
        }

        PFIconButton(
            icon = PFIcons.Close,
            contentDescription = stringResource(R.string.nav_close),
            onClick = onClose,
            tint = PFTheme.colors.onSurfaceMuted,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(PFTheme.spacing.md)
                .background(PFTheme.colors.elevated, CircleShape),
        )
    }
}

@Composable
private fun PlanCard(
    product: PaywallProduct,
    savingsPercent: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(PFRadius.xl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) PFTheme.colors.accentSoft else PFTheme.colors.elevated, shape)
            .border(
                width = if (selected) 1.5.dp else 0.7.dp,
                color = if (selected) PFTheme.colors.accent else PFTheme.colors.border,
                shape = shape,
            )
            .pfPressable(onClick = onClick, haptic = PFHaptic.Selection, pressedScale = 0.98f)
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    if (product.cadence == PaywallProduct.Cadence.Annual) R.string.annual
                    else R.string.monthly
                ),
                style = PFTheme.type.bodyEmphasis,
                color = PFTheme.colors.onSurface,
            )
            savingsPercent?.let {
                Spacer(Modifier.width(PFTheme.spacing.sm))
                PFTagPill(
                    title = stringResource(R.string.save, it),
                    tint = PFTheme.colors.success,
                    prominent = true,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (selected) PFTheme.colors.accent else PFTheme.colors.elevatedHigh,
                        CircleShape,
                    )
                    .border(
                        1.5.dp,
                        if (selected) PFTheme.colors.accent else PFTheme.colors.border,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = PFIcons.Check,
                        contentDescription = null,
                        tint = PFTheme.colors.onAccent,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = product.priceLabel,
                style = PFTheme.type.title2,
                color = PFTheme.colors.onSurface,
            )
            Spacer(Modifier.width(PFTheme.spacing.xs))
            Text(
                text = stringResource(
                    if (product.cadence == PaywallProduct.Cadence.Annual) R.string.year
                    else R.string.month
                ),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )
            Spacer(Modifier.weight(1f))
            if (product.cadence == PaywallProduct.Cadence.Annual) {
                Text(
                    text = product.monthlyEquivLabel,
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }

        product.introLabel?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PFIcons.Sparkle,
                    contentDescription = null,
                    tint = PFTheme.colors.accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.xs))
                Text(text = it, style = PFTheme.type.footnote, color = PFTheme.colors.accent)
            }
        }
    }
}
