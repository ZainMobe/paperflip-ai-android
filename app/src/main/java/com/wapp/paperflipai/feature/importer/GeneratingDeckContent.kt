package com.wapp.paperflipai.feature.importer

import androidx.compose.animation.Crossfade
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
import com.wapp.paperflipai.core.data.GenerationJobStatus
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
import com.wapp.paperflipai.feature.library.tint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * The wait while the AI builds the deck — the Android counterpart of
 * `GeneratingDeckView.swift`.
 *
 * Starts an async job on `generate-deck-job` and follows the real job
 * status: "Reading your PDF" while the server fetches the source, "Writing
 * N cards" while the model works, then done. A failure shows the server's
 * own friendly message with Try again. Leaving the screen stops watching;
 * the server finishes the job and the deck is simply not saved.
 */
@Composable
fun GeneratingDeckContent(
    request: GenerationRequest,
    onGenerated: (DeckDto) -> Unit,
    onCancel: () -> Unit,
    /** What to show under the title, e.g. the PDF's file name (sourceRef is its storage path). */
    sourceLabelOverride: String? = null,
) {
    val env = LocalAppEnvironment.current
    val haptics = rememberPFHaptics()
    var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    val cancel = {
        PFAnalytics.log(AnalyticsEvent.generationCancelled(request.sourceType, PFAnalytics.secondsSince(startedAt)))
        onCancel()
    }

    var stepIndex by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var flavourIndex by remember { mutableIntStateOf(0) }

    val sourceType = PFSourceType.from(request.sourceType)
    val steps = listOf(
        stringResource(
            when (sourceType) {
                PFSourceType.Pdf -> R.string.gen_step_reading_pdf
                PFSourceType.Youtube -> R.string.gen_step_getting_transcript
                PFSourceType.Article -> R.string.gen_step_fetching_article
                PFSourceType.Text -> R.string.gen_step_reading_notes
                PFSourceType.Photo -> R.string.gen_step_reading_scan
                PFSourceType.Audio -> R.string.gen_step_reading_transcript
            }
        ),
        if (request.autoCardCount) stringResource(R.string.gen_step_writing_your_cards)
        else stringResource(R.string.gen_step_writing_n_cards, request.targetCardCount),
        stringResource(R.string.gen_step_checking_cards),
    )
    val flavourLines = listOf(
        stringResource(R.string.gen_flavour_testable_points),
        stringResource(R.string.gen_flavour_short_answers),
        stringResource(R.string.gen_flavour_stands_alone),
    )
    val fallbackError = stringResource(R.string.gen_error_generic)
    val emptyDeckError = stringResource(R.string.gen_error_empty_deck)

    // Rotating line under the title while the model writes.
    LaunchedEffect(attempt) {
        flavourIndex = 0
        while (true) {
            delay(6_000)
            flavourIndex += 1
        }
    }

    // The real work: start a job, then follow its status.
    LaunchedEffect(attempt) {
        error = null
        isComplete = false
        stepIndex = 0
        startedAt = System.currentTimeMillis()
        PFAnalytics.log(AnalyticsEvent.generationStarted(request.sourceType, request.targetCardCount, request.autoCardCount))
        try {
            val jobId = env.remote.startGenerationJob(request)
            var finished = false
            env.remote.watchGenerationJob(jobId).collect { job ->
                when (job.status) {
                    GenerationJobStatus.Queued, GenerationJobStatus.Fetching -> stepIndex = 0
                    GenerationJobStatus.Generating -> stepIndex = maxOf(stepIndex, 1)
                    GenerationJobStatus.Done -> {
                        finished = true
                        val dto = job.deck
                        if (dto == null) {
                            error = emptyDeckError
                            PFAnalytics.log(AnalyticsEvent.generationFailed(request.sourceType, "empty_result", emptyDeckError, PFAnalytics.secondsSince(startedAt)))
                            haptics.perform(PFHaptic.Error)
                        } else {
                            stepIndex = steps.size
                            isComplete = true
                            PFAnalytics.log(AnalyticsEvent.generationCompleted(request.sourceType, dto.cards.size, PFAnalytics.secondsSince(startedAt)))
                            haptics.perform(PFHaptic.Success)
                            delay(550)
                            onGenerated(dto)
                        }
                    }
                    GenerationJobStatus.Failed -> {
                        finished = true
                        error = job.errorMessage ?: fallbackError
                        PFAnalytics.log(
                            AnalyticsEvent.generationFailed(
                                request.sourceType,
                                job.errorCode ?: "failed",
                                job.errorMessage ?: "none",
                                PFAnalytics.secondsSince(startedAt),
                            )
                        )
                        haptics.perform(PFHaptic.Error)
                    }
                }
            }
            if (!finished) {
                error = fallbackError
                PFAnalytics.log(AnalyticsEvent.generationFailed(request.sourceType, "stream_ended", fallbackError, PFAnalytics.secondsSince(startedAt)))
                haptics.perform(PFHaptic.Error)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message?.takeIf { it.isNotBlank() } ?: fallbackError
            PFAnalytics.log(
                AnalyticsEvent.generationFailed(
                    request.sourceType,
                    failure::class.java.simpleName,
                    failure.message ?: "none",
                    PFAnalytics.secondsSince(startedAt),
                )
            )
            PFAnalytics.error(failure, "generation", mapOf("source_type" to request.sourceType))
            haptics.perform(PFHaptic.Error)
        }
    }

    val tint: Color = sourceType.tint()
    val sourceLabel = sourceLabelOverride ?: when (sourceType) {
        PFSourceType.Pdf -> request.sourceRef.substringAfterLast('/')
        PFSourceType.Youtube, PFSourceType.Article ->
            request.sourceRef.substringAfter("://").substringBefore('/')
        PFSourceType.Text -> stringResource(R.string.source_label_pasted_notes)
        PFSourceType.Photo -> stringResource(R.string.source_label_photo_scan)
        PFSourceType.Audio -> stringResource(R.string.source_label_lecture)
    }

    PFScreen(
        topBar = {
            PFTopBar(
                title = stringResource(R.string.generating),
                onBack = cancel,
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
                    if (!isComplete && error == null && stepIndex >= 1) {
                        Crossfade(targetState = flavourIndex % flavourLines.size, label = "flavour") { i ->
                            Text(
                                text = flavourLines[i],
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.onSurfaceFaint,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
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
                        text = stringResource(R.string.gen_error_title),
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
                        onClick = cancel,
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
