package com.example.glucocalculateur.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Formatter {
    fun formatDouble(value: Double): String {
        if (value == 0.0 || value.isNaN() || value.isInfinite()) return "0"
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        val df = DecimalFormat("#.##", symbols).apply {
            isGroupingUsed = false
        }
        val formatted = df.format(value)
        return if (formatted == "-0" || formatted == "-0.0" || formatted == "-0,0") "0" else formatted
    }

    fun parseDouble(value: String): Double? {
        val cleanedValue = value.trim().replace(',', '.')
        return cleanedValue.toDoubleOrNull()
    }

    fun validateNumericInput(input: String): String {
        val decimalSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
        val filtered = input.replace(',', '.').replace(' ', '.')
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

}
