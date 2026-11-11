package com.example.glucocalculateur.data.io

import com.example.glucocalculateur.data.FoodNames
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sauvegarde complète au format JSON.
 *
 * Version 2 : `{"version": 2, "foods": [...], "recipes": [...], "meals": [...]}`.
 * Les fichiers de la version 1 (sans `version`, sans `meals`) restent lisibles.
 */
object JsonBackupCodec {
    const val CURRENT_VERSION = 2

    fun encode(backup: Backup): String {
        val json = JSONObject()
        json.put("version", CURRENT_VERSION)
        json.put("foods", JSONArray().apply {
            backup.foods.forEach { food ->
                put(JSONObject().put("name", food.name).put("carbsPer100g", food.carbsPer100g))
            }
        })
        json.put("recipes", JSONArray().apply {
            backup.recipes.forEach { recipe ->
                put(JSONObject().put("name", recipe.name).put("components", JSONArray().apply {
                    recipe.components.forEach { comp ->
                        put(JSONObject().put("foodName", comp.foodName).put("weightGrams", comp.weightGrams))
                    }
                }))
            }
        })
        json.put("meals", JSONArray().apply {
            backup.meals.forEach { meal ->
                put(JSONObject().put("name", meal.name).put("dateTimestamp", meal.timestamp).put("items", JSONArray().apply {
                    meal.items.forEach { item ->
                        put(JSONObject().apply {
                            item.foodName?.let { put("foodName", it) }
                            item.recipeName?.let { put("recipeName", it) }
                            item.label?.let { put("name", it) }
                            item.carbsPer100g?.let { put("carbsPer100g", it) }
                            put("weightGrams", item.weightGrams)
                        })
                    }
                }))
            }
        })
        return json.toString(2)
    }

    /** Décode une sauvegarde ; lève [org.json.JSONException] si le texte n'est pas un objet JSON. */
    fun decode(text: String): DecodedBackup {
        val json = JSONObject(text.removePrefix("\uFEFF"))
        val issues = mutableListOf<ImportIssue>()

        val foods = json.objects("foods").mapNotNull { obj ->
            val name = FoodNames.clean(obj.optString("name", ""))
            val carbs = obj.optDouble("carbsPer100g", Double.NaN)
            when {
                name.isEmpty() -> null.also { issues += ImportIssue(ImportIssue.Type.MALFORMED_LINE, subject = obj.toString()) }
                carbs.isNaN() || carbs < 0.0 || carbs > CsvCodec.MAX_CARBS_PER_100G ->
                    null.also { issues += ImportIssue(ImportIssue.Type.INVALID_NUMBER, subject = name) }
                else -> FoodRow(name, carbs)
            }
        }

        val recipes = json.objects("recipes").mapNotNull { obj ->
            val name = FoodNames.clean(obj.optString("name", ""))
            if (name.isEmpty()) {
                issues += ImportIssue(ImportIssue.Type.MALFORMED_LINE, subject = obj.toString())
                return@mapNotNull null
            }
            val components = obj.objects("components").mapNotNull { comp ->
                val foodName = FoodNames.clean(comp.optString("foodName", ""))
                val weight = comp.optDouble("weightGrams", Double.NaN)
                if (foodName.isEmpty() || weight.isNaN() || weight <= 0.0) null else BackupComponent(foodName, weight)
            }
            BackupRecipe(name, components)
        }

        val meals = json.objects("meals").mapNotNull { obj ->
            val name = FoodNames.clean(obj.optString("name", ""))
            val timestamp = obj.optLong("dateTimestamp", Long.MIN_VALUE)
            if (name.isEmpty() || timestamp == Long.MIN_VALUE) {
                issues += ImportIssue(ImportIssue.Type.MALFORMED_LINE, subject = obj.toString())
                return@mapNotNull null
            }
            val items = obj.objects("items").mapNotNull { item ->
                val weight = item.optDouble("weightGrams", Double.NaN)
                if (weight.isNaN() || weight <= 0.0) return@mapNotNull null
                BackupMealItem(
                    foodName = item.optStringOrNull("foodName"),
                    recipeName = item.optStringOrNull("recipeName"),
                    label = item.optStringOrNull("name"),
                    carbsPer100g = item.optDouble("carbsPer100g", Double.NaN).takeUnless { it.isNaN() },
                    weightGrams = weight
                )
            }
            BackupMeal(name, timestamp, items)
        }

        return DecodedBackup(Backup(foods, recipes, meals), issues)
    }

    private fun JSONObject.objects(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) FoodNames.clean(optString(key)).takeIf { it.isNotEmpty() } else null
}
