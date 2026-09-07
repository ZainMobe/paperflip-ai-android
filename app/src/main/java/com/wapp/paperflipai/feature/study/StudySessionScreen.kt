package com.wapp.paperflipai.feature.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.PFFlashcard
import com.wapp.paperflipai.core.data.PFStudySession
import com.wapp.paperflipai.core.srs.SRSResponse
import com.wapp.paperflipai.core.srs.StudyQueue
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFIconButton
import com.wapp.paperflipai.designsystem.component.PFProgressRing
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * The full-screen study session — the Android counterpart of
 * `StudySessionView.swift`. Owns the queue, drives SM-2 on each response,
 * persists the card state plus a session row, and swaps to the summary
 * when the queue empties.
 */
@Composable
fun StudySessionScreen(
    deckId: String?,
    onFinish: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val snapshot by env.database.snapshot.collectAsStateWithLifecycle()

    val deck = deckId?.let { id -> snapshot.decks.firstOrNull { it.id == id } }
    val deckTitle = deck?.title ?: stringResource(R.string.mixed_session)

    // The queue is captured once so mutating SRS state mid-session doesn't
    // reshuffle the cards under the user.
    val queue: List<PFFlashcard> = remember(deckId) {
        val pool = if (deckId != null) env.database.cards(deckId) else env.database.snapshot.value.cards
        StudyQueue.dueCards(pool).ifEmpty {
            pool.shuffled().take(StudyQueue.DEFAULT_LIMIT)
        }
    }

    var index by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var didIncrementStreak by remember { mutableStateOf(false) }
    var streakCount by remember { mutableIntStateOf(0) }
    val startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var finishedAt by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = !isComplete) { onFinish() }

    fun finish() {
        finishedAt = System.currentTimeMillis()
        val touchedDecks = queue.take(index).map { it.deckId }.toSet()
        env.repository.finishSession(
            session = PFStudySession(
                deckId = deckId,
                startedAt = startedAt,
                endedAt = finishedAt,
                cardsStudied = index,
                cardsCorrect = correctCount,
            ),
            deckIds = touchedDecks,
        )
        didIncrementStreak = env.streaks.recordStudy()
        streakCount = env.streaks.count.value
        env.refreshWidget()
        isComplete = true
    }

    fun handle(response: SRSResponse) {
        val card = queue.getOrNull(index) ?: return
        env.repository.review(card, response)
        if (response.didRecall) correctCount += 1
        scope.launch {
            env.database.deck(card.deckId)?.let { env.repository.touchDeck(it) }
        }
        index += 1
        if (index >= queue.size) finish()
    }

    if (queue.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PFTheme.colors.surface)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
        ) {
            PFEmptyState(
                icon = PFIcons.CheckCircle,
                title = stringResource(R.string.all_caught_up),
                message = stringResource(R.string.nothing_to_review_right_now_come_back_tomorrow),
                ctaTitle = stringResource(R.string.done),
                onCta = onFinish,
            )
        }
        return
    }

    AnimatedContent(
        targetState = isComplete,
        transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
        label = "sessionComplete",
        modifier = Modifier
            .fillMaxSize()
            .background(PFTheme.colors.surface),
    ) { complete ->
        if (complete) {
            StudySummary(
                cardsReviewed = index,
                cardsCorrect = correctCount,
                durationMillis = (finishedAt - startedAt).coerceAtLeast(0),
                streak = streakCount,
                didIncrementStreak = didIncrementStreak,
                onDone = onFinish,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // ── Top bar ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PFTheme.spacing.lg)
                        .padding(top = PFTheme.spacing.md, bottom = PFTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PFIconButton(
                        icon = PFIcons.Close,
                        contentDescription = stringResource(R.string.nav_close),
                        onClick = onFinish,
                        tint = PFTheme.colors.onSurfaceMuted,
                        modifier = Modifier.background(PFTheme.colors.elevated, CircleShape),
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = deckTitle,
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(
                                R.string.of,
                                (index + 1).coerceAtMost(queue.size),
                                queue.size,
                            ),
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    PFProgressRing(
                        progress = index.toFloat() / queue.size,
                        size = 36.dp,
                        lineWidth = 3.dp,
                        showsLabel = false,
                    )
                }

                // ── Card stack ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pfReadableWidth(560.dp)
                        .padding(horizontal = PFTheme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CardStack(
                        cards = queue.drop(index),
                        onResponse = { response -> handle(response) },
                    )
                }

                // ── Response bar ──────────────────────────────────────
                queue.getOrNull(index)?.let { card ->
                    StudyResponseBar(
                        card = card,
                        onResponse = { response -> handle(response) },
                        modifier = Modifier.padding(bottom = PFTheme.spacing.lg),
                    )
                }
            }
        }
    }
}
