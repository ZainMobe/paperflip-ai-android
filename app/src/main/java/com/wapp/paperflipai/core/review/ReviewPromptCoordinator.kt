package com.wapp.paperflipai.core.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.wapp.paperflipai.core.analytics.AnalyticsEvent
import com.wapp.paperflipai.core.analytics.PFAnalytics

/**
 * Decides when to ask for a Play Store rating. Counterpart of
 * `ReviewPromptCoordinator.swift`, same rules: after the second real
 * success (a deck saved, a quiz passed), never on the first day, at most
 * once every 30 days and three times a year. Screens call [recordSuccess];
 * the root host watches [pendingPrompt] and calls [launch] a moment later
 * so the sheet appears on the next screen, not over the save button.
 *
 * Play enforces its own quota on top of this and may silently show
 * nothing, which is expected and why no "thanks" UI follows the call.
 */
class ReviewPromptCoordinator(context: Context) {

    private val prefs = context.getSharedPreferences("review_prompt", Context.MODE_PRIVATE)

    private val _pendingPrompt = MutableStateFlow(false)
    val pendingPrompt: StateFlow<Boolean> = _pendingPrompt.asStateFlow()

    init {
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    fun recordSuccess() {
        val count = prefs.getInt(KEY_SUCCESSES, 0) + 1
        prefs.edit().putInt(KEY_SUCCESSES, count).apply()
        if (shouldPrompt(count)) _pendingPrompt.value = true
    }

    fun launch(activity: Activity) {
        if (!_pendingPrompt.value) return
        markPrompted()
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                PFAnalytics.log(AnalyticsEvent.reviewPromptShown)
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }

    private fun markPrompted() {
        _pendingPrompt.value = false
        val now = System.currentTimeMillis()
        val dates = prefs.getStringSet(KEY_PROMPT_DATES, emptySet()).orEmpty() + now.toString()
        prefs.edit()
            .putLong(KEY_LAST_PROMPT, now)
            .putStringSet(KEY_PROMPT_DATES, dates)
            .apply()
    }

    private fun shouldPrompt(successes: Int): Boolean {
        if (successes < 2) return false
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_FIRST_LAUNCH, now) < DAY) return false
        val last = prefs.getLong(KEY_LAST_PROMPT, 0L)
        if (last > 0 && now - last < 30 * DAY) return false
        val lastYear = prefs.getStringSet(KEY_PROMPT_DATES, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .count { now - it < 365 * DAY }
        return lastYear < 3
    }

    private companion object {
        const val KEY_SUCCESSES = "successes"
        const val KEY_FIRST_LAUNCH = "first_launch_at"
        const val KEY_LAST_PROMPT = "last_prompt_at"
        const val KEY_PROMPT_DATES = "prompt_dates"
        const val DAY = 86_400_000L
    }
}
