package com.example.glucocalculateur.domain

import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealItemDraft
import com.example.glucocalculateur.data.MealItemEntity
import com.example.glucocalculateur.data.RecipeComponentEntity
import com.example.glucocalculateur.data.RecipeEntity
import com.example.glucocalculateur.data.RecipeWithComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarbCalculatorTest {
    private val rice = FoodEntity(id = 1, name = "Riz", carbsPer100g = 28.0)
    private val sauce = FoodEntity(id = 2, name = "Sauce", carbsPer100g = 10.0)
    private val foodMap = listOf(rice, sauce).associateBy { it.id }

    private val dish = RecipeWithComponents(
        recipe = RecipeEntity(id = 10, name = "Riz sauce"),
        components = listOf(
            RecipeComponentEntity(recipeId = 10, foodId = 1, weightGrams = 150.0),
            RecipeComponentEntity(recipeId = 10, foodId = 2, weightGrams = 50.0)
        )
    )
    private val recipeMap = mapOf(10L to dish)

    @Test
    fun componentCarbs() {
        assertEquals(42.0, CarbCalculator.componentCarbs(28.0, 150.0), 1e-9)
    }

    @Test
    fun emptyRecipeHasZeroCarbsPer100g() {
        assertEquals(0.0, CarbCalculator.recipeCarbsPer100g(emptyList(), foodMap), 0.0)
    }

    @Test
    fun recipeCarbsPer100gIsWeightedAverage() {
        // (42 + 5) g de glucides pour 200 g
        assertEquals(47.0, CarbCalculator.recipeTotalCarbs(dish.components, foodMap), 1e-9)
        assertEquals(23.5, CarbCalculator.recipeCarbsPer100g(dish.components, foodMap), 1e-9)
    }

    @Test
    fun missingFoodCountsAsZeroButKeepsWeight() {
        val components = dish.components + RecipeComponentEntity(recipeId = 10, foodId = 99, weightGrams = 200.0)
        assertEquals(47.0, CarbCalculator.recipeTotalCarbs(components, foodMap), 1e-9)
        assertEquals(11.75, CarbCalculator.recipeCarbsPer100g(components, foodMap), 1e-9)
    }

    @Test
    fun mixedMealTotal() {
        val items = listOf(
            MealItemEntity(mealId = 1, foodId = 1, weightGrams = 100.0),      // 28
            MealItemEntity(mealId = 1, recipeId = 10, weightGrams = 100.0),   // 23,5
            MealItemEntity(mealId = 1, weightGrams = 50.0, snapshotName = "Pain", snapshotCarbsPer100g = 50.0) // 25
        )
        assertEquals(76.5, CarbCalculator.mealTotalCarbs(items, foodMap, recipeMap), 1e-9)
    }

    @Test
    fun deletedSourceFallsBackToSnapshot() {
        val item = MealItemEntity(mealId = 1, foodId = 99, weightGrams = 100.0, snapshotCarbsPer100g = 12.0)
        assertEquals(12.0, CarbCalculator.mealItemCarbs(item, foodMap, recipeMap), 1e-9)
        val orphan = MealItemEntity(mealId = 1, foodId = 99, weightGrams = 100.0)
        assertNull(CarbCalculator.mealItemCarbsPer100g(orphan, foodMap, recipeMap))
        assertEquals(0.0, CarbCalculator.mealItemCarbs(orphan, foodMap, recipeMap), 0.0)
    }

    @Test
    fun draftCarbs() {
        assertEquals(28.0, CarbCalculator.draftCarbsPer100g(MealItemDraft.Food(1, 10.0), foodMap, recipeMap)!!, 1e-9)
        assertEquals(23.5, CarbCalculator.draftCarbsPer100g(MealItemDraft.Recipe(10, 10.0), foodMap, recipeMap)!!, 1e-9)
        assertEquals(5.0, CarbCalculator.draftCarbsPer100g(MealItemDraft.Snapshot("x", 5.0, 10.0), foodMap, recipeMap)!!, 1e-9)
        assertNull(CarbCalculator.draftCarbsPer100g(MealItemDraft.Food(99, 10.0), foodMap, recipeMap))
    }
}
