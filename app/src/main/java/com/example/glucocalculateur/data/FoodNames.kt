package com.example.glucocalculateur.data

import java.util.Locale

/**
 * Normalisation unique des noms (aliments, recettes), utilisée à la création manuelle,
 * à l'import et à la résolution des ingrédients.
 */
object FoodNames {
    private val spaces = Regex("\\s+")

    /** Nom tel qu'il doit être enregistré : sans BOM, sans espaces superflus. */
    fun clean(raw: String): String = raw.replace("﻿", "").trim().replace(spaces, " ")

    /** Clé de comparaison : nom nettoyé, insensible à la casse (y compris les lettres accentuées). */
    fun key(raw: String): String = clean(raw).lowercase(Locale.ROOT)
}
