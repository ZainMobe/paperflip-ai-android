package com.wapp.paperflipai.core.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder

/**
 * Decodes any audio file the platform can read (m4a, mp3, wav, ogg, …) to
 * raw 16 kHz mono 16-bit PCM, the format [AudioTranscriber] feeds the
 * speech recogniser. Channels are averaged; the sample rate is converted
 * with linear interpolation, which is plenty for speech.
 *
 * No iOS counterpart: SFSpeechRecognizer reads files directly, Android's
 * recogniser only takes a PCM stream.
 */
object AudioDecoder {

    class DecodeError(message: String) : Exception(message)

    suspend fun decodeToPcm(
        context: Context,
        uri: Uri,
        out: File,
        onProgress: (Float) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw DecodeError("no audio track")
            extractor.selectTrack(track)
            val inputFormat = extractor.getTrackFormat(track)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw DecodeError("no mime")
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION))
                inputFormat.getLong(MediaFormat.KEY_DURATION) else 0L

            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            var sourceRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            // Streaming linear resampler state (positions in source frames).
            var sourceFrame = 0L
            var nextOutput = 0.0
            var previous = 0f
            var step = sourceRate / TARGET_RATE.toDouble()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            BufferedOutputStream(FileOutputStream(out), 64 * 1024).use { stream ->
                val bytes = ByteArray(2)
                fun write(sample: Float) {
                    val value = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
                    bytes[0] = (value and 0xFF).toByte()
                    bytes[1] = ((value shr 8) and 0xFF).toByte()
                    stream.write(bytes)
                }

                while (!outputDone) {
                    ensureActive()
                    if (!inputDone) {
                        val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val buffer = decoder.getInputBuffer(inIndex) ?: throw DecodeError("input buffer")
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = decoder.outputFormat
                            sourceRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                pcmEncoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            }
                            step = sourceRate / TARGET_RATE.toDouble()
                        }
                        outIndex >= 0 -> {
                            val buffer = decoder.getOutputBuffer(outIndex)
                            if (buffer != null && info.size > 0) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                buffer.order(ByteOrder.nativeOrder())
                                val isFloat = pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT
                                val samples = if (isFloat) info.size / 4 else info.size / 2
                                val frames = samples / channels.coerceAtLeast(1)
                                val floats = if (isFloat) buffer.asFloatBuffer() else null
                                val shorts = if (!isFloat) buffer.asShortBuffer() else null
                                for (f in 0 until frames) {
                                    var sum = 0f
                                    for (c in 0 until channels) {
                                        val i = f * channels + c
                                        sum += floats?.get(i) ?: (shorts!!.get(i) / 32768f)
                                    }
                                    val current = sum / channels
                                    // Emit every output sample that falls between
                                    // the previous source frame and this one.
                                    while (nextOutput <= sourceFrame) {
                                        val fraction = (nextOutput - (sourceFrame - 1)).toFloat().coerceIn(0f, 1f)
                                        write(previous + (current - previous) * fraction)
                                        nextOutput += step
                                    }
                                    previous = current
                                    sourceFrame += 1
                                }
                                if (durationUs > 0) {
                                    onProgress((info.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f))
                                }
                            }
                            decoder.releaseOutputBuffer(outIndex, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
            }
            onProgress(1f)
        } catch (error: DecodeError) {
            out.delete()
            throw error
        } catch (error: Exception) {
            out.delete()
            if (error is kotlinx.coroutines.CancellationException) throw error
            throw DecodeError(error.message ?: "decode failed")
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    const val TARGET_RATE = 16_000
    private const val TIMEOUT_US = 10_000L
}
