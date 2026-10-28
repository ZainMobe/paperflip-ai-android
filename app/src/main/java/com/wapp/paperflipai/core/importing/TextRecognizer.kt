package com.wapp.paperflipai.core.importing

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR with ML Kit — the Android counterpart of
 * `TextRecognizer.swift` (Apple Vision). The Play Services ("unbundled")
 * models are used, so the APK does not grow; Play downloads them on first
 * use. Latin and Devanagari run side by side and the richer result wins,
 * which covers English, French, Spanish and Hindi notes.
 *
 * ML Kit has no Arabic or Urdu model; those pages come back empty and the
 * screen tells the user to type or paste instead.
 */
object TextRecognizer {

    private val latin by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val devanagari by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    suspend fun recognise(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return coroutineScope {
            val latinResult = async { runCatching { latin.process(image).await() }.getOrNull() }
            val devanagariResult = async { runCatching { devanagari.process(image).await() }.getOrNull() }
            val candidates = listOfNotNull(latinResult.await(), devanagariResult.await())
            candidates
                .map { toParagraphs(it) }
                .maxByOrNull { text -> text.count { it.isLetterOrDigit() } }
                .orEmpty()
        }
    }

    /**
     * Lines inside a block flow together; a line ending a sentence closes
     * the paragraph. Same rule as the iOS joiner, so both platforms send the
     * generator the same shape of text.
     */
    private fun toParagraphs(result: Text): String {
        val paragraphs = ArrayList<String>()
        for (block in result.textBlocks) {
            val current = StringBuilder()
            for (line in block.lines) {
                val text = line.text.trim()
                if (text.isEmpty()) continue
                if (current.isNotEmpty()) current.append(' ')
                current.append(text)
                if (text.last() in ".!?:") {
                    paragraphs += current.toString()
                    current.clear()
                }
            }
            if (current.isNotEmpty()) paragraphs += current.toString()
        }
        return paragraphs.joinToString("\n")
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
