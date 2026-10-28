package com.wapp.paperflipai.feature.importer

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.wapp.paperflipai.R
import com.wapp.paperflipai.core.importing.SourceLimits
import com.wapp.paperflipai.core.importing.TextRecognizer
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFIndeterminateRing
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * "From a photo": snap a page with the camera or pick up to five from the
 * gallery; text is recognised on the device (ML Kit) and shown in an
 * editable box before generating. Counterpart of `PhotoScanInput.swift`.
 *
 * The camera goes through the system camera app (ACTION_IMAGE_CAPTURE), so
 * the app needs no CAMERA permission. The gallery uses the Photo Picker,
 * which needs no storage permission either.
 */
@Composable
internal fun PhotoScanInput(
    recognisedText: String,
    onRecognisedTextChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()
    val tint = PFTheme.colors.info

    val pages = remember { mutableStateListOf<ScannedPage>() }
    var isRecognising by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val noTextMessage = stringResource(R.string.photo_no_text_found)
    val failedMessage = stringResource(R.string.photo_read_failed)

    fun rebuild() {
        onRecognisedTextChange(
            pages.joinToString("\n\n") { it.text }.take(SourceLimits.MAX_SOURCE_CHARACTERS)
        )
    }

    fun add(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            isRecognising = true
            scanError = null
            var anyEmpty = false
            for (uri in uris) {
                if (pages.size >= MAX_PAGES) break
                val text = runCatching { TextRecognizer.recognise(context, uri) }.getOrNull()
                if (text == null) {
                    scanError = failedMessage
                    PFAnalytics.log(AnalyticsEvent.importFailed("photo", "ocr", "recognition_failed"))
                    haptics.perform(PFHaptic.Error)
                } else {
                    if (text.isBlank()) anyEmpty = true
                    pages += ScannedPage(uri, text)
                }
            }
            rebuild()
            isRecognising = false
            if (anyEmpty) {
                scanError = noTextMessage
                PFAnalytics.log(AnalyticsEvent.importFailed("photo", "ocr", "no_text"))
                haptics.perform(PFHaptic.Warning)
            } else if (scanError == null) {
                haptics.perform(PFHaptic.Success)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PAGES),
    ) { uris -> add(uris.take(MAX_PAGES - pages.size)) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) add(listOf(uri))
    }

    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
        val canAdd = pages.size < MAX_PAGES && !isRecognising
        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
            ImportActionTile(
                title = stringResource(R.string.photo_take),
                icon = PFIcons.Camera,
                tint = tint,
                enabled = canAdd,
                onClick = {
                    val uri = newCameraUri(context)
                    pendingCameraUri = uri
                    camera.launch(uri)
                },
            )
            ImportActionTile(
                title = stringResource(R.string.photo_choose),
                icon = PFIcons.PhotoLibrary,
                tint = tint,
                enabled = canAdd,
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }

        if (pages.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEach { page ->
                    PageThumbnail(page = page, onRemove = {
                        haptics.perform(PFHaptic.Light)
                        pages.remove(page)
                        rebuild()
                    })
                }
                Text(
                    text = "${pages.size}/$MAX_PAGES",
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                    modifier = Modifier.padding(start = PFTheme.spacing.xs),
                )
            }
        }

        if (isRecognising) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PFIndeterminateRing(size = 16.dp, lineWidth = 2.dp, tint = tint)
                Spacer(Modifier.width(PFTheme.spacing.sm))
                Text(
                    text = stringResource(R.string.photo_reading),
                    style = PFTheme.type.footnote,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }

        PFErrorBanner(scanError)

        if (pages.isNotEmpty() || recognisedText.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                ImportSectionLabel(
                    title = stringResource(R.string.photo_recognised_text),
                    trailing = stringResource(R.string.photo_tap_to_fix),
                )
                ImportTextEditor(
                    value = recognisedText,
                    onValueChange = { onRecognisedTextChange(it.take(SourceLimits.MAX_SOURCE_CHARACTERS)) },
                    placeholder = "",
                    accessibilityLabel = stringResource(R.string.photo_recognised_text),
                    accent = tint,
                    minHeight = 160.dp,
                    maxHeight = 320.dp,
                )
                if (recognisedText.isNotEmpty() &&
                    recognisedText.trim().length < SourceLimits.MIN_SOURCE_CHARACTERS
                ) {
                    Text(
                        text = stringResource(R.string.photo_need_more_text),
                        style = PFTheme.type.caption,
                        color = PFTheme.colors.warning,
                    )
                }
            }
        }
    }
}

private data class ScannedPage(val uri: Uri, val text: String)

private const val MAX_PAGES = 5

@Composable
private fun PageThumbnail(page: ScannedPage, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, page.uri) {
        value = withContext(Dispatchers.IO) { loadThumbnail(context, page.uri) }
    }
    val shape = RoundedCornerShape(PFRadius.md)
    Box {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 96.dp)
                .clip(shape)
                .background(PFTheme.colors.elevated)
                .border(0.7.dp, PFTheme.colors.border, shape),
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .pfPressable(onClick = onRemove, pressedScale = 0.9f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PFIcons.Close,
                contentDescription = stringResource(R.string.photo_remove),
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun loadThumbnail(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val longest = max(info.size.width, info.size.height).coerceAtLeast(1)
        val scale = 300f / longest
        if (scale < 1f) {
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    }.asImageBitmap()
}.getOrNull()

/** A cache file the system camera writes into, shared through FileProvider. */
private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    val file = File(dir, "scan-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
