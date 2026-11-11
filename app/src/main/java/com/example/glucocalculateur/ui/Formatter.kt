package com.example.glucocalculateur.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Formatage et saisie des nombres, selon la langue de l'application. */
object Formatter {
    /** Nombre avec au plus deux décimales, sans séparateur de milliers. */
    fun formatNumber(value: Double, locale: Locale): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val formatted = DecimalFormat("#.##", DecimalFormatSymbols.getInstance(locale))
            .apply { isGroupingUsed = false }
            .format(value)
        return if (formatted.startsWith("-") && formatted.drop(1).all { it == '0' }) "0" else formatted
    }

    /** Lit un nombre saisi avec une virgule ou un point. */
    fun parseDouble(value: String): Double? =
        value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    /**
     * Filtre une saisie décimale : chiffres et un seul séparateur (affiché selon la langue),
     * deux décimales au plus. Renvoie `null` si la saisie contient un caractère interdit.
     */
    fun sanitizeDecimalInput(input: String, locale: Locale): String? {
        if (!input.all { it.isDigit() || it == ',' || it == '.' }) return null
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val parts = input.replace(',', '.').split('.')
        if (parts.size == 1) return parts[0]
        return parts[0] + separator + parts.drop(1).joinToString("").take(2)
    }
}
