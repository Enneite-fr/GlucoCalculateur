package com.example.glucocalculateur.domain

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Recherche insensible à la casse et aux accents, par mots ; tri alphabétique selon la langue. */
object TextSearch {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace("œ", "oe")
            .replace("æ", "ae")

    /** Vrai si chaque mot de [query] apparaît dans [normalizedText] (déjà passé par [normalize]). */
    fun matches(normalizedText: String, query: String): Boolean =
        normalize(query).split(whitespace).filter { it.isNotEmpty() }.all { normalizedText.contains(it) }

    /** Comparateur alphabétique : « Épinard » se range avec les E, pas après Z. */
    fun collator(locale: Locale): Comparator<String> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        return Comparator { a, b -> collator.compare(a, b) }
    }
}
