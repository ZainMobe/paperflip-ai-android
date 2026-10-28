package com.wapp.paperflipai.core.audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Speech-to-text for a recorded or imported lecture, on the device.
 * Counterpart of `AudioTranscriber.swift`.
 *
 * Android's recogniser cannot open a file, but since API 33 (this app's
 * minSdk) it accepts a PCM stream through `EXTRA_AUDIO_SOURCE` and a
 * "segmented session" that runs until the stream ends, which is what makes
 * hour-long audio possible. The PCM file is written into a pipe on an IO
 * thread; progress is how much of it has been written.
 *
 * The on-device recogniser is preferred (offline, free, private). If the
 * device has none, the default recognition service is tried with the same
 * stream. If the language pack is missing, a download is requested and the
 * user is asked to try again once it finishes.
 */
object AudioTranscriber {

    class TranscribeError(val reason: Reason) : Exception(reason.name) {
        enum class Reason { Unavailable, LanguagePackDownloading, LanguageNotSupported, NoSpeech, Failed }
    }

    /** Must be called from the main thread (SpeechRecognizer requirement). */
    suspend fun transcribe(
        context: Context,
        pcm: File,
        languageTag: String,
        scope: CoroutineScope,
        onProgress: (Float) -> Unit,
    ): String = withContext(Dispatchers.Main) {
        val onDevice = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if (!onDevice && !SpeechRecognizer.isRecognitionAvailable(context)) {
            throw TranscribeError(TranscribeError.Reason.Unavailable)
        }
        val recognizer = if (onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        else SpeechRecognizer.createSpeechRecognizer(context)

        val pipe = ParcelFileDescriptor.createPipe()
        val readEnd = pipe[0]
        val writeEnd = pipe[1]

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readEnd)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, AudioDecoder.TARGET_RATE)
            putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
            putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        var writer: Job? = null
        try {
            suspendCancellableCoroutine { cont ->
                val segments = ArrayList<String>()
                var finished = false
                fun finish(result: Result<String>) {
                    if (finished) return
                    finished = true
                    result.fold({ cont.resume(it) }, { cont.resumeWithException(it) })
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onSegmentResults(segmentResults: Bundle) {
                        segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { segments += it }
                    }

                    override fun onEndOfSegmentedSession() {
                        val text = segments.joinToString(" ")
                        finish(
                            if (text.isBlank()) Result.failure(TranscribeError(TranscribeError.Reason.NoSpeech))
                            else Result.success(text)
                        )
                    }

                    // Services that ignore the segmented flag deliver one final result.
                    override fun onResults(results: Bundle) {
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { segments += it }
                        val text = segments.joinToString(" ")
                        finish(
                            if (text.isBlank()) Result.failure(TranscribeError(TranscribeError.Reason.NoSpeech))
                            else Result.success(text)
                        )
                    }

                    override fun onError(error: Int) {
                        val partial = segments.joinToString(" ")
                        val reason = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                if (partial.isNotBlank()) { finish(Result.success(partial)); return }
                                TranscribeError.Reason.NoSpeech
                            }
                            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                                if (onDevice) runCatching { recognizer.triggerModelDownload(intent) }
                                TranscribeError.Reason.LanguagePackDownloading
                            }
                            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
                                TranscribeError.Reason.LanguageNotSupported
                            else -> TranscribeError.Reason.Failed
                        }
                        finish(Result.failure(TranscribeError(reason)))
                    }

                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                recognizer.startListening(intent)

                // Feed the PCM into the pipe; closing the write end ends the session.
                writer = scope.launch(Dispatchers.IO) {
                    val total = pcm.length().coerceAtLeast(1L)
                    var written = 0L
                    runCatching {
                        ParcelFileDescriptor.AutoCloseOutputStream(writeEnd).use { out ->
                            FileInputStream(pcm).use { input ->
                                val buffer = ByteArray(32 * 1024)
                                while (isActive) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    out.write(buffer, 0, read)
                                    written += read
                                    onProgress((written.toFloat() / total).coerceIn(0f, 0.99f))
                                }
                            }
                        }
                    }
                }

                cont.invokeOnCancellation {
                    writer?.cancel()
                    runCatching { recognizer.cancel() }
                }
            }
        } finally {
            writer?.cancel()
            runCatching { recognizer.destroy() }
            runCatching { readEnd.close() }
            runCatching { writeEnd.close() }
            onProgress(1f)
        }
    }
}
