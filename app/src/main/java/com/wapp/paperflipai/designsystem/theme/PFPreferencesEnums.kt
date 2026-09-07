package com.wapp.paperflipai.designsystem.theme

import com.wapp.paperflipai.designsystem.PFIcons
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * User-selectable appearance preference — counterpart of `PFAppearance.swift`.
 * Persisted by `SettingsStore`; the app root maps it onto [PaperflipTheme].
 */
enum class PFAppearance(val rawValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    val icon: ImageVector
        get() = when (this) {
            System -> PFIcons.System
            Light -> PFIcons.LightMode
            Dark -> PFIcons.Appearance
        }

    companion object {
        fun from(raw: String?): PFAppearance =
            entries.firstOrNull { it.rawValue == raw } ?: System
    }
}

/**
 * User-selectable app language — counterpart of `PFLanguage.swift`.
 * Applied through `AppCompatDelegate.setApplicationLocales` so it survives
 * process death and shows up in Android 13+ per-app language settings.
 */
enum class PFLanguage(val rawValue: String, val displayName: String) {
    System("system", "System"),
    En("en", "English"),
    Ar("ar", "العربية"),
    Fr("fr", "Français"),
    Es("es", "Español");

    /** BCP-47 tag, or empty string meaning "follow the system". */
    val localeTag: String get() = if (this == System) "" else rawValue

    val icon: ImageVector
        get() = when (this) {
            System -> PFIcons.System
            Ar -> PFIcons.Language
            else -> PFIcons.Language
        }

    companion object {
        fun from(raw: String?): PFLanguage =
            entries.firstOrNull { it.rawValue == raw } ?: System
    }
}

/**
 * The language the AI writes flashcards in — counterpart of
 * `PFGenerationLanguage.swift`. Independent of the app UI language.
 */
enum class PFGenerationLanguage(val rawValue: String, val displayName: String, val flag: String) {
    En("en", "English", "🇬🇧"),
    Fr("fr", "Français", "🇫🇷"),
    Es("es", "Español", "🇪🇸"),
    Hi("hi", "हिन्दी", "🇮🇳"),
    Ur("ur", "اردو", "🇵🇰"),
    Ar("ar", "العربية", "🇸🇦");

    companion object {
        fun from(raw: String?): PFGenerationLanguage? =
            entries.firstOrNull { it.rawValue == raw }

        /** Falls back to the app language, then the device language, then English. */
        fun defaultLanguage(appLanguageRaw: String? = null): PFGenerationLanguage {
            from(appLanguageRaw)?.let { return it }
            from(Locale.getDefault().language)?.let { return it }
            return En
        }
    }
}
