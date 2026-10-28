package com.wapp.paperflipai.feature.quiz

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.newId
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFEmptyState
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFMotion
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * Match: up to six terms and their six answers scattered in a grid. Tap a
 * term, then its answer; matched pairs fade out, wrong pairs shake. Rounds
 * of six run through the whole deck against the clock, with a best time
 * kept per deck. Counterpart of `MatchGameView.swift`.
 */
@Composable
fun MatchGameScreen(
    deckId: String,
    onFinish: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val haptics = rememberPFHaptics()
    val scope = rememberCoroutineScope()

    val cards = remember(deckId) {
        env.database.cards(deckId).filter { it.front.isNotBlank() && it.back.isNotBlank() }
    }
    val bestTimes = remember { context.getSharedPreferences("match_best", Context.MODE_PRIVATE) }

    var rounds by remember { mutableStateOf(emptyList<List<Pair<String, Pair<String, String>>>>()) }
    var roundIndex by remember { mutableIntStateOf(0) }
    var tiles by remember { mutableStateOf(emptyList<MatchTile>()) }
    var selected by remember { mutableStateOf<MatchTile?>(null) }
    var matched by remember { mutableStateOf(emptySet<String>()) }
    var shaking by remember { mutableStateOf(emptySet<String>()) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var mistakes by remember { mutableIntStateOf(0) }
    var complete by remember { mutableStateOf(false) }
    var isNewBest by remember { mutableStateOf(false) }
    var game by remember { mutableIntStateOf(0) }

    val best = bestTimes.getLong(deckId, 0L).takeIf { it > 0 }

    fun loadRound() {
        tiles = rounds[roundIndex].flatMap { (cardId, faces) ->
            listOf(
                MatchTile(newId(), cardId, faces.first, isTerm = true),
                MatchTile(newId(), cardId, faces.second, isTerm = false),
            )
        }.shuffled()
        matched = emptySet()
        selected = null
        shaking = emptySet()
    }

    fun start() {
        val shuffled = cards.shuffled().map { it.id to (it.front to it.back) }
        if (shuffled.size < 3) {
            rounds = emptyList()
            return
        }
        val chunks = shuffled.chunked(6).toMutableList()
        // Fold a last round of one or two pairs into the previous one.
        if (chunks.size > 1 && chunks.last().size < 3) {
            val last = chunks.removeAt(chunks.lastIndex)
            chunks[chunks.lastIndex] = chunks.last() + last
        }
        rounds = chunks
        roundIndex = 0
        elapsedMs = 0
        mistakes = 0
        complete = false
        isNewBest = false
        loadRound()
        game += 1
        PFAnalytics.log(AnalyticsEvent.matchStarted)
    }

    fun finishGame() {
        complete = true
        PFAnalytics.log(AnalyticsEvent.matchCompleted((elapsedMs / 1000).toInt()))
        val previous = bestTimes.getLong(deckId, 0L)
        if (previous == 0L || elapsedMs < previous) {
            isNewBest = true
            bestTimes.edit().putLong(deckId, elapsedMs).apply()
        }
        haptics.perform(PFHaptic.Success)
    }

    fun tap(tile: MatchTile) {
        if (tile.id in matched || complete) return
        val first = selected
        when {
            first == null -> {
                haptics.perform(PFHaptic.Selection)
                selected = tile
            }
            first.id == tile.id -> selected = null
            first.cardId == tile.cardId && first.isTerm != tile.isTerm -> {
                haptics.perform(PFHaptic.Success)
                matched = matched + first.id + tile.id
                selected = null
                if (matched.size == tiles.size) {
                    scope.launch {
                        delay(450)
                        if (roundIndex + 1 < rounds.size) {
                            roundIndex += 1
                            loadRound()
                        } else {
                            finishGame()
                        }
                    }
                }
            }
            else -> {
                haptics.perform(PFHaptic.Error)
                mistakes += 1
                shaking = setOf(first.id, tile.id)
                selected = null
                scope.launch {
                    delay(420)
                    shaking = emptySet()
                }
            }
        }
    }

    LaunchedEffect(Unit) { start() }

    // Clock, restarted for each game.
    LaunchedEffect(game) {
        if (game == 0) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (!complete) {
            delay(100)
            if (!complete) elapsedMs = System.currentTimeMillis() - startedAt
        }
    }

    PFScreen(
        topBar = {
            PFTopBar(
                title = stringResource(R.string.match_title),
                onBack = onFinish,
                navigationIcon = PFIcons.Close,
                actions = {
                    if (!complete && rounds.isNotEmpty()) {
                        val label = formatMatchTime(elapsedMs)
                        val description = stringResource(R.string.match_a11y_elapsed, label)
                        Text(
                            text = label,
                            style = PFTheme.type.footnoteBold,
                            color = PFTheme.colors.onSurfaceMuted,
                            modifier = Modifier
                                .padding(end = PFTheme.spacing.lg)
                                .semantics { contentDescription = description },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                cards.size < 3 -> PFEmptyState(
                    icon = PFIcons.Grid,
                    title = stringResource(R.string.match_need_cards),
                    message = stringResource(R.string.match_need_cards_message),
                    ctaTitle = stringResource(R.string.done),
                    onCta = onFinish,
                )
                complete -> MatchSummary(
                    elapsedMs = elapsedMs,
                    mistakes = mistakes,
                    isNewBest = isNewBest,
                    onPlayAgain = { start() },
                    onDone = onFinish,
                )
                tiles.isNotEmpty() -> Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.match_round_n_of_m, roundIndex + 1, rounds.size),
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceMuted,
                            modifier = Modifier.weight(1f),
                        )
                        best?.let {
                            Icon(PFIcons.Trophy, null, tint = PFTheme.colors.warning, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.match_best, formatMatchTime(it)),
                                style = PFTheme.type.caption,
                                color = PFTheme.colors.warning,
                            )
                        }
                    }
                    key(roundIndex) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = PFTheme.spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
                            modifier = Modifier.weight(1f),
                        ) {
                            items(tiles, key = { it.id }) { tile ->
                                MatchTileView(
                                    tile = tile,
                                    isSelected = selected?.id == tile.id,
                                    isMatched = tile.id in matched,
                                    isShaking = tile.id in shaking,
                                    onClick = { tap(tile) },
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.match_hint),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.onSurfaceFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PFTheme.spacing.lg),
                    )
                }
            }
        }
    }
}

private data class MatchTile(val id: String, val cardId: String, val text: String, val isTerm: Boolean)

@Composable
private fun MatchTileView(
    tile: MatchTile,
    isSelected: Boolean,
    isMatched: Boolean,
    isShaking: Boolean,
    onClick: () -> Unit,
) {
    val fade by animateFloatAsState(if (isMatched) 0f else 1f, PFMotion.snappy(), label = "matchFade")
    val shake = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            repeat(3) {
                shake.animateTo(-8f, tween(45))
                shake.animateTo(8f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
    }
    val colors = PFTheme.colors
    val shape = RoundedCornerShape(PFRadius.lg)
    val role = stringResource(if (tile.isTerm) R.string.match_a11y_term else R.string.match_a11y_answer)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .graphicsLayer {
                alpha = fade
                scaleX = 0.6f + 0.4f * fade
                scaleY = 0.6f + 0.4f * fade
                translationX = shake.value
            }
            .background(
                when {
                    isSelected -> colors.accentSoft
                    tile.isTerm -> colors.paper
                    else -> colors.elevated
                },
                shape,
            )
            .border(
                if (isSelected || isShaking) 1.5.dp else 0.7.dp,
                when {
                    isShaking -> colors.danger
                    isSelected -> colors.accent
                    else -> colors.border
                },
                shape,
            )
            .pfPressable(onClick = onClick, enabled = !isMatched, haptic = null, pressedScale = 0.96f)
            .semantics {
                contentDescription = "$role: ${tile.text}"
                selected = isSelected
            }
            .padding(PFTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.text,
            style = if (tile.isTerm) PFTheme.type.footnoteBold else PFTheme.type.footnote,
            color = if (isSelected) colors.accent else colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MatchSummary(
    elapsedMs: Long,
    mistakes: Int,
    isNewBest: Boolean,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PFTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(PFTheme.colors.success.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isNewBest) PFIcons.Trophy else PFIcons.Check,
                contentDescription = null,
                tint = if (isNewBest) PFTheme.colors.warning else PFTheme.colors.success,
                modifier = Modifier.size(52.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(if (isNewBest) R.string.match_new_best else R.string.match_all_matched),
                style = PFTheme.type.title2,
                color = PFTheme.colors.onSurface,
            )
            Text(formatMatchTime(elapsedMs), style = PFTheme.type.display, color = PFTheme.colors.onSurface)
            Text(
                text = if (mistakes == 0) stringResource(R.string.match_no_mistakes)
                else stringResource(R.string.match_n_wrong, mistakes),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
            PFButton(
                title = stringResource(R.string.match_play_again),
                onClick = onPlayAgain,
                size = PFButtonSize.Lg,
                leadingIcon = PFIcons.Refresh,
            )
            PFButton(title = stringResource(R.string.done), onClick = onDone, variant = PFButtonVariant.Ghost)
        }
    }
}

private fun formatMatchTime(ms: Long): String {
    val total = ms / 1000
    val tenths = (ms % 1000) / 100
    return "%d:%02d.%d".format(total / 60, total % 60, tenths)
}
