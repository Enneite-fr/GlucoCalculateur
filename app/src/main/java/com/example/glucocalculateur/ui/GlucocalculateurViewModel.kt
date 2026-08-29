package com.example.glucocalculateur.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glucocalculateur.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GlucoCalculateurViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val foodDao = db.foodDao()
    private val recipeDao = db.recipeDao()
    private val mealDao = db.mealDao()

    val allFood: StateFlow<List<FoodEntity>> = foodDao.getAllFood()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<List<RecipeWithComponents>> = recipeDao.getAllRecipesWithComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeals: StateFlow<List<MealWithItems>> = mealDao.getAllMealsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFood(name: String, carbsPer100g: Double) {
        viewModelScope.launch {
            foodDao.insertFood(FoodEntity(name = name, carbsPer100g = carbsPer100g))
        }
    }

    fun deleteFood(food: FoodEntity) {
        viewModelScope.launch {
            foodDao.deleteFood(food)
        }
    }

    fun addRecipe(name: String, components: List<Pair<Long, Double>>) {
        viewModelScope.launch {
            val recipeId = recipeDao.insertRecipe(RecipeEntity(name = name))
            val componentEntities = components.map { (foodId, weight) ->
                RecipeComponentEntity(recipeId = recipeId, foodId = foodId, weightGrams = weight)
            }
            recipeDao.insertRecipeComponents(componentEntities)
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            recipeDao.deleteRecipe(recipe)
        }
    }

    fun addMeal(name: String, items: List<Triple<Long?, Long?, Double>>) {
        viewModelScope.launch {
            val mealId = mealDao.insertMeal(MealEntity(name = name))
            val itemEntities = items.map { (foodId, recipeId, weight) ->
                MealItemEntity(mealId = mealId, foodId = foodId, recipeId = recipeId, weightGrams = weight)
            }
            mealDao.insertMealItems(itemEntities)
        }
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            mealDao.deleteMeal(meal)
        }
    }
}
