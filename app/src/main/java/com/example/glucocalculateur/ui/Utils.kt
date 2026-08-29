package com.example.glucocalculateur.ui

import java.text.NumberFormat
import java.util.Locale

object Formatter {
    fun formatDouble(value: Double): String {
        return "%.1f".format(Locale.getDefault(), value)
    }

    fun parseDouble(value: String): Double? {
        val cleanedValue = value.replace(',', '.')
        return cleanedValue.toDoubleOrNull()
    }

    fun validateNumericInput(input: String): String {
        val filtered = input.replace(',', '.')
        val parts = filtered.split('.')
        return if (parts.size > 2) {
            // Garde uniquement le premier point
            parts[0] + "." + parts.drop(1).joinToString("")
        } else if (parts.size == 2 && parts[1].length > 2) {
            // Limite à 2 chiffres après la virgule
            parts[0] + "." + parts[1].take(2)
        } else {
            filtered
        }.replace('.', if (Locale.getDefault().language == "fr") ',' else '.')
    }
}
