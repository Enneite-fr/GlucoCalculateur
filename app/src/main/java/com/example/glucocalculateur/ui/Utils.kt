package com.example.glucocalculateur.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object Formatter {
    fun formatDouble(value: Double): String {
        // Formate avec au plus 2 décimales, et supprime les zéros inutiles
        val formatted = "%.2f".format(Locale.getDefault(), value)
        return if (formatted.contains(",") || formatted.contains(".")) {
            formatted.trimEnd('0').trimEnd(',').trimEnd('.')
        } else {
            formatted
        }
    }

    fun parseDouble(value: String): Double? {
        val decimalSeparator = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
        val cleanedValue = value.replace(decimalSeparator.toString(), ".")
        return cleanedValue.toDoubleOrNull()
    }

    fun validateNumericInput(input: String): String {
        val decimalSeparator = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
        val filtered = input.replace(',', '.').replace(' ', '.') // Normalise vers le point pour le traitement
        val parts = filtered.split('.')
        val processed = if (parts.size > 2) {
            // Garde uniquement le premier point
            parts[0] + "." + parts.drop(1).joinToString("")
        } else if (parts.size == 2 && parts[1].length > 2) {
            // Limite à 2 chiffres après la virgule
            parts[0] + "." + parts[1].take(2)
        } else {
            filtered
        }
        return processed.replace('.', decimalSeparator)
    }

    fun updateResourceLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        val localeList = android.os.LocaleList(locale)
        configuration.setLocales(localeList)
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    fun applyLocaleToContext(context: Context, locale: Locale) {
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        val localeList = android.os.LocaleList(locale)
        config.setLocales(localeList)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
