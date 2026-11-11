package com.example.glucocalculateur.domain

import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealItemDraft
import com.example.glucocalculateur.data.MealItemEntity
import com.example.glucocalculateur.data.RecipeComponentEntity
import com.example.glucocalculateur.data.RecipeWithComponents

/** Calculs de glucides. Fonctions pures, sans dépendance Android. */
object CarbCalculator {

    /** Glucides (g) apportés par [weightGrams] grammes d'un élément à [carbsPer100g] g / 100 g. */
    fun componentCarbs(carbsPer100g: Double, weightGrams: Double): Double = carbsPer100g / 100.0 * weightGrams

    fun recipeTotalWeight(components: List<RecipeComponentEntity>): Double = components.sumOf { it.weightGrams }

    fun recipeTotalCarbs(components: List<RecipeComponentEntity>, foodMap: Map<Long, FoodEntity>): Double =
        components.sumOf { comp ->
            foodMap[comp.foodId]?.let { componentCarbs(it.carbsPer100g, comp.weightGrams) } ?: 0.0
        }

    fun recipeCarbsPer100g(components: List<RecipeComponentEntity>, foodMap: Map<Long, FoodEntity>): Double =
        carbsPer100g(recipeTotalCarbs(components, foodMap), recipeTotalWeight(components))

    /** Glucides pour 100 g d'un total : 0 si le poids est nul. */
    fun carbsPer100g(totalCarbs: Double, totalWeight: Double): Double =
        if (totalWeight <= 0.0) 0.0 else totalCarbs / totalWeight * 100.0

    /** Glucides / 100 g d'un élément de repas, ou `null` si sa source est introuvable. */
    fun mealItemCarbsPer100g(
        item: MealItemEntity,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): Double? {
        val live = when {
            item.foodId != null -> foodMap[item.foodId]?.carbsPer100g
            item.recipeId != null -> recipeMap[item.recipeId]?.let { recipeCarbsPer100g(it.components, foodMap) }
            else -> null
        }
        return live ?: item.snapshotCarbsPer100g
    }

    fun mealItemCarbs(
        item: MealItemEntity,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): Double = mealItemCarbsPer100g(item, foodMap, recipeMap)?.let { componentCarbs(it, item.weightGrams) } ?: 0.0

    fun mealTotalCarbs(
        items: List<MealItemEntity>,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): Double = items.sumOf { mealItemCarbs(it, foodMap, recipeMap) }

    /** Glucides / 100 g d'un élément de repas en cours d'édition, ou `null` si sa source est introuvable. */
    fun draftCarbsPer100g(
        draft: MealItemDraft,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): Double? = when (draft) {
        is MealItemDraft.Food -> foodMap[draft.foodId]?.carbsPer100g
        is MealItemDraft.Recipe -> recipeMap[draft.recipeId]?.let { recipeCarbsPer100g(it.components, foodMap) }
        is MealItemDraft.Snapshot -> draft.carbsPer100g
    }
}
