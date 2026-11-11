package com.example.glucocalculateur.data.io

/** Ligne d'aliment lue dans un fichier. */
data class FoodRow(val name: String, val carbsPer100g: Double)

/** Ligne « recette ; ingrédient ; poids » lue dans un fichier. */
data class RecipeRow(val recipeName: String, val foodName: String, val weightGrams: Double)

/** Problème rencontré pendant un import ; le libellé affiché est choisi par l'interface. */
data class ImportIssue(
    val type: Type,
    /** Numéro de ligne (1 = première ligne du fichier), si pertinent. */
    val line: Int? = null,
    /** Élément concerné : texte de la ligne, nom de recette, de repas… */
    val subject: String = "",
    /** Précision : ingrédients introuvables, etc. */
    val detail: String = ""
) {
    enum class Type {
        /** Ligne illisible (nombre de colonnes, nom vide). */
        MALFORMED_LINE,
        /** Nombre illisible ou hors bornes. */
        INVALID_NUMBER,
        /** Recette ignorée : ingrédients absents de la base. */
        UNKNOWN_INGREDIENTS,
        /** Repas ignoré : aucun élément utilisable. */
        EMPTY_MEAL,
        /** Repas importé sans certains éléments, dont la source est introuvable. */
        PARTIAL_MEAL
    }
}

/** Résultat d'un décodage : lignes valides et problèmes rencontrés. */
data class Parsed<T>(val rows: List<T>, val issues: List<ImportIssue>)

/** Bilan d'un import. */
data class ImportReport(
    val added: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
    /** Éléments déjà présents, ignorés volontairement (repas en double). */
    val alreadyPresent: Int = 0,
    val issues: List<ImportIssue> = emptyList()
) {
    operator fun plus(other: ImportReport) = ImportReport(
        added = added + other.added,
        updated = updated + other.updated,
        unchanged = unchanged + other.unchanged,
        alreadyPresent = alreadyPresent + other.alreadyPresent,
        issues = issues + other.issues
    )
}

// --- Sauvegarde complète (JSON) ---

data class Backup(
    val foods: List<FoodRow> = emptyList(),
    val recipes: List<BackupRecipe> = emptyList(),
    val meals: List<BackupMeal> = emptyList()
)

data class DecodedBackup(val backup: Backup, val issues: List<ImportIssue>)

data class BackupRecipe(val name: String, val components: List<BackupComponent>)

data class BackupComponent(val foodName: String, val weightGrams: Double)

data class BackupMeal(val name: String, val timestamp: Long, val items: List<BackupMealItem>)

/**
 * Élément de repas sauvegardé. [foodName] ou [recipeName] désigne la source quand elle existe ;
 * [label] et [carbsPer100g] en sont une copie, utilisée si la source est introuvable à l'import.
 */
data class BackupMealItem(
    val foodName: String? = null,
    val recipeName: String? = null,
    val label: String? = null,
    val carbsPer100g: Double? = null,
    val weightGrams: Double
)
