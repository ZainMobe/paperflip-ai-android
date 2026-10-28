package com.wapp.paperflipai.feature.importer

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapp.paperflipai.R
import com.wapp.paperflipai.app.LocalAppEnvironment
import com.wapp.paperflipai.core.audio.AudioDecoder
import com.wapp.paperflipai.core.audio.AudioRecorder
import com.wapp.paperflipai.core.audio.AudioTranscriber
import com.wapp.paperflipai.core.importing.SourceLimits
import com.wapp.paperflipai.designsystem.PFIcons
import com.wapp.paperflipai.designsystem.component.PFButton
import com.wapp.paperflipai.designsystem.component.PFButtonVariant
import com.wapp.paperflipai.designsystem.component.PFErrorBanner
import com.wapp.paperflipai.designsystem.component.PFIndeterminateRing
import com.wapp.paperflipai.designsystem.modifier.PFHaptic
import com.wapp.paperflipai.designsystem.modifier.pfPressable
import com.wapp.paperflipai.designsystem.modifier.rememberPFHaptics
import com.wapp.paperflipai.designsystem.theme.PFRadius
import com.wapp.paperflipai.designsystem.theme.PFTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * "From a lecture": record now or open an audio file, transcribe on the
 * device, then edit the transcript before generating. Counterpart of
 * `AudioSourceInput.swift`. Recording only runs while this screen is open,
 * exactly as on iOS.
 */
@Composable
internal fun AudioSourceInput(
    transcript: String,
    onTranscriptChange: (String) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPFHaptics()
    val tint = PFTheme.colors.hard

    val recorder = remember { AudioRecorder(context.applicationContext) }
    val isRecording by recorder.isRecording.collectAsStateWithLifecycle()
    val elapsed by recorder.elapsedMillis.collectAsStateWithLifecycle()

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf<String?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isTranscribing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    val micDenied = stringResource(R.string.audio_mic_denied)
    val micFailed = stringResource(R.string.audio_mic_failed)
    val errUnavailable = stringResource(R.string.audio_err_unavailable)
    val errDownloading = stringResource(R.string.audio_err_language_downloading)
    val errNotSupported = stringResource(R.string.audio_err_language_not_supported)
    val errNoSpeech = stringResource(R.string.audio_err_no_speech)
    val errFailed = stringResource(R.string.audio_err_failed)
    val errDecode = stringResource(R.string.audio_err_decode)

    // Stop the mic if the user leaves mid-recording.
    DisposableEffect(Unit) { onDispose { if (recorder.isRecording.value) recorder.discard() } }

    fun transcribe(pcm: File, deleteAfter: Boolean, decodeShare: Float) {
        scope.launch {
            isTranscribing = true
            error = null
            try {
                val tag = env.settings.language.value.localeTag
                    .ifEmpty { Locale.getDefault().toLanguageTag() }
                val text = AudioTranscriber.transcribe(
                    context = context,
                    pcm = pcm,
                    languageTag = tag,
                    scope = scope,
                    onProgress = { p -> progress = decodeShare + p * (1f - decodeShare) },
                )
                onTranscriptChange(text.take(SourceLimits.MAX_SOURCE_CHARACTERS))
                haptics.perform(PFHaptic.Success)
            } catch (failure: AudioTranscriber.TranscribeError) {
                PFAnalytics.log(AnalyticsEvent.importFailed("audio", "transcribe", failure.reason.name))
                error = when (failure.reason) {
                    AudioTranscriber.TranscribeError.Reason.Unavailable -> errUnavailable
                    AudioTranscriber.TranscribeError.Reason.LanguagePackDownloading -> errDownloading
                    AudioTranscriber.TranscribeError.Reason.LanguageNotSupported -> errNotSupported
                    AudioTranscriber.TranscribeError.Reason.NoSpeech -> errNoSpeech
                    AudioTranscriber.TranscribeError.Reason.Failed -> errFailed
                }
                haptics.perform(PFHaptic.Error)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = errFailed
                PFAnalytics.error(failure, "audio_transcribe")
                haptics.perform(PFHaptic.Error)
            } finally {
                isTranscribing = false
                if (deleteAfter) pcm.delete()
            }
        }
    }

    fun transcribePicked() {
        val uri = pickedUri ?: return
        scope.launch {
            isTranscribing = true
            progress = 0f
            error = null
            val pcm = File(context.cacheDir, "import-${System.currentTimeMillis()}.pcm")
            val decoded = runCatching {
                AudioDecoder.decodeToPcm(context, uri, pcm) { p -> progress = p * 0.3f }
            }.isSuccess
            if (!decoded) {
                isTranscribing = false
                error = errDecode
                haptics.perform(PFHaptic.Error)
                return@launch
            }
            transcribe(pcm, deleteAfter = true, decodeShare = 0.3f)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (!recorder.start(scope)) error = micFailed
        } else {
            error = micDenied
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedUri = uri
            pickedName = context.queryFileName(uri)
            recordedFile = null
            error = null
            haptics.perform(PFHaptic.Light)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
        when {
            isRecording -> RecordingCard(
                elapsedMillis = elapsed,
                onDiscard = { recorder.discard() },
                onStop = {
                    scope.launch {
                        val file = recorder.stop() ?: return@launch
                        recordedFile = file
                        pickedName = context.getString(R.string.audio_recording_name, formatElapsed(elapsed))
                        transcribe(file, deleteAfter = true, decodeShare = 0f)
                    }
                },
            )
            isTranscribing -> Unit
            transcript.isEmpty() && pickedUri != null -> FileChip(
                name = pickedName.orEmpty(),
                onTranscribe = { transcribePicked() },
            )
            transcript.isEmpty() -> Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
                ImportActionTile(
                    title = stringResource(R.string.audio_record),
                    icon = PFIcons.Record,
                    tint = PFTheme.colors.danger,
                    onClick = {
                        error = null
                        if (recorder.hasPermission()) {
                            if (!recorder.start(scope)) error = micFailed
                        } else {
                            permission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                )
                ImportActionTile(
                    title = stringResource(R.string.audio_open_file),
                    icon = PFIcons.AudioFile,
                    tint = tint,
                    onClick = { filePicker.launch(arrayOf("audio/*")) },
                )
            }
        }

        if (isTranscribing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PFTheme.colors.elevated, RoundedCornerShape(PFRadius.lg))
                    .padding(PFTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PFIndeterminateRing(size = 16.dp, lineWidth = 2.dp, tint = tint)
                    Spacer(Modifier.width(PFTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.audio_transcribing),
                        style = PFTheme.type.footnoteBold,
                        color = PFTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (progress > 0f) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = PFTheme.type.caption,
                            color = PFTheme.colors.onSurfaceMuted,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress.coerceAtLeast(0.02f) },
                    color = tint,
                    trackColor = PFTheme.colors.divider,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Text(
                    text = stringResource(R.string.audio_transcribing_hint),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceMuted,
                )
            }
        }

        PFErrorBanner(error)

        if (transcript.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.audio_transcript),
                        style = PFTheme.type.overline,
                        color = PFTheme.colors.onSurfaceMuted,
                        modifier = Modifier.weight(1f),
                    )
                    MiniChip(
                        icon = PFIcons.Undo,
                        label = stringResource(R.string.audio_start_over),
                        muted = true,
                        onClick = {
                            onTranscriptChange("")
                            pickedUri = null
                            pickedName = null
                            recordedFile = null
                            error = null
                        },
                    )
                }
                ImportTextEditor(
                    value = transcript,
                    onValueChange = { onTranscriptChange(it.take(SourceLimits.MAX_SOURCE_CHARACTERS)) },
                    placeholder = "",
                    accessibilityLabel = stringResource(R.string.audio_transcript),
                    accent = tint,
                    minHeight = 180.dp,
                    maxHeight = 340.dp,
                )
                Text(
                    text = stringResource(
                        R.string.audio_word_count,
                        transcript.split(Regex("""\s+""")).count { it.isNotEmpty() },
                    ),
                    style = PFTheme.type.caption,
                    color = PFTheme.colors.onSurfaceFaint,
                )
            }
        }
    }
}

@Composable
private fun RecordingCard(elapsedMillis: Long, onDiscard: () -> Unit, onStop: () -> Unit) {
    val shape = RoundedCornerShape(PFRadius.xl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(1.5.dp, PFTheme.colors.danger.copy(alpha = 0.4f), shape)
            .padding(PFTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(PFTheme.spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val blinkOn = (elapsedMillis / 500) % 2 == 0L
            Box(
                Modifier
                    .size(10.dp)
                    .background(PFTheme.colors.danger.copy(alpha = if (blinkOn) 1f else 0.3f), CircleShape)
            )
            Spacer(Modifier.width(PFTheme.spacing.sm))
            Text(
                text = stringResource(R.string.audio_recording),
                style = PFTheme.type.footnoteBold,
                color = PFTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatElapsed(elapsedMillis),
                style = PFTheme.type.title3,
                color = PFTheme.colors.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.audio_recording_hint),
            style = PFTheme.type.caption,
            color = PFTheme.colors.onSurfaceMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PFTheme.spacing.md)) {
            PFButton(
                title = stringResource(R.string.audio_discard),
                onClick = onDiscard,
                variant = PFButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            )
            PFButton(
                title = stringResource(R.string.audio_stop_and_transcribe),
                onClick = onStop,
                leadingIcon = PFIcons.Stop,
                modifier = Modifier.weight(1.6f),
            )
        }
    }
}

@Composable
private fun FileChip(name: String, onTranscribe: () -> Unit) {
    val tint = PFTheme.colors.hard
    val shape = RoundedCornerShape(PFRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PFTheme.colors.elevated, shape)
            .border(0.7.dp, PFTheme.colors.border, shape)
            .padding(PFTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(PFRadius.md)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PFIcons.Waveform, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(PFTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = name, style = PFTheme.type.bodyEmphasis, color = PFTheme.colors.onSurface, maxLines = 1)
            Text(
                text = stringResource(R.string.audio_ready_to_transcribe),
                style = PFTheme.type.caption,
                color = PFTheme.colors.onSurfaceMuted,
            )
        }
        Text(
            text = stringResource(R.string.audio_transcribe),
            style = PFTheme.type.footnoteBold,
            color = PFTheme.colors.accent,
            modifier = Modifier
                .background(PFTheme.colors.accentSoft, CircleShape)
                .pfPressable(onClick = onTranscribe, pressedScale = 0.96f)
                .padding(horizontal = PFTheme.spacing.md, vertical = 6.dp),
        )
    }
}

private fun formatElapsed(millis: Long): String {
    val total = millis / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun android.content.Context.queryFileName(uri: Uri): String {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "audio"
    runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index) ?: name
        }
    }
    return name
}
