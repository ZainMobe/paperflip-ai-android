package com.wapp.paperflipai.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Lecture capture for "From a lecture" — the Android counterpart of
 * `AudioRecorder.swift`.
 *
 * Records raw 16 kHz mono 16-bit PCM rather than AAC: that is exactly the
 * format [AudioTranscriber] feeds the speech recogniser, so a recording
 * needs no decode step. An hour is about 115 MB in the cache directory and
 * is deleted as soon as it has been transcribed. Capped at two hours.
 *
 * The caller must hold RECORD_AUDIO before [start].
 */
class AudioRecorder(private val context: Context) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    var file: File? = null
        private set

    private var job: Job? = null

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns false when the microphone could not be opened. */
    @SuppressLint("MissingPermission") // checked by hasPermission(); callers gate on it
    fun start(scope: CoroutineScope): Boolean {
        if (_isRecording.value || !hasPermission()) return false
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) return false
        val bufferSize = minBuffer * 4
        val record = runCatching {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, CHANNEL, ENCODING, bufferSize)
        }.getOrNull() ?: return false
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }

        val out = File(context.cacheDir, "lecture-${System.currentTimeMillis()}.pcm")
        file = out
        _elapsedMillis.value = 0L
        _isRecording.value = true
        record.startRecording()

        job = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferSize)
            var bytesWritten = 0L
            try {
                FileOutputStream(out).use { stream ->
                    while (isActive && _isRecording.value && bytesWritten < MAX_BYTES) {
                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            stream.write(buffer, 0, read)
                            bytesWritten += read
                            _elapsedMillis.value = bytesWritten * 1000L / BYTES_PER_SECOND
                        } else if (read < 0) {
                            break
                        }
                    }
                }
            } finally {
                runCatching { record.stop() }
                record.release()
                _isRecording.value = false
            }
        }
        return true
    }

    /** Stops and returns the finished file, or null if nothing was captured. */
    suspend fun stop(): File? {
        _isRecording.value = false
        job?.join()
        job = null
        return file?.takeIf { it.exists() && it.length() > 0 }
    }

    fun discard() {
        _isRecording.value = false
        job?.cancel()
        job = null
        file?.delete()
        file = null
        _elapsedMillis.value = 0L
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_SECOND = SAMPLE_RATE * 2L
        private const val MAX_BYTES = BYTES_PER_SECOND * 2 * 60 * 60
    }
}
