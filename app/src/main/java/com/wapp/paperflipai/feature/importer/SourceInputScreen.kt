package com.wapp.paperflipai.feature.importer

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.data.DeckDto
import com.wapp.paperflipai.core.data.GenerationRequest
import com.wapp.paperflipai.core.data.PFSourceType
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonSize
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFScreen
import com.wapp.paperflipai.designsystem.component.PFTextField
import com.wapp.paperflipai.designsystem.component.PFTopBar
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.pfReadableWidth
import com.wapp.paperflipai.designsystem.theme.PFGenerationLanguage
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The whole import flow after a source is chosen — the Android counterpart
 * of `SourceInputView` + `GeneratingDeckView` + `ReviewGeneratedDeckView`.
 *
 * iOS pushes each step onto a NavigationStack; here the three steps live in
 * one destination and swap with [AnimatedContent], which keeps the picked
 * file, the generated DTO and the user's edits in one place and makes
 * "back" behave sensibly at every step.
 */
private sealed interface ImportStep {
    data object Input : ImportStep
    data class Generating(val request: GenerationRequest) : ImportStep
    data class Review(val deck: DeckDto) : ImportStep
}

data class PickedPdf(val uri: Uri, val fileName: String, val sizeBytes: Long) {
    val sizeLabel: String
        get() = when {
            sizeBytes >= 1_048_576 -> "%.1f MB".format(sizeBytes / 1_048_576.0)
            sizeBytes >= 1024 -> "%.0f KB".format(sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}

@Composable
fun SourceInputScreen(
    sourceTypeRaw: String,
    sharedValue: String?,
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sourceType = PFSourceType.from(sourceTypeRaw)
    val entitlement by env.entitlements.entitlement.collectAsStateWithLifecycle()
    val appLanguage by env.settings.language.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf<ImportStep>(ImportStep.Input) }

    // Input state
    var url by remember { mutableStateOf(if (sourceType != PFSourceType.Pdf) sharedValue.orEmpty() else "") }
    var pickedPdf by remember { mutableStateOf<PickedPdf?>(null) }
    var pdfError by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var cardCount by remember { mutableFloatStateOf(10f) }
    var autoCardCount by remember { mutableStateOf(false) }
    var useSourceLanguage by remember { mutableStateOf(true) }
    var selectedLanguage by remember {
        mutableStateOf(PFGenerationLanguage.defaultLanguage(appLanguage.rawValue))
    }

    val maxCards = entitlement.cardCap
    LaunchedEffect(maxCards) {
        cardCount = cardCount.coerceAtMost(maxCards.toFloat())
        if (!entitlement.isActive) autoCardCount = false
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val (name, size) = context.queryFile(uri)
        if (size > MAX_PDF_BYTES) {
            pdfError = context.getString(R.string.pdf_too_large)
        } else {
            pdfError = null
            pickedPdf = PickedPdf(uri, name, size)
        }
    }

    // A PDF shared in from another app arrives as a content:// URI.
    LaunchedEffect(sharedValue) {
        if (sourceType == PFSourceType.Pdf && sharedValue != null) {
            val uri = Uri.parse(sharedValue)
            val (name, size) = context.queryFile(uri)
            if (size in 1..MAX_PDF_BYTES) pickedPdf = PickedPdf(uri, name, size)
        }
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
        label = "importStep",
    ) { current ->
        when (current) {
            is ImportStep.Input -> {
                val trimmedUrl = url.trim()
                val urlValid = trimmedUrl.isValidHttpUrl()
                val youtubeValid = urlValid && trimmedUrl.isYouTubeUrl()
                val urlError = when {
                    trimmedUrl.isEmpty() -> null
                    sourceType == PFSourceType.Youtube && !youtubeValid ->
                        stringResource(R.string.doesnt_look_like_a_youtube_link)
                    sourceType == PFSourceType.Article && !urlValid ->
                        stringResource(R.string.that_doesnt_look_like_a_valid_url)
                    else -> null
                }
                val isReady = when (sourceType) {
                    PFSourceType.Pdf -> pickedPdf != null
                    PFSourceType.Youtube -> youtubeValid
                    PFSourceType.Article -> urlValid
                }

                SourceInputContent(
                    sourceType = sourceType,
                    onBack = onBack,
                    url = url,
                    onUrlChange = { url = it },
                    urlError = urlError,
                    pickedPdf = pickedPdf,
                    pdfError = pdfError,
                    onPickPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
                    isReady = isReady,
                    cardCount = cardCount,
                    onCardCountChange = { cardCount = it },
                    maxCards = maxCards,
                    autoCardCount = autoCardCount,
                    onToggleAuto = {
                        if (entitlement.isActive) autoCardCount = !autoCardCount else onOpenPaywall()
                    },
                    isPro = entitlement.isActive,
                    onOpenPaywall = onOpenPaywall,
                    useSourceLanguage = useSourceLanguage,
                    selectedLanguage = selectedLanguage,
                    onSelectSourceLanguage = { useSourceLanguage = true },
                    onSelectLanguage = { selectedLanguage = it; useSourceLanguage = false },
                    isUploading = isUploading,
                    onGenerate = {
                        scope.launch {
                            var sourceRef = when (sourceType) {
                                PFSourceType.Pdf -> pickedPdf?.fileName.orEmpty()
                                else -> url.trim()
                            }
                            if (sourceType == PFSourceType.Pdf) {
                                val pick = pickedPdf ?: return@launch
                                isUploading = true
                                val bytes = withContext(Dispatchers.IO) {
                                    runCatching {
                                        context.contentResolver.openInputStream(pick.uri)?.use { it.readBytes() }
                                    }.getOrNull()
                                }
                                if (bytes == null) {
                                    pdfError = context.getString(R.string.couldnt_read_the_pdf)
                                    isUploading = false
                                    return@launch
                                }
                                val userId = env.auth.session.value?.userId.orEmpty()
                                sourceRef = try {
                                    env.remote.uploadPdf(bytes, pick.fileName, userId)
                                } catch (failure: Exception) {
                                    pdfError = failure.message
                                    isUploading = false
                                    return@launch
                                }
                                isUploading = false
                            }
                            step = ImportStep.Generating(
                                GenerationRequest.of(
                                    sourceType = sourceType,
                                    sourceRef = sourceRef,
                                    targetCardCount = if (autoCardCount) maxCards else cardCount.roundToInt(),
                                    autoCardCount = autoCardCount,
                                    language = selectedLanguage,
                                    useSourceLanguage = useSourceLanguage,
                                )
                            )
                        }
                    },
                )
            }

            is ImportStep.Generating -> GeneratingDeckContent(
                request = current.request,
                onGenerated = { step = ImportStep.Review(it) },
                onCancel = { step = ImportStep.Input },
            )

            is ImportStep.Review -> ReviewGeneratedDeckContent(
                dto = current.deck,
                onSaved = { onOpenDeck(it) },
                onDiscarded = onBack,
                onOpenPaywall = onOpenPaywall,
            )
        }
    }
}

private const val MAX_PDF_BYTES = 32L * 1024 * 1024

private fun android.content.Context.queryFile(uri: Uri): Pair<String, Long> {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
    var size = 0L
    runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
    }
    return name to size
}

internal fun String.isValidHttpUrl(): Boolean {
    val lower = lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
    val host = substringAfter("://").substringBefore('/').substringBefore('?')
    return host.contains('.') && host.length > 3
}

internal fun String.isYouTubeUrl(): Boolean {
    val host = lowercase().substringAfter("://").substringBefore('/')
    return host.contains("youtube.com") || host.contains("youtu.be")
}

// ── Input step ───────────────────────────────────────────────────────

@Composable
private fun SourceInputContent(
    sourceType: PFSourceType,
    onBack: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    urlError: String?,
    pickedPdf: PickedPdf?,
    pdfError: String?,
    onPickPdf: () -> Unit,
    isReady: Boolean,
    cardCount: Float,
    onCardCountChange: (Float) -> Unit,
    maxCards: Int,
    autoCardCount: Boolean,
    onToggleAuto: () -> Unit,
    isPro: Boolean,
    onOpenPaywall: () -> Unit,
    useSourceLanguage: Boolean,
    selectedLanguage: PFGenerationLanguage,
    onSelectSourceLanguage: () -> Unit,
    onSelectLanguage: (PFGenerationLanguage) -> Unit,
    isUploading: Boolean,
    onGenerate: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val tint = when (sourceType) {
        PFSourceType.Pdf -> PFTheme.colors.accent
        PFSourceType.Youtube -> PFTheme.colors.danger
        PFSourceType.Article -> PFTheme.colors.warning
    }
    val icon: ImageVector = when (sourceType) {
        PFSourceType.Pdf -> PFIcons.Pdf
        PFSourceType.Youtube -> PFIcons.Video
        PFSourceType.Article -> PFIcons.Article
    }
    val title = stringResource(
        when (sourceType) {
            PFSourceType.Pdf -> R.string.from_a_pdf
            PFSourceType.Youtube -> R.string.from_youtube
            PFSourceType.Article -> R.string.from_an_article
        }
    )
    val subtitle = stringResource(
        when (sourceType) {
            PFSourceType.Pdf -> R.string.pick_a_file_from_your_device_or_icloud
            PFSourceType.Youtube -> R.string.any_video_with_captions_works
            PFSourceType.Article -> R.string.we_strip_the_chrome_and_read_just_the_content
        }
    )

    PFScreen(topBar = { PFTopBar(title = title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .pfReadableWidth()
                .padding(horizontal = PFTheme.spacing.lg)
                .padding(bottom = PFTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.xl),
        ) {
            Row(
                modifier = Modifier.padding(top = PFTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(tint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(text = title, style = PFTheme.type.title2, color = PFTheme.colors.onSurface)
                    Text(
                        text = subtitle,
                        style = PFTheme.type.callout,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
            }

            if (sourceType == PFSourceType.Pdf) {
                PdfInput(
                    tint = tint,
                    picked = pickedPdf,
                    error = pdfError,
                    onPick = onPickPdf,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                    PFTextField(
                        label = stringResource(R.string.link),
                        value = url,
                        onValueChange = onUrlChange,
                        placeholder = if (sourceType == PFSourceType.Youtube)
                            "https://youtube.com/watch?v=…" else "https://example.com/article",
                        icon = PFIcons.Link,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        error = urlError,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                        MiniChip(
                            icon = PFIcons.Copy,
                            label = stringResource(R.string.paste),
                            onClick = {
                                clipboard.getText()?.text?.let(onUrlChange)
                            },
                        )
                        if (url.isNotEmpty()) {
                            MiniChip(
                                icon = PFIcons.Close,
                                label = stringResource(R.string.clear),
                                muted = true,
                                onClick = { onUrlChange("") },
                            )
                        }
                    }
                }
            }

            if (isReady) {
                CardCountCard(
                    cardCount = cardCount,
                    onCardCountChange = onCardCountChange,
                    maxCards = maxCards,
                    autoCardCount = autoCardCount,
                    onToggleAuto = onToggleAuto,
                    isPro = isPro,
                    onOpenPaywall = onOpenPaywall,
                )

                LanguageCard(
                    sourceType = sourceType,
                    useSourceLanguage = useSourceLanguage,
                    selectedLanguage = selectedLanguage,
                    onSelectSourceLanguage = onSelectSourceLanguage,
                    onSelectLanguage = onSelectLanguage,
                )

                PFButton(
                    title = stringResource(
                        if (isUploading) R.string.uploading else R.string.generate_cards
                    ),
                    onClick = onGenerate,
                    size = PFButtonSize.Lg,
                    trailingIcon = PFIcons.Sparkle,
                    isLoading = isUploading,
                    enabled = !isUploading,
                    modifier = Modifier.padding(top = PFTheme.spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun PdfInput(
    tint: Color,
    picked: PickedPdf?,
    error: String?,
    onPick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
        if (picked == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.xl))
                    .border(1.5.dp, PFTheme.colors.border, RoundedCornerShape(PFRadius.xl))
                    .pfPressable(onClick = onPick, pressedScale = 0.98f)
                    .padding(vertical = PFTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(tint.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PFIcons.Upload, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = stringResource(R.string.choose_a_pdf),
                    style = PFTheme.type.headline,
                    color = PFTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.up_to_50_pages_on_the_free_plan),
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
                    .border(0.7.dp, PFTheme.colors.border, RoundedCornerShape(PFRadius.lg))
                    .padding(PFTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(tint.copy(alpha = 0.14f), RoundedCornerShape(PFRadius.md)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PFIcons.Pdf, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(PFTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = picked.fileName,
                        style = PFTheme.type.bodyEmphasis,
                        color = PFTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = picked.sizeLabel,
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.onSurfaceMuted,
                    )
                }
                Icon(
                    imageVector = PFIcons.Refresh,
                    contentDescription = null,
                    tint = PFTheme.colors.accent,
                    modifier = Modifier
                        .size(22.dp)
                        .pfPressable(onClick = onPick, pressedScale = 0.9f),
                )
            }
        }
        PFErrorBanner(error)
    }
}

@Composable
private fun MiniChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    val foreground = if (muted) PFTheme.colors.onSurfaceMuted else PFTheme.colors.accent
    val background = if (muted) PFTheme.colors.elevated else PFTheme.colors.accentSoft
    Row(
        modifier = Modifier
            .background(background, CircleShape)
            .pfPressable(onClick = onClick, pressedScale = 0.96f)
            .padding(horizontal = PFTheme.spacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(PFTheme.spacing.xs))
        Text(text = label, style = PFTheme.type.footnoteBold, color = foreground)
    }
}

@Composable
private fun CardCountCard(
    cardCount: Float,
    onCardCountChange: (Float) -> Unit,
    maxCards: Int,
    autoCardCount: Boolean,
    onToggleAuto: () -> Unit,
    isPro: Boolean,
    onOpenPaywall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cards_2),
                style = PFTheme.type.overline,
                color = PFTheme.colors.onSurfaceMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (autoCardCount) stringResource(R.string.auto) else cardCount.roundToInt().toString(),
                style = PFTheme.type.title3,
                color = PFTheme.colors.accent,
            )
        }

        SelectableRow(
            selected = autoCardCount,
            icon = PFIcons.Sparkle,
            title = stringResource(R.string.auto),
            subtitle = stringResource(R.string.pick_a_count_from_the_source_s_length),
            trailingBadge = if (!isPro) stringResource(R.string.pro) else null,
            onClick = onToggleAuto,
        )

        if (!autoCardCount) {
            Slider(
                value = cardCount,
                onValueChange = onCardCountChange,
                valueRange = 5f..maxCards.toFloat(),
                steps = ((maxCards - 5) / 5) - 1,
                colors = SliderDefaults.colors(
                    thumbColor = PFTheme.colors.accent,
                    activeTrackColor = PFTheme.colors.accent,
                    inactiveTrackColor = PFTheme.colors.divider,
                ),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("5", style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceFaint)
                Spacer(Modifier.weight(1f))
                Text(
                    text = maxCards.toString(),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.we_ll_pick_a_count_based_on_the_source_s_lengt, maxCards),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }

        if (!isPro) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pfPressable(onClick = onOpenPaywall, pressedScale = 0.99f)
                    .padding(top = PFTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = PFIcons.Sparkle,
                    contentDescription = null,
                    tint = PFTheme.colors.accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(PFTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.free_plan_up_to_cards_get_50_with_pro, maxCards),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    sourceType: PFSourceType,
    useSourceLanguage: Boolean,
    selectedLanguage: PFGenerationLanguage,
    onSelectSourceLanguage: () -> Unit,
    onSelectLanguage: (PFGenerationLanguage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.flashcard_language),
            style = PFTheme.type.overline,
            color = PFTheme.colors.onSurfaceMuted,
        )
        Text(
            text = stringResource(R.string.the_language_your_cards_will_be_written_in),
            style = PFTheme.type.caption,
            color = PFTheme.colors.onSurfaceMuted,
            modifier = Modifier.padding(bottom = PFTheme.spacing.xs),
        )

        SelectableRow(
            selected = useSourceLanguage,
            icon = PFIcons.Document,
            title = stringResource(
                when (sourceType) {
                    PFSourceType.Pdf -> R.string.pdf_default
                    PFSourceType.Youtube -> R.string.video_default
                    PFSourceType.Article -> R.string.article_default
                }
            ),
            subtitle = stringResource(R.string.match_the_source_s_language),
            onClick = onSelectSourceLanguage,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PFTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            userScrollEnabled = false,
        ) {
            items(PFGenerationLanguage.entries) { language ->
                val selected = !useSourceLanguage && selectedLanguage == language
                SelectableRow(
                    selected = selected,
                    emoji = language.flag,
                    title = language.displayName,
                    onClick = { onSelectLanguage(language) },
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    emoji: String? = null,
    trailingBadge: String? = null,
) {
    val shape = RoundedCornerShape(PFRadius.md)
    val foreground = if (selected) PFTheme.colors.accent else PFTheme.colors.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) PFTheme.colors.accentSoft else PFTheme.colors.surface, shape)
            .border(
                width = if (selected) 1.5.dp else 0.7.dp,
                color = if (selected) PFTheme.colors.accent.copy(alpha = 0.4f) else PFTheme.colors.border,
                shape = shape,
            )
            .pfPressable(onClick = onClick, haptic = PFHaptic.Selection, pressedScale = 0.97f)
            .padding(horizontal = PFTheme.spacing.md, vertical = PFTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        emoji?.let {
            Text(text = it, style = PFTheme.type.body)
            Spacer(Modifier.width(PFTheme.spacing.sm))
        }
        icon?.let {
            Icon(it, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(PFTheme.spacing.sm))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = PFTheme.type.footnoteBold,
                    color = foreground,
                    maxLines = 1,
                )
                trailingBadge?.let {
                    Spacer(Modifier.width(PFTheme.spacing.xs))
                    Text(
                        text = it,
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.accent,
                        modifier = Modifier
                            .background(PFTheme.colors.accentSoft, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            subtitle?.let {
                Text(text = it, style = PFTheme.type.caption, color = PFTheme.colors.onSurfaceMuted)
            }
        }
        if (selected) {
            Icon(
                imageVector = PFIcons.Check,
                contentDescription = null,
                tint = PFTheme.colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
