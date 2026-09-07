package com.wapp.paperflipai

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wapp.paperflipai.app.AppEnvironment
import com.wapp.paperflipai.app.IntentInbox
import com.wapp.paperflipai.app.PFAction
import com.wapp.paperflipai.app.PaperflipApp

/**
 * The single activity. Everything above it is Jetpack Compose.
 *
 * It also plays the role `Paperflip_AIApp.onOpenURL` plays on iOS: deep
 * links, notification taps, widget taps and share-sheet hand-offs are all
 * translated into a [PFAction] and dropped in the [IntentInbox] for the
 * navigation layer to pick up.
 */
class MainActivity : ComponentActivity() {

    private val environment: AppEnvironment
        get() = (application as PaperflipApplication).environment

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyStoredLocale()
        handleIntent(intent)
        setContent { PaperflipApp(environment) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        environment.notifications.refreshAuthorization()
        environment.refreshWidget()
    }

    // ── Incoming intents ──────────────────────────────────────────────

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                IntentInbox.actionFor(intent.dataString)?.let(IntentInbox::post)
            }

            Intent.ACTION_SEND -> when {
                intent.type == "application/pdf" -> {
                    val uri = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    uri?.let { IntentInbox.post(PFAction.ImportSharedPdf(it.toString())) }
                }

                intent.type?.startsWith("text/") == true -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                    if (!text.isNullOrEmpty()) {
                        val link = text.lineSequence()
                            .map { it.trim() }
                            .firstOrNull { it.startsWith("http", ignoreCase = true) }
                        val action = link?.let { IntentInbox.actionFor(it) }
                        IntentInbox.post(action ?: PFAction.ImportSharedText(text))
                    }
                }
            }
        }
    }

    // ── Per-app language ──────────────────────────────────────────────

    /**
     * Applies the language chosen in Settings. Uses the platform
     * LocaleManager (API 33+, which is our minSdk) so the choice also shows
     * up in the system's per-app language screen.
     */
    private fun applyStoredLocale() {
        val tag = environment.settings.language.value.localeTag
        val manager = getSystemService(LocaleManager::class.java) ?: return
        val desired = if (tag.isEmpty()) LocaleList.getEmptyLocaleList()
        else LocaleList.forLanguageTags(tag)
        if (manager.applicationLocales != desired) {
            manager.applicationLocales = desired
        }
    }
}
