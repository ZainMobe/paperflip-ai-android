package com.wapp.paperflipai.feature.support

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.network.SupportChatMessage
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFSheet
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFToast
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch

/**
 * The in-app AI support assistant — the Android counterpart of
 * `SupportChatView.swift`. A one-time consent notice gates the first
 * message; long-pressing an assistant reply flags it for review.
 */
private val suggestions = listOf(
    R.string.how_do_i_cancel,
    R.string.how_does_spaced_repetition_work_q,
    R.string.why_is_my_deck_missing,
)

@Composable
fun SupportChatScreen(onBack: () -> Unit) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val hasConsented by env.settings.hasAcknowledgedAI.collectAsStateWithLifecycle()
    var showConsent by remember { mutableStateOf(!hasConsented) }

    val messages = remember { mutableListOf<SupportChatMessage>().toMutableStateList() }
    var draft by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var flagTarget by remember { mutableStateOf<SupportChatMessage?>(null) }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isThinking) return
        draft = ""
        error = null
        messages.add(SupportChatMessage.user(trimmed))
        scope.launch {
            isThinking = true
            try {
                val reply = env.remote.supportChat(messages.toList())
                messages.add(SupportChatMessage.assistant(reply))
            } catch (failure: Exception) {
                error = failure.message
            } finally {
                isThinking = false
            }
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    PFScreen(
        topBar = { PFTopBar(title = stringResource(R.string.help_support), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pfReadableWidth(),
                contentPadding = PaddingValues(PFTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                            Text(
                                text = stringResource(R.string.ai_assistant),
                                style = PFTheme.type.overline,
                                color = PFTheme.colors.accent,
                            )
                            Text(
                                text = stringResource(R.string.paperflip_support_uses_ai_to_answer_product_qu),
                                style = PFTheme.type.callout,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                            Spacer(Modifier.size(PFTheme.spacing.sm))
                            Text(
                                text = stringResource(R.string.try_asking),
                                style = PFTheme.type.overline,
                                color = PFTheme.colors.onSurfaceMuted,
                            )
                            suggestions.forEach { suggestionRes ->
                                val text = stringResource(suggestionRes)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            PFTheme.colors.elevated,
                                            RoundedCornerShape(PFRadius.lg),
                                        )
                                        .border(
                                            0.7.dp,
                                            PFTheme.colors.border,
                                            RoundedCornerShape(PFRadius.lg),
                                        )
                                        .pfPressable(onClick = { send(text) }, pressedScale = 0.98f)
                                        .padding(PFTheme.spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = PFIcons.Idea,
                                        contentDescription = null,
                                        tint = PFTheme.colors.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(PFTheme.spacing.sm))
                                    Text(
                                        text = text,
                                        style = PFTheme.type.body,
                                        color = PFTheme.colors.onSurface,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            onLongClick = { if (!message.isUser) flagTarget = message },
                        )
                    }
                }

                if (isThinking) {
                    item { TypingIndicator() }
                }

                item { PFErrorBanner(error) }
            }

            // ── Composer ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pfReadableWidth()
                    .padding(horizontal = PFTheme.spacing.lg, vertical = PFTheme.spacing.sm),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(Modifier.weight(1f)) {
                    PFTextField(
                        label = "",
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = stringResource(R.string.ask_anything),
                        singleLine = false,
                        imeAction = ImeAction.Send,
                    )
                }
                Spacer(Modifier.width(PFTheme.spacing.sm))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (draft.isBlank()) PFTheme.colors.elevated else PFTheme.colors.accent,
                            CircleShape,
                        )
                        .pfPressable(
                            onClick = { send(draft) },
                            enabled = draft.isNotBlank() && !isThinking,
                            pressedScale = 0.9f,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PFIcons.Send,
                        contentDescription = stringResource(R.string.submit),
                        tint = if (draft.isBlank()) PFTheme.colors.onSurfaceFaint
                        else PFTheme.colors.onAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    if (showConsent) {
        AIConsentSheet(
            onDismiss = {
                showConsent = false
                if (!hasConsented) onBack()
            },
            onAcknowledge = {
                env.settings.acknowledgeAI()
                showConsent = false
            },
        )
    }

    flagTarget?.let { target ->
        FlagMessageSheet(
            onDismiss = { flagTarget = null },
            onSubmit = { reason ->
                flagTarget = null
                scope.launch {
                    val userId = env.auth.session.value?.userId.orEmpty()
                    runCatching { env.remote.flagSupportMessage(target.content, reason, userId) }
                }
            },
        )
    }
}

@Composable
private fun ChatBubble(message: SupportChatMessage, onLongClick: () -> Unit) {
    val isUser = message.isUser
    val shape = RoundedCornerShape(
        topStart = PFRadius.lg,
        topEnd = PFRadius.lg,
        bottomStart = if (isUser) PFRadius.lg else PFRadius.sm,
        bottomEnd = if (isUser) PFRadius.sm else PFRadius.lg,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.content,
            style = PFTheme.type.body,
            color = if (isUser) PFTheme.colors.onAccent else PFTheme.colors.onSurface,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(if (isUser) PFTheme.colors.accent else PFTheme.colors.elevated, shape)
                .pfPressable(onClick = {}, onLongClick = onLongClick, pressedScale = 0.99f, haptic = null)
                .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
        )
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "typingDot$index",
            )
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(6.dp)
                    .background(PFTheme.colors.onSurfaceMuted.copy(alpha = alpha), CircleShape)
            )
        }
        Spacer(Modifier.width(PFTheme.spacing.sm))
        Text(
            text = stringResource(R.string.assistant_is_thinking),
            style = PFTheme.type.caption,
            color = PFTheme.colors.onSurfaceMuted,
        )
    }
}

@Composable
private fun FlagMessageSheet(
    onDismiss: () -> Unit,
    onSubmit: (String?) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    val flaggedLabel = stringResource(R.string.flagged_for_review)

    PFSheet(onDismiss = onDismiss, title = stringResource(R.string.flag_this_response)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PFTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.help_us_improve_the_assistant_we_ll_review_fla),
                style = PFTheme.type.callout,
                color = PFTheme.colors.onSurfaceMuted,
            )
            PFTextField(
                label = stringResource(R.string.reason_optional),
                value = reason,
                onValueChange = { reason = it },
                placeholder = stringResource(R.string.ask_anything),
                singleLine = false,
                minLines = 2,
                imeAction = ImeAction.Done,
            )
            PFButton(
                title = stringResource(R.string.flag_response),
                onClick = {
                    onSubmit(reason.trim().ifEmpty { null })
                    PFToast.info(flaggedLabel)
                },
                leadingIcon = PFIcons.Flag,
                isDestructive = true,
            )
        }
    }
}
