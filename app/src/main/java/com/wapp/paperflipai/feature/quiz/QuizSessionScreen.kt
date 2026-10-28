package com.wapp.paperflipai.feature.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFQuizResult
import com.wapp.paperflipai.core.data.QuizMode
import com.wapp.paperflipai.core.quiz.QuizAnswer
import com.wapp.paperflipai.core.quiz.QuizEngine
import com.wapp.paperflipai.core.quiz.QuizQuestion
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFConfirmation
import com.wapp.paperflipai.designsystem.component.PFConfirmationHost
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTagPill
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFElevation
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import com.wapp.paperflipai.designsystem.theme.pfElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics
import com.wapp.paperflipai.core.analytics.TrackScreen
import androidx.compose.runtime.DisposableEffect

/** What kind of round to run. Null fields mean "every card" / "untimed". */
data class QuizConfiguration(
    val mode: QuizMode,
    val questionCount: Int? = null,
    val timeLimitSeconds: Int? = null,
) {
    companion object {
        val Quiz = QuizConfiguration(QuizMode.Quiz)
    }
}

/**
 * Quiz mode and practice tests on one screen — the Android counterpart of
 * `QuizSessionView.swift`. Questions come from [QuizEngine] (deck cards
 * only, no network).
 *
 *  - Quiz: feedback after each answer, Continue to move on, and a "retry
 *    the ones you missed" round at the end.
 *  - Test: a fixed number of questions, optional countdown, no feedback
 *    until the results screen.
 */
@Composable
fun QuizSessionScreen(
    deckId: String,
    configuration: QuizConfiguration,
    onFinish: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val haptics = rememberPFHaptics()
    val scope = rememberCoroutineScope()

    val deck = remember(deckId) { env.database.deck(deckId) }
    val seeds = remember(deckId) {
        env.database.cards(deckId).map { QuizEngine.CardSeed(it.id, it.front, it.back) }
    }

    var questions by remember { mutableStateOf(emptyList<QuizQuestion>()) }
    var index by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf(emptyList<QuizAnswer>()) }
    var selection by remember { mutableStateOf<Int?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var complete by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var secondsLeft by remember { mutableStateOf<Int?>(null) }
    var retryRound by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    var confirmation by remember { mutableStateOf<PFConfirmation?>(null) }

    val isTest = configuration.mode == QuizMode.Test

    fun start(wrongFirst: Set<String> = emptySet()) {
        val built = if (wrongFirst.isEmpty()) {
            QuizEngine.makeQuestions(seeds, configuration.questionCount)
        } else {
            // Distractors still come from the whole deck; keep only the missed cards.
            QuizEngine.makeQuestions(seeds, wrongFirst = wrongFirst).filter { it.cardId in wrongFirst }
        }
        questions = if (built.size >= 2 || wrongFirst.isNotEmpty()) built else emptyList()
        index = 0
        answers = emptyList()
        selection = null
        revealed = false
        complete = false
        startedAt = System.currentTimeMillis()
        secondsLeft = if (isTest) configuration.timeLimitSeconds else null
        round += 1
        if (questions.isNotEmpty()) PFAnalytics.log(AnalyticsEvent.quizStarted(configuration.mode.raw, questions.size))
    }

    fun finish() {
        if (complete) return
        val correct = answers.count { it.wasCorrect }
        val result = PFQuizResult(
            deckId = deckId,
            mode = configuration.mode,
            startedAt = startedAt,
            questionCount = questions.size,
            correctCount = correct,
            timeLimitSeconds = configuration.timeLimitSeconds,
        )
        env.repository.saveQuizResult(result)
        PFAnalytics.log(
            AnalyticsEvent.quizCompleted(configuration.mode.raw, questions.size, result.scorePercent, PFAnalytics.secondsSince(startedAt))
        )
        if (result.scorePercent >= 80) env.reviewPrompts.recordSuccess()
        haptics.perform(PFHaptic.Success)
        complete = true
    }

    fun advance() {
        if (index + 1 >= questions.size) {
            finish()
        } else {
            index += 1
            selection = null
            revealed = false
        }
    }

    fun choose(option: Int, isCorrect: Boolean, picked: String) {
        if (selection != null || complete) return
        val question = questions.getOrNull(index) ?: return
        selection = option
        answers = answers + QuizAnswer(question, isCorrect, picked)
        if (isTest) {
            haptics.perform(PFHaptic.Selection)
            scope.launch {
                delay(350)
                advance()
            }
        } else {
            revealed = true
            haptics.perform(if (isCorrect) PFHaptic.Success else PFHaptic.Error)
        }
    }

    LaunchedEffect(Unit) { start() }

    // Countdown for timed tests; time running out submits what's there.
    LaunchedEffect(round, complete) {
        if (!isTest || complete) return@LaunchedEffect
        while (true) {
            val left = secondsLeft ?: return@LaunchedEffect
            if (left <= 0) {
                haptics.perform(PFHaptic.Warning)
                finish()
                return@LaunchedEffect
            }
            delay(1_000)
            secondsLeft = left - 1
        }
    }

    val leaveTitle = stringResource(if (isTest) R.string.quiz_leave_test else R.string.quiz_leave_quiz)
    val leaveMessage = stringResource(R.string.quiz_leave_message)
    val leaveConfirm = stringResource(R.string.quiz_leave_confirm)
    val keepGoing = stringResource(R.string.quiz_keep_going)
    fun requestExit() {
        if (complete || answers.isEmpty()) {
            onFinish()
        } else {
            confirmation = PFConfirmation(
                title = leaveTitle,
                message = leaveMessage,
                confirmTitle = leaveConfirm,
                cancelTitle = keepGoing,
                destructive = true,
                onConfirm = { onFinish() },
            )
        }
    }
    BackHandler { requestExit() }

    DisposableEffect(Unit) {
        onDispose {
            if (!complete && questions.isNotEmpty()) {
                PFAnalytics.log(AnalyticsEvent.quizAbandoned(configuration.mode.raw, answers.size, questions.size))
            }
        }
    }
    if (complete) TrackScreen("quiz_results", "QuizResultsContent")

    PFScreen(
        topBar = {
            PFTopBar(
                title = when {
                    complete -> deck?.title.orEmpty()
                    isTest -> stringResource(R.string.quiz_practice_test)
                    else -> stringResource(R.string.quiz_title)
                },
                onBack = { requestExit() },
                navigationIcon = PFIcons.Close,
                actions = {
                    val left = secondsLeft
                    if (left != null && !complete) TimerPill(left)
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                deck == null || (questions.isEmpty() && round > 0) -> PFEmptyState(
                    icon = PFIcons.Quiz,
                    title = stringResource(R.string.quiz_not_enough_title),
                    message = stringResource(R.string.quiz_not_enough_message),
                    ctaTitle = stringResource(R.string.done),
                    onCta = onFinish,
                )
                complete -> QuizResultsContent(
                    configuration = configuration,
                    answers = answers,
                    retryRound = retryRound,
                    onRetryMissed = {
                        val missed = answers.filterNot { it.wasCorrect }.map { it.question.cardId }.toSet()
                        retryRound += 1
                        start(wrongFirst = missed)
                    },
                    onDone = onFinish,
                )
                questions.isNotEmpty() -> QuestionContent(
                    question = questions[index],
                    index = index,
                    total = questions.size,
                    correctSoFar = answers.count { it.wasCorrect },
                    showsRunningScore = !isTest,
                    selection = selection,
                    revealed = revealed,
                    isTest = isTest,
                    lastAnswerCorrect = answers.lastOrNull()?.wasCorrect,
                    onChoose = ::choose,
                    onContinue = ::advance,
                )
            }
        }
    }

    PFConfirmationHost(confirmation) { confirmation = null }
}

// ── Question ─────────────────────────────────────────────────────────

private enum class OptionState { Idle, Selected, Correct, Wrong, Dimmed }

@Composable
private fun QuestionContent(
    question: QuizQuestion,
    index: Int,
    total: Int,
    correctSoFar: Int,
    showsRunningScore: Boolean,
    selection: Int?,
    revealed: Boolean,
    isTest: Boolean,
    lastAnswerCorrect: Boolean?,
    onChoose: (option: Int, isCorrect: Boolean, picked: String) -> Unit,
    onContinue: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = (index + if (revealed) 1 else 0).toFloat() / total,
        animationSpec = PFMotion.smooth(),
        label = "quizProgress",
    )

    fun state(option: Int, correctIndex: Int): OptionState = when {
        selection == null -> OptionState.Idle
        isTest || !revealed -> if (option == selection) OptionState.Selected else OptionState.Idle
        option == correctIndex -> OptionState.Correct
        option == selection -> OptionState.Wrong
        else -> OptionState.Dimmed
    }

    Column(Modifier.fillMaxSize()) {
        // Progress
        Column(
            modifier = Modifier.padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(PFTheme.colors.divider, CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(PFTheme.colors.accent, CircleShape)
                )
            }
            Row {
                Text(
                    text = stringResource(R.string.quiz_question_n_of_m, index + 1, total),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceMuted,
                    modifier = Modifier.weight(1f),
                )
                if (showsRunningScore && correctSoFar > 0) {
                    Text(
                        text = stringResource(R.string.quiz_n_correct, correctSoFar),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.success,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(top = PFTheme.spacing.md, bottom = PFTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            AnimatedContent(
                targetState = question,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                },
                label = "quizPrompt",
            ) { q ->
                PromptCard(q)
            }

            when (val kind = question.kind) {
                is QuizQuestion.Kind.MultipleChoice -> Column(
                    verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
                ) {
                    kind.options.forEachIndexed { i, option ->
                        OptionRow(
                            badge = ('A' + i).toString(),
                            text = option,
                            state = state(i, kind.correctIndex),
                            enabled = selection == null,
                            onClick = { onChoose(i, i == kind.correctIndex, option) },
                        )
                    }
                }
                is QuizQuestion.Kind.TrueFalse -> {
                    val trueLabel = stringResource(R.string.quiz_true)
                    val falseLabel = stringResource(R.string.quiz_false)
                    val correctIndex = if (kind.isTrue) 1 else 0
                    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                        val shape = RoundedCornerShape(PFRadius.lg)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PFTheme.colors.elevated, shape)
                                .border(0.7.dp, PFTheme.colors.border, shape)
                                .padding(PFTheme.spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
                        ) {
                            Text(
                                text = stringResource(R.string.quiz_is_this_the_answer),
                                style = PFTheme.type.footnoteBold,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                            Text(text = kind.shown, style = PFTheme.type.body, color = PFTheme.colors.onSurface)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                            OptionRow(
                                badge = null,
                                icon = PFIcons.Check,
                                text = trueLabel,
                                state = state(1, correctIndex),
                                enabled = selection == null,
                                onClick = { onChoose(1, kind.isTrue, trueLabel) },
                                modifier = Modifier.weight(1f),
                            )
                            OptionRow(
                                badge = null,
                                icon = PFIcons.Close,
                                text = falseLabel,
                                state = state(0, correctIndex),
                                enabled = selection == null,
                                onClick = { onChoose(0, !kind.isTrue, falseLabel) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = revealed && !isTest && lastAnswerCorrect != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
            ) {
                FeedbackCard(correct = lastAnswerCorrect == true, answer = question.correctAnswer)
            }
        }

        AnimatedVisibility(
            visible = revealed && !isTest,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            PFButton(
                title = stringResource(if (index == total - 1) R.string.quiz_see_results else R.string.continue_action),
                onClick = onContinue,
                size = PFButtonSize.Lg,
                trailingIcon = PFIcons.Forward,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun PromptCard(question: QuizQuestion) {
    val shape = RoundedCornerShape(PFRadius.xxl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pfElevation(PFElevation.Card, shape)
            .background(PFTheme.colors.paper, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
    ) {
        PFTagPill(
            title = stringResource(if (question.isTrueFalse) R.string.quiz_true_or_false else R.string.quiz_pick_one),
            icon = if (question.isTrueFalse) PFIcons.CheckCircle else PFIcons.List,
        )
        Text(text = question.prompt, style = PFTheme.type.title3, color = PFTheme.colors.onSurface)
    }
}

@Composable
private fun OptionRow(
    badge: String?,
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = PFTheme.colors
    val fill = when (state) {
        OptionState.Idle, OptionState.Dimmed -> colors.elevated
        OptionState.Selected -> colors.accentSoft
        OptionState.Correct -> colors.success.copy(alpha = 0.10f)
        OptionState.Wrong -> colors.danger.copy(alpha = 0.10f)
    }
    val stroke = when (state) {
        OptionState.Idle, OptionState.Dimmed -> colors.border
        OptionState.Selected -> colors.accent
        OptionState.Correct -> colors.success
        OptionState.Wrong -> colors.danger
    }
    val badgeFill = when (state) {
        OptionState.Idle, OptionState.Dimmed -> colors.surface
        OptionState.Selected -> colors.accent
        OptionState.Correct -> colors.success
        OptionState.Wrong -> colors.danger
    }
    val badgeContent = if (state == OptionState.Idle || state == OptionState.Dimmed) colors.onSurfaceMuted else Color.White
    val description = when (state) {
        OptionState.Correct -> stringResource(R.string.quiz_a11y_correct, text)
        OptionState.Wrong -> stringResource(R.string.quiz_a11y_wrong, text)
        else -> text
    }
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (state == OptionState.Dimmed) 0.6f else 1f)
            .background(fill, shape)
            .border(if (state == OptionState.Idle || state == OptionState.Dimmed) 0.7.dp else 1.5.dp, stroke, shape)
            .pfPressable(onClick = onClick, enabled = enabled, pressedScale = 0.985f)
            .semantics {
                contentDescription = description
                selected = state == OptionState.Selected
            }
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).background(badgeFill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state == OptionState.Correct -> Icon(PFIcons.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                state == OptionState.Wrong -> Icon(PFIcons.Close, null, tint = Color.White, modifier = Modifier.size(15.dp))
                icon != null -> Icon(icon, null, tint = badgeContent, modifier = Modifier.size(15.dp))
                badge != null -> Text(badge, style = PFTheme.type.footnoteBold, color = badgeContent)
            }
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Text(
            text = text,
            style = PFTheme.type.body,
            color = if (state == OptionState.Dimmed) colors.onSurfaceFaint else colors.onSurface,
        )
    }
}

@Composable
private fun FeedbackCard(correct: Boolean, answer: String) {
    val tint = if (correct) PFTheme.colors.success else PFTheme.colors.danger
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (correct) PFIcons.CheckCircle else PFIcons.Error, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(PFTheme.spacing.sm))
            Text(
                text = stringResource(if (correct) R.string.quiz_correct else R.string.quiz_not_quite),
                style = PFTheme.type.bodyEmphasis,
                color = tint,
            )
        }
        if (!correct) {
            Text(
                text = stringResource(R.string.quiz_the_answer_is, answer),
                style = PFTheme.type.footnote,
                color = PFTheme.colors.onSurface,
            )
        }
    }
}

@Composable
private fun TimerPill(secondsLeft: Int) {
    val urgent = secondsLeft <= 30
    val tint = if (urgent) PFTheme.colors.danger else PFTheme.colors.onSurfaceMuted
    val label = "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)
    val description = stringResource(R.string.quiz_a11y_time_left, label)
    Row(
        modifier = Modifier
            .padding(end = PFTheme.spacing.md)
            .background(tint.copy(alpha = 0.10f), CircleShape)
            .semantics { contentDescription = description }
            .padding(horizontal = PFTheme.spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PFIcons.Timer, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = PFTheme.type.footnoteBold, color = tint)
    }
}
