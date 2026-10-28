package com.wapp.paperflipai.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Reads card text aloud with the system TextToSpeech engine. Counterpart
 * of `SpeechReader.swift`. The voice language comes from the text itself,
 * so an Urdu card is read in Urdu inside an English deck: the script
 * decides for Arabic-script and Devanagari text, and the platform's
 * on-device language detector decides between Latin-script languages.
 */
class SpeechReader(private val context: Context, private val scope: CoroutineScope) {

    private val _speakingId = MutableStateFlow<String?>(null)
    /** Id passed to [toggle] for the utterance being spoken, or null. */
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, String>? = null

    private fun engine(): TextToSpeech = tts ?: TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) pending?.let { (text, id) -> pending = null; speak(text, id) }
        else _speakingId.value = null
    }.also { engine ->
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (_speakingId.value == utteranceId) _speakingId.value = null
            }
            @Deprecated("Required override on older engines")
            override fun onError(utteranceId: String?) {
                if (_speakingId.value == utteranceId) _speakingId.value = null
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (_speakingId.value == utteranceId) _speakingId.value = null
            }
        })
    }

    /** Speaks [text]; calling again with the same [id] while speaking stops it. */
    fun toggle(text: String, id: String) {
        if (_speakingId.value == id) {
            stop()
            return
        }
        speak(text, id)
    }

    private fun speak(text: String, id: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val engine = engine()
        _speakingId.value = id
        if (!ready) {
            pending = trimmed to id
            return
        }
        scope.launch {
            val locale = detectLocale(trimmed)
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.getDefault())
            }
            engine.setSpeechRate(0.95f)
            engine.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, id)
        }
    }

    fun stop() {
        tts?.stop()
        pending = null
        _speakingId.value = null
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }

    private suspend fun detectLocale(text: String): Locale {
        scriptLocale(text)?.let { return it }
        // TextClassifier must not run on the main thread.
        val tag = withContext(Dispatchers.Default) {
            runCatching {
                val manager = context.getSystemService(TextClassificationManager::class.java)
                val result = manager.textClassifier.detectLanguage(TextLanguage.Request.Builder(text).build())
                if (result.localeHypothesisCount > 0) result.getLocale(0).toLanguageTag() else null
            }.getOrNull()
        }
        return tag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
    }

    companion object {
        // Letters used in Urdu but not in standard Arabic.
        private val urduOnly = setOf('ٹ', 'ڈ', 'ڑ', 'ں', 'ے', 'ھ', 'ہ', 'ی', 'گ', 'چ', 'پ', 'ژ')

        /** Decides by script where the script is unambiguous enough. */
        fun scriptLocale(text: String): Locale? {
            var arabicScript = 0
            var devanagari = 0
            var urdu = 0
            for (ch in text) {
                when (Character.UnicodeBlock.of(ch)) {
                    Character.UnicodeBlock.ARABIC,
                    Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
                    Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B -> {
                        arabicScript += 1
                        if (ch in urduOnly) urdu += 1
                    }
                    Character.UnicodeBlock.DEVANAGARI -> devanagari += 1
                    else -> Unit
                }
            }
            return when {
                devanagari > arabicScript && devanagari > 0 -> Locale.forLanguageTag("hi-IN")
                arabicScript > 0 && urdu > 0 -> Locale.forLanguageTag("ur-PK")
                arabicScript > 0 -> Locale.forLanguageTag("ar")
                else -> null
            }
        }
    }
}
