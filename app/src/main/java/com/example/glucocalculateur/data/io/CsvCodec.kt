package com.example.glucocalculateur.data.io

import com.example.glucocalculateur.data.FoodNames

/**
 * CSV des aliments (« nom ; glucides pour 100 g ») et des recettes (« recette ; ingrédient ; poids »).
 * Fonctions pures : texte → lignes, lignes → texte.
 *
 * Lecture : séparateur `;` (ou tabulation), champs entre guillemets, virgule décimale,
 * ligne d'en-tête et lignes commençant par `#` ignorées.
 * Écriture : BOM UTF-8 et ligne d'en-tête (pour Excel), champs mis entre guillemets si besoin.
 */
object CsvCodec {
    const val MAX_CARBS_PER_100G = 100.0

    fun parseFoods(text: String): Parsed<FoodRow> = parse(text, columns = 2) { fields, lineNumber, raw ->
        val name = FoodNames.clean(fields[0])
        val carbs = FileNumbers.parse(fields[1])
        when {
            carbs == null -> LineResult.BadNumber
            name.isEmpty() -> LineResult.Issue(ImportIssue(ImportIssue.Type.MALFORMED_LINE, lineNumber, raw))
            carbs < 0.0 || carbs > MAX_CARBS_PER_100G ->
                LineResult.Issue(ImportIssue(ImportIssue.Type.INVALID_NUMBER, lineNumber, raw))
            else -> LineResult.Row(FoodRow(name, carbs))
        }
    }

    fun parseRecipes(text: String): Parsed<RecipeRow> = parse(text, columns = 3) { fields, lineNumber, raw ->
        val recipeName = FoodNames.clean(fields[0])
        val foodName = FoodNames.clean(fields[1])
        val weight = FileNumbers.parse(fields[2])
        when {
            weight == null -> LineResult.BadNumber
            recipeName.isEmpty() || foodName.isEmpty() ->
                LineResult.Issue(ImportIssue(ImportIssue.Type.MALFORMED_LINE, lineNumber, raw))
            weight <= 0.0 -> LineResult.Issue(ImportIssue(ImportIssue.Type.INVALID_NUMBER, lineNumber, raw))
            else -> LineResult.Row(RecipeRow(recipeName, foodName, weight))
        }
    }

    fun writeFoods(foods: List<FoodRow>, header: List<String>, decimalSeparator: Char): String =
        write(header, foods.map { listOf(it.name, FileNumbers.format(it.carbsPer100g, decimalSeparator)) })

    fun writeRecipes(rows: List<RecipeRow>, header: List<String>, decimalSeparator: Char): String =
        write(header, rows.map { listOf(it.recipeName, it.foodName, FileNumbers.format(it.weightGrams, decimalSeparator)) })

    // --- Implémentation ---

    private const val SEPARATOR = ';'
    private const val LINE_END = "\r\n"

    private sealed interface LineResult<out T> {
        data class Row<T>(val row: T) : LineResult<T>
        data class Issue(val issue: ImportIssue) : LineResult<Nothing>
        /** Valeur numérique illisible : en-tête si c'est la première ligne utile, erreur sinon. */
        data object BadNumber : LineResult<Nothing>
    }

    private fun <T> parse(
        text: String,
        columns: Int,
        readLine: (fields: List<String>, lineNumber: Int, raw: String) -> LineResult<T>
    ): Parsed<T> {
        val separator = detectSeparator(text)
        val rows = mutableListOf<T>()
        val issues = mutableListOf<ImportIssue>()
        var firstUsefulLine = true
        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.replace("\uFEFF", "").trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val lineNumber = index + 1
            val isFirst = firstUsefulLine
            firstUsefulLine = false

            val fields = splitLine(line, separator)
            if (fields.size < columns) {
                issues += ImportIssue(ImportIssue.Type.MALFORMED_LINE, lineNumber, line)
                return@forEachIndexed
            }
            when (val result = readLine(fields, lineNumber, line)) {
                is LineResult.Row -> rows += result.row
                is LineResult.Issue -> issues += result.issue
                LineResult.BadNumber ->
                    if (!isFirst) issues += ImportIssue(ImportIssue.Type.INVALID_NUMBER, lineNumber, line)
            }
        }
        return Parsed(rows, issues)
    }

    private fun detectSeparator(text: String): Char =
        if (!text.contains(SEPARATOR) && text.contains('\t')) '\t' else SEPARATOR

    /** Découpe une ligne en respectant les guillemets (`""` = guillemet littéral). */
    internal fun splitLine(line: String, separator: Char = SEPARATOR): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == separator && !inQuotes -> {
                    fields += current.toString().trim()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields += current.toString().trim()
        // Ligne entière entre guillemets (« "Pomme;12" ») : on découpe son contenu.
        if (fields.size == 1 && fields[0].contains(separator)) return splitLine(fields[0], separator)
        return fields
    }

    private fun write(header: List<String>, rows: List<List<String>>): String = buildString {
        append('\uFEFF')
        append(header.joinToString(SEPARATOR.toString(), transform = ::escape)).append(LINE_END)
        rows.forEach { append(it.joinToString(SEPARATOR.toString(), transform = ::escape)).append(LINE_END) }
    }

    private fun escape(field: String): String =
        if (field.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
