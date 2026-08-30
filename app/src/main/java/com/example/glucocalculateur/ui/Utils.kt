package com.example.glucocalculateur.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealItemEntity
import com.example.glucocalculateur.data.RecipeComponentEntity
import com.example.glucocalculateur.data.RecipeWithComponents
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

    fun updateResourceLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        val localeList = LocaleList(locale)
        configuration.setLocales(localeList)
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    fun applyLocaleToContext(context: Context, locale: Locale) {
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        val localeList = LocaleList(locale)
        config.setLocales(localeList)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

object CarbCalculator {
    fun calculateRecipeTotalWeight(components: List<RecipeComponentEntity>): Double {
        return components.sumOf { it.weightGrams }
    }

    fun calculateRecipeCarbsPer100g(
        components: List<RecipeComponentEntity>,
        foodMap: Map<Long, FoodEntity>
    ): Double {
        val totalWeight = calculateRecipeTotalWeight(components)
        if (totalWeight <= 0.0) return 0.0
        val totalCarbs = components.sumOf { comp ->
            val food = foodMap[comp.foodId]
            if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
        }
        return (totalCarbs / totalWeight) * 100.0
    }

    fun calculateMealItemCarbs(
        item: MealItemEntity,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): Double {
        return when {
            item.foodId != null -> {
                val food = foodMap[item.foodId]
                if (food != null) (food.carbsPer100g / 100.0) * item.weightGrams else 0.0
            }
            item.recipeId != null -> {
                val recipeWithComponents = recipeMap[item.recipeId]
                if (recipeWithComponents != null) {
                    val rCarbsPer100g = calculateRecipeCarbsPer100g(recipeWithComponents.components, foodMap)
                    (rCarbsPer100g / 100.0) * item.weightGrams
                } else 0.0
            }
            else -> 0.0
        }
    }

    fun calculateMealTotalCarbs(
        items: List<MealItemEntity>,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): Double {
        return items.sumOf { calculateMealItemCarbs(it, foodMap, recipeMap) }
    }
}
