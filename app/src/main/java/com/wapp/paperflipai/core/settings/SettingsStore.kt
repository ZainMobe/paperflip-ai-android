package com.wapp.paperflipai.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.wapp.paperflipai.designsystem.theme.PFAppearance
import com.wapp.paperflipai.designsystem.theme.PFGenerationLanguage
import com.wapp.paperflipai.designsystem.theme.PFLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Every user preference the app persists, in one place — the Android
 * counterpart of the scattered `@AppStorage` keys on iOS (PFAppearanceStore,
 * PFLanguageStore, OnboardingState, AIConsent, notification settings).
 *
 * Backed by SharedPreferences rather than DataStore on purpose: the theme
 * and the onboarding flag must be readable synchronously during the first
 * composition, otherwise the app flashes the wrong appearance on launch.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Appearance ────────────────────────────────────────────────────

    private val _appearance = MutableStateFlow(PFAppearance.from(prefs.getString(KEY_APPEARANCE, null)))
    val appearance: StateFlow<PFAppearance> = _appearance.asStateFlow()

    fun setAppearance(value: PFAppearance) {
        _appearance.value = value
        prefs.edit().putString(KEY_APPEARANCE, value.rawValue).apply()
    }

    // ── App language ──────────────────────────────────────────────────

    private val _language = MutableStateFlow(PFLanguage.from(prefs.getString(KEY_LANGUAGE, null)))
    val language: StateFlow<PFLanguage> = _language.asStateFlow()

    fun setLanguage(value: PFLanguage) {
        _language.value = value
        prefs.edit().putString(KEY_LANGUAGE, value.rawValue).apply()
    }

    // ── Flashcard generation language ─────────────────────────────────

    private val _generationLanguage = MutableStateFlow(
        PFGenerationLanguage.from(prefs.getString(KEY_GEN_LANGUAGE, null))
            ?: PFGenerationLanguage.defaultLanguage(_language.value.rawValue)
    )
    val generationLanguage: StateFlow<PFGenerationLanguage> = _generationLanguage.asStateFlow()

    fun setGenerationLanguage(value: PFGenerationLanguage) {
        _generationLanguage.value = value
        prefs.edit().putString(KEY_GEN_LANGUAGE, value.rawValue).apply()
    }

    private val _useSourceLanguage = MutableStateFlow(prefs.getBoolean(KEY_USE_SOURCE_LANGUAGE, false))
    val useSourceLanguage: StateFlow<Boolean> = _useSourceLanguage.asStateFlow()

    fun setUseSourceLanguage(value: Boolean) {
        _useSourceLanguage.value = value
        prefs.edit().putBoolean(KEY_USE_SOURCE_LANGUAGE, value).apply()
    }

    // ── Onboarding ────────────────────────────────────────────────────

    private val _hasCompletedOnboarding =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun markOnboardingCompleted() {
        _hasCompletedOnboarding.value = true
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun resetOnboarding() {
        _hasCompletedOnboarding.value = false
        prefs.edit().remove(KEY_ONBOARDING_DONE).apply()
    }

    // ── AI consent (support chat + generation review) ─────────────────

    private val _hasAcknowledgedAI = MutableStateFlow(prefs.getBoolean(KEY_AI_CONSENT, false))
    val hasAcknowledgedAI: StateFlow<Boolean> = _hasAcknowledgedAI.asStateFlow()

    fun acknowledgeAI() {
        _hasAcknowledgedAI.value = true
        prefs.edit().putBoolean(KEY_AI_CONSENT, true).apply()
    }

    // ── Study reminders ───────────────────────────────────────────────

    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ON, false))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    /** Minutes past midnight, local time. Defaults to 19:00. */
    private val _reminderMinuteOfDay = MutableStateFlow(prefs.getInt(KEY_REMINDER_TIME, 19 * 60))
    val reminderMinuteOfDay: StateFlow<Int> = _reminderMinuteOfDay.asStateFlow()

    fun setReminder(enabled: Boolean, minuteOfDay: Int = _reminderMinuteOfDay.value) {
        _reminderEnabled.value = enabled
        _reminderMinuteOfDay.value = minuteOfDay
        prefs.edit()
            .putBoolean(KEY_REMINDER_ON, enabled)
            .putInt(KEY_REMINDER_TIME, minuteOfDay)
            .apply()
    }

    // ── Library view preference ───────────────────────────────────────

    private val _libraryGrid = MutableStateFlow(prefs.getBoolean(KEY_LIBRARY_GRID, true))
    val libraryGrid: StateFlow<Boolean> = _libraryGrid.asStateFlow()

    fun setLibraryGrid(value: Boolean) {
        _libraryGrid.value = value
        prefs.edit().putBoolean(KEY_LIBRARY_GRID, value).apply()
    }

    companion object {
        private const val PREFS = "paperflip_settings"
        private const val KEY_APPEARANCE = "appearance.mode"
        private const val KEY_LANGUAGE = "app.language"
        private const val KEY_GEN_LANGUAGE = "generation.language"
        private const val KEY_USE_SOURCE_LANGUAGE = "generation.useSourceLanguage"
        private const val KEY_ONBOARDING_DONE = "onboarding.completed.v1"
        private const val KEY_AI_CONSENT = "paperflip.aiConsent.acknowledged.v1"
        private const val KEY_REMINDER_ON = "reminder.enabled"
        private const val KEY_REMINDER_TIME = "reminder.minuteOfDay"
        private const val KEY_LIBRARY_GRID = "library.grid"
    }
}
