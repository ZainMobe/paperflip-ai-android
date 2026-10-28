package com.wapp.paperflipai.feature.quiz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.data.QuizMode
import com.wapp.paperflipai.core.quiz.QuizAnswer
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFProgressRing
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlin.math.roundToInt

/**
 * Score ring, a one-line verdict, the questions missed with the right
 * answer, and the next step. Counterpart of `QuizResultsView.swift`.
 */
@Composable
internal fun QuizResultsContent(
    configuration: QuizConfiguration,
    answers: List<QuizAnswer>,
    retryRound: Int,
    onRetryMissed: () -> Unit,
    onDone: () -> Unit,
) {
    val total = answers.size
    val correct = answers.count { it.wasCorrect }
    val percent = if (total == 0) 0 else (correct * 100f / total).roundToInt()
    val missed = answers.filterNot { it.wasCorrect }
    val isTest = configuration.mode == QuizMode.Test

    val ring = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        ring.animateTo(percent / 100f, spring(dampingRatio = 0.8f, stiffness = 60f))
    }
    val tint = when {
        percent >= 80 -> PFTheme.colors.success
        percent >= 50 -> PFTheme.colors.warning
        else -> PFTheme.colors.danger
    }
    val verdictTitle = stringResource(
        when {
            retryRound > 0 && missed.isEmpty() -> R.string.quiz_verdict_cleared
            percent == 100 -> R.string.quiz_verdict_perfect
            percent >= 80 -> R.string.quiz_verdict_strong
            percent >= 50 -> R.string.quiz_verdict_getting_there
            else -> R.string.quiz_verdict_another_pass
        }
    )
    val verdictBody = when {
        missed.isEmpty() -> stringResource(R.string.quiz_verdict_body_all)
        isTest -> stringResource(R.string.quiz_verdict_body_test, missed.size)
        else -> stringResource(R.string.quiz_verdict_body_quiz, missed.size)
    }
    val scoreDescription = stringResource(R.string.quiz_a11y_score, percent, correct, total)

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(top = PFTheme.spacing.lg, bottom = PFTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            val shape = RoundedCornerShape(PFRadius.xxl)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pfElevation(PFElevation.Card, shape)
                    .background(PFTheme.colors.paper, shape)
                    .border(0.7.dp, PFTheme.colors.border, shape)
                    .padding(PFTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = scoreDescription },
                ) {
                    PFProgressRing(
                        progress = ring.value,
                        size = 148.dp,
                        lineWidth = 12.dp,
                        tint = tint,
                        showsLabel = false,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$percent%", style = PFTheme.type.display, color = PFTheme.colors.onSurface)
                        Text(
                            text = stringResource(R.string.quiz_n_of_m, correct, total),
                            style = PFTheme.type.footnote,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
                Text(verdictTitle, style = PFTheme.type.title2, color = PFTheme.colors.onSurface, textAlign = TextAlign.Center)
                Text(verdictBody, style = PFTheme.type.callout, color = PFTheme.colors.onSurfaceMuted, textAlign = TextAlign.Center)
                if (isTest) {
                    Row(Modifier.fillMaxWidth()) {
                        Stat(PFIcons.List, total.toString(), stringResource(R.string.quiz_questions), Modifier.weight(1f))
                        configuration.timeLimitSeconds?.let {
                            Stat(
                                PFIcons.Timer,
                                stringResource(R.string.quiz_n_min, it / 60),
                                stringResource(R.string.quiz_time_limit),
                                Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (missed.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.quiz_missed),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                    missed.forEach { MissedRow(it) }
                }
            }
        }

        Column(
            modifier = Modifier
                .background(PFTheme.colors.surface)
                .navigationBarsPadding()
                .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
        ) {
            if (!isTest && missed.isNotEmpty()) {
                PFButton(
                    title = stringResource(R.string.quiz_retry_missed, missed.size),
                    onClick = onRetryMissed,
                    size = PFButtonSize.Lg,
                    leadingIcon = PFIcons.Refresh,
                )
                PFButton(title = stringResource(R.string.done), onClick = onDone, variant = PFButtonVariant.Ghost)
            } else {
                PFButton(
                    title = stringResource(R.string.done),
                    onClick = onDone,
                    size = PFButtonSize.Lg,
                    trailingIcon = PFIcons.Check,
                )
            }
        }
    }
}

@Composable
private fun Stat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = PFTheme.colors.accent, modifier = Modifier.size(18.dp))
        Text(value, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
        Text(label, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun MissedRow(answer: QuizAnswer) {
    val shape = RoundedCornerShape(PFRadius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Text(answer.question.prompt, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface)
        Row(verticalAlignment = Alignment.Top) {
            Icon(PFIcons.CheckCircle, null, tint = PFTheme.colors.success, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(PFTheme.spacing.sm))
            Text(answer.question.correctAnswer, style = PFTheme.type.footnote, color = PFTheme.colors.onSurface)
        }
        Row(verticalAlignment = Alignment.Top) {
            Icon(PFIcons.Error, null, tint = PFTheme.colors.danger, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(PFTheme.spacing.sm))
            Text(
                answer.picked,
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurfaceMuted,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}
