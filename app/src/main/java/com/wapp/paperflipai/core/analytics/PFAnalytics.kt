package com.wapp.paperflipai.core.analytics

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.wapp.paperflipai.core.monetization.Entitlement

/** Firebase Analytics + Crashlytics funnel — the Android counterpart of `PFAnalytics.swift`. */
object PFAnalytics {
    private var analytics: FirebaseAnalytics? = null
    private val crashlytics: FirebaseCrashlytics? get() = runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    private val screenStack = mutableListOf<String>()
    private val currentScreen: String? get() = screenStack.lastOrNull()
    private var screenEnteredAt = System.currentTimeMillis()
    private var sessionStartedAt = System.currentTimeMillis()

    fun init(context: Context) {
        analytics = runCatching { FirebaseAnalytics.getInstance(context.applicationContext) }.getOrNull()
    }

    fun log(event: AnalyticsEvent) {
        analytics?.logEvent(event.name, event.params.toBundle())
        crashlytics?.log("${event.name} ${event.params}")
    }

    fun screen(name: String, screenClass: String = name) {
        if (name == currentScreen) return
        screenStack.remove(name)
        screenStack.add(name)
        screenEnteredAt = System.currentTimeMillis()
        crashlytics?.setCustomKey("screen", name)
        analytics?.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to name,
                FirebaseAnalytics.Param.SCREEN_CLASS to screenClass,
            ).toBundle(),
        )
    }

    fun screenLeft(name: String) {
        screenStack.remove(name)
    }

    fun error(throwable: Throwable, context: String, extra: Map<String, Any?> = emptyMap()) {
        log(
            AnalyticsEvent(
                "app_error",
                extra + mapOf(
                    "context" to context,
                    "error_domain" to throwable::class.java.simpleName,
                    "error_code" to 0,
                    "error_message" to (throwable.message ?: throwable.toString()),
                    "screen" to (currentScreen ?: "none"),
                ),
            )
        )
        crashlytics?.setCustomKey("error_context", context)
        crashlytics?.recordException(throwable)
    }

    fun errorShown(title: String, message: String?) {
        log(
            AnalyticsEvent(
                "error_shown",
                mapOf("title" to title, "message" to message.orEmpty(), "screen" to (currentScreen ?: "none")),
            )
        )
    }

    fun setUser(id: String?) {
        analytics?.setUserId(id)
        crashlytics?.setUserId(id.orEmpty())
    }

    fun setPlan(entitlement: Entitlement) {
        analytics?.setUserProperty("plan", entitlement.raw)
        analytics?.setUserProperty("tier", if (entitlement.isActive) "pro" else "free")
        crashlytics?.setCustomKey("plan", entitlement.raw)
    }

    fun setUserProperty(value: String?, name: String) {
        analytics?.setUserProperty(name, value)
    }

    fun appForegrounded() {
        sessionStartedAt = System.currentTimeMillis()
        screenEnteredAt = sessionStartedAt
        log(AnalyticsEvent("app_foreground", mapOf("screen" to (currentScreen ?: "none"))))
    }

    fun appBackgrounded() {
        log(
            AnalyticsEvent(
                "app_background",
                mapOf(
                    "last_screen" to (currentScreen ?: "none"),
                    "screen_seconds" to secondsSince(screenEnteredAt),
                    "session_seconds" to secondsSince(sessionStartedAt),
                ),
            )
        )
    }

    fun secondsSince(millis: Long): Int =
        ((System.currentTimeMillis() - millis) / 1000).toInt().coerceAtLeast(0)

    private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                null -> putString(key, "none")
                is String -> putString(key, value.take(100))
                is Boolean -> putLong(key, if (value) 1L else 0L)
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Float -> putDouble(key, value.toDouble())
                else -> putString(key, value.toString().take(100))
            }
        }
    }
}

/** Logs a screen_view while in composition — the Android counterpart of `.pfTrackScreen(_:)`. */
@Composable
fun TrackScreen(name: String, screenClass: String = name) {
    DisposableEffect(name) {
        PFAnalytics.screen(name, screenClass)
        onDispose { PFAnalytics.screenLeft(name) }
    }
}
