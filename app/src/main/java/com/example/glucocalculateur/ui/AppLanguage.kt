package com.example.glucocalculateur.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Langue de l'application, appliquée par AppCompat (`setApplicationLocales`) : Android recrée
 * l'activité et conserve le choix (`autoStoreLocales`), sans manipulation manuelle de la configuration.
 */
enum class AppLanguage(val tag: String, val nativeName: String?) {
    SYSTEM("", null),
    FRENCH_FR("fr-FR", "Français (France)"),
    ENGLISH_US("en-US", "English (United States)"),
    ENGLISH_GB("en-GB", "English (United Kingdom)");

    fun apply() {
        AppCompatDelegate.setApplicationLocales(
            if (this == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    companion object {
        private const val LEGACY_PREFS = "settings"
        private const val LEGACY_KEY = "app_language"

        fun current(): AppLanguage {
            val locales = AppCompatDelegate.getApplicationLocales()
            val locale = if (locales.isEmpty) null else locales[0]
            if (locale == null) return SYSTEM
            val candidates = entries.filter { it != SYSTEM }
            return candidates.firstOrNull { Locale.forLanguageTag(it.tag) == Locale(locale.language, locale.country) }
                ?: candidates.firstOrNull { Locale.forLanguageTag(it.tag).language == locale.language }
                ?: SYSTEM
        }

        /** Reprend une seule fois la langue enregistrée par les versions précédentes. */
        fun migrateLegacyPreference(context: Context) {
            val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            if (!prefs.contains(LEGACY_KEY)) return
            val legacy = runCatching { valueOf(prefs.getString(LEGACY_KEY, null) ?: "") }.getOrNull()
            prefs.edit { remove(LEGACY_KEY) }
            if (legacy != null && legacy != SYSTEM && AppCompatDelegate.getApplicationLocales().isEmpty) {
                legacy.apply()
            }
        }
    }
}
