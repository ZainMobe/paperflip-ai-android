package com.wapp.paperflipai.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.GenerationRequest
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFDeckCardSkeleton
import com.wapp.paperflipai.designsystem.component.PFIndeterminateRing
import com.wapp.paperflipai.designsystem.component.PFLottie
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The wait while the AI builds the deck — the Android counterpart of
 * `GeneratingDeckView.swift`. Designed to feel intentional: the brand
 * animation, a step list that checks itself off on a timer, and shimmering
 * skeleton cards underneath.
 */
@Composable
fun GeneratingDeckContent(
    request: GenerationRequest,
    onGenerated: (DeckDto) -> Unit,
    onCancel: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val haptics = rememberPFHaptics()

    var stepIndex by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    val steps = listOf(
        stringResource(R.string.reading_your_source),
        stringResource(R.string.identifying_key_concepts),
        stringResource(R.string.crafting_questions),
        stringResource(R.string.polishing_answers),
    )

    // Step animator — motion to look at while the network call runs.
    LaunchedEffect(attempt) {
        stepIndex = 0
        repeat(steps.size) {
            delay(700)
            stepIndex += 1
        }
    }

    // The real work.
    LaunchedEffect(attempt) {
        error = null
        isComplete = false
        try {
            val userId = env.auth.session.value?.userId.orEmpty()
            val dto = env.remote.generateDeck(request, userId)
            stepIndex = steps.size
            isComplete = true
            haptics.perform(PFHaptic.Success)
            delay(450)
            onGenerated(dto)
        } catch (failure: Exception) {
            error = failure.message
            haptics.perform(PFHaptic.Error)
        }
    }

    val sourceType = PFSourceType.from(request.sourceType)
    val tint: Color = when (sourceType) {
        PFSourceType.Pdf -> PFTheme.colors.accent
        PFSourceType.Youtube -> PFTheme.colors.danger
        PFSourceType.Article -> PFTheme.colors.warning
    }
    val sourceLabel = when (sourceType) {
        PFSourceType.Pdf -> request.sourceRef
        else -> request.sourceRef.substringAfter("://").substringBefore('/')
    }

    PFScreen(
        topBar = {
            PFTopBar(
                title = stringResource(R.string.generating),
                onBack = onCancel,
                navigationIcon = PFIcons.Close,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            // ── Hero ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PFTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
            ) {
                Box(
                    modifier = Modifier.height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isComplete) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(PFTheme.colors.success.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PFIcons.Check,
                                contentDescription = null,
                                tint = PFTheme.colors.success,
                                modifier = Modifier.size(58.dp),
                            )
                        }
                    } else {
                        PFLottie(
                            resId = R.raw.paperflip_ai_cards_lottie,
                            modifier = Modifier.size(220.dp),
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
                ) {
                    Text(
                        text = stringResource(
                            if (isComplete) R.string.done_2 else R.string.building_your_deck
                        ),
                        style = PFTheme.type.title2,
                        color = PFTheme.colors.onSurface,
                    )
                    Text(
                        text = sourceLabel,
                        style = PFTheme.type.callout,
                        color = PFTheme.colors.onSurfaceMuted,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (error != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(PFTheme.colors.danger.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PFIcons.Warning,
                            contentDescription = null,
                            tint = PFTheme.colors.danger,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.that_didn_t_work),
                        style = PFTheme.type.title3,
                        color = PFTheme.colors.onSurface,
                    )
                    Text(
                        text = error.orEmpty(),
                        style = PFTheme.type.callout,
                        color = PFTheme.colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                    PFButton(
                        title = stringResource(R.string.try_again),
                        onClick = { attempt += 1 },
                        size = PFButtonSize.Lg,
                        leadingIcon = PFIcons.Refresh,
                    )
                    PFButton(
                        title = stringResource(R.string.cancel),
                        onClick = onCancel,
                        variant = PFButtonVariant.Ghost,
                    )
                }
            } else {
                // ── Step list ─────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
                        .padding(PFTheme.spacing.lg),
                ) {
                    steps.forEachIndexed { index, label ->
                        val complete = isComplete || index < stepIndex
                        val active = !complete && index == stepIndex
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (complete) PFTheme.colors.success else PFTheme.colors.elevatedHigh,
                                        CircleShape,
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            complete -> Color.Transparent
                                            active -> tint.copy(alpha = 0.7f)
                                            else -> PFTheme.colors.border
                                        },
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    complete -> Icon(
                                        imageVector = PFIcons.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    active -> PFIndeterminateRing(size = 16.dp, lineWidth = 2.dp, tint = tint)
                                    else -> Text(
                                        text = "${index + 1}",
                                        style = PFTheme.type.caption,
                                        color = PFTheme.colors.onSurfaceMuted,
                                    )
                                }
                            }
                            Spacer(Modifier.width(PFTheme.spacing.md))
                            Text(
                                text = label,
                                style = PFTheme.type.body,
                                color = if (complete || active) PFTheme.colors.onSurface
                                else PFTheme.colors.onSurfaceMuted,
                                textDecoration = if (complete) TextDecoration.LineThrough else null,
                            )
                        }
                        if (index < steps.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 13.5.dp)
                                    .width(1.5.dp)
                                    .height(22.dp)
                                    .background(PFTheme.colors.divider)
                            )
                        }
                    }
                }

                // ── Skeleton preview ──────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.preview),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                        repeat(3) { PFDeckCardSkeleton() }
                    }
                }
            }
        }
    }
}
