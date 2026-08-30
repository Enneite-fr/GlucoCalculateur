package com.example.glucocalculateur.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glucocalculateur.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

enum class AppLanguage(val code: String, val country: String, val label: String) {
    SYSTEM("", "", "Système"),
    FRENCH_FR("fr", "FR", "Français (France)"),
    ENGLISH_US("en", "US", "English (United States)"),
    ENGLISH_GB("en", "GB", "English (United Kingdom)")
}

class GlucoCalculateurViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val foodDao = db.foodDao()
    private val recipeDao = db.recipeDao()
    private val mealDao = db.mealDao()

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    private val _language = MutableStateFlow(
        AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.name).apply()
        _language.value = lang
    }

    fun exportDataToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val foods = allFood.value
                val recipes = allRecipes.value
                val json = JSONObject()
                
                val foodArray = JSONArray()
                foods.forEach { food ->
                    foodArray.put(JSONObject().apply {
                        put("name", food.name)
                        put("carbsPer100g", food.carbsPer100g)
                    })
                }
                json.put("foods", foodArray)
                
                val recipeArray = JSONArray()
                recipes.forEach { recipe ->
                    recipeArray.put(JSONObject().apply {
                        put("name", recipe.recipe.name)
                        val componentsArray = JSONArray()
                        recipe.components.forEach { comp ->
                            val food = foods.find { it.id == comp.foodId }
                            componentsArray.put(JSONObject().apply {
                                put("foodName", food?.name)
                                put("weightGrams", comp.weightGrams)
                            })
                        }
                        put("components", componentsArray)
                    })
                }
                json.put("recipes", recipeArray)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toString(2).toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importDataFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonString != null) {
                    val json = JSONObject(jsonString)
                    
                    // Import foods
                    val foodArray = json.optJSONArray("foods")
                    if (foodArray != null) {
                        for (i in 0 until foodArray.length()) {
                            val foodJson = foodArray.getJSONObject(i)
                            val name = foodJson.getString("name")
                            val carbs = foodJson.getDouble("carbsPer100g")
                            foodDao.insertFood(FoodEntity(name = name, carbsPer100g = carbs))
                        }
                    }
                    
                    // Re-fetch foods to get new IDs for recipes
                    val currentFoods = foodDao.getAllFoodSync()
                    
                    // Import recipes
                    val recipeArray = json.optJSONArray("recipes")
                    if (recipeArray != null) {
                        for (i in 0 until recipeArray.length()) {
                            val recipeJson = recipeArray.getJSONObject(i)
                            val recipeName = recipeJson.getString("name")
                            val recipeId = recipeDao.insertRecipe(RecipeEntity(name = recipeName))
                            
                            val componentsArray = recipeJson.getJSONArray("components")
                            val componentEntities = mutableListOf<RecipeComponentEntity>()
                            for (j in 0 until componentsArray.length()) {
                                val compJson = componentsArray.getJSONObject(j)
                                val foodName = compJson.getString("foodName")
                                val weight = compJson.getDouble("weightGrams")
                                
                                val food = currentFoods.find { it.name == foodName }
                                if (food != null) {
                                    componentEntities.add(RecipeComponentEntity(
                                        recipeId = recipeId,
                                        foodId = food.id,
                                        weightGrams = weight
                                    ))
                                }
                            }
                            recipeDao.insertRecipeComponents(componentEntities)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportFoodsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val foods = allFood.value
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        foods.forEach { food ->
                            writer.write("${food.name};${food.carbsPer100g}\n")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importFoodsFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                
                // Détection simplifiée : si UTF-8 produit des caractères invalides (), on bascule sur ISO-8859-1
                val utf8Content = String(bytes, Charsets.UTF_8)
                val content = if (utf8Content.contains("\uFFFD")) {
                    String(bytes, charset("ISO-8859-1"))
                } else {
                    utf8Content
                }

                content.lineSequence().forEach { rawLine ->
                    // Nettoyage de la ligne : suppression des guillemets éventuels et des espaces
                    val line = rawLine.trim().removeSurrounding("\"")
                    if (line.isBlank() || line.startsWith("#")) return@forEach
                    
                    val parts = line.split(";")
                    if (parts.size >= 2) {
                        val name = parts[0].trim().removeSurrounding("\"").trim()
                        val carbsPart = parts[1].trim().removeSurrounding("\"").trim().replace(",", ".")
                        val carbs = carbsPart.toDoubleOrNull() ?: 0.0
                        
                        if (name.isNotEmpty()) {
                            val existing = foodDao.getFoodByNameSync(name)
                            if (existing != null) {
                                foodDao.updateFood(existing.copy(carbsPer100g = carbs))
                            } else {
                                foodDao.insertFood(FoodEntity(name = name, carbsPer100g = carbs))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportRecipesToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recipes = allRecipes.value
                val foods = foodDao.getAllFoodSync()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        recipes.forEach { recipe ->
                            recipe.components.forEach { comp ->
                                val foodName = foods.find { it.id == comp.foodId }?.name ?: "Inconnu"
                                writer.write("${recipe.recipe.name};${foodName};${comp.weightGrams}\n")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importRecipesFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentFoods = foodDao.getAllFoodSync()
                val recipeData = mutableMapOf<String, MutableList<Pair<String, Double>>>()
                
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        if (line.isBlank()) return@forEach
                        val parts = line.split(";")
                        if (parts.size >= 3) {
                            val recipeName = parts[0].trim()
                            val foodName = parts[1].trim()
                            val weight = parts[2].trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                            
                            if (recipeName.isNotEmpty()) {
                                recipeData.getOrPut(recipeName) { mutableListOf() }.add(foodName to weight)
                            }
                        }
                    }
                }
                
                recipeData.forEach { (recipeName, components) ->
                    var recipeId = recipeDao.getRecipeByNameSync(recipeName)?.id
                    if (recipeId == null) {
                        recipeId = recipeDao.insertRecipe(RecipeEntity(name = recipeName))
                    } else {
                        recipeDao.updateRecipe(RecipeEntity(id = recipeId, name = recipeName))
                        recipeDao.deleteRecipeComponents(recipeId)
                    }
                    
                    val componentEntities = components.mapNotNull { (foodName, weight) ->
                        val food = currentFoods.find { it.name == foodName }
                        if (food != null) {
                            RecipeComponentEntity(recipeId = recipeId!!, foodId = food.id, weightGrams = weight)
                        } else {
                            null
                        }
                    }
                    recipeDao.insertRecipeComponents(componentEntities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    fun updateFood(id: Long, name: String, carbsPer100g: Double) {
        viewModelScope.launch {
            foodDao.updateFood(FoodEntity(id = id, name = name, carbsPer100g = carbsPer100g))
        }
    }

    fun deleteFood(food: FoodEntity) {
        viewModelScope.launch {
            // Ordre : Repas liés aux recettes liées à l'aliment -> Recettes liées à l'aliment -> Repas liés à l'aliment -> Aliment
            val recipes = foodDao.getRecipesUsingFood(food.id)
            recipes.forEach { mealDao.deleteMealsUsingRecipe(it.id) }
            mealDao.deleteRecipesUsingFood(food.id)
            mealDao.deleteMealsUsingFood(food.id)
            foodDao.deleteFood(food)
        }
    }

    suspend fun getFoodImpact(food: FoodEntity): Pair<Int, Int> {
        val recipes = foodDao.getRecipesUsingFood(food.id)
        val mealsFromFood = foodDao.getMealsUsingFood(food.id)
        val mealsFromRecipes = recipes.flatMap { recipeDao.getMealsUsingRecipe(it.id) }
        val totalMeals = (mealsFromFood + mealsFromRecipes).distinctBy { it.id }.size
        return recipes.size to totalMeals
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

    fun updateRecipe(id: Long, name: String, components: List<Pair<Long, Double>>) {
        viewModelScope.launch {
            recipeDao.updateRecipe(RecipeEntity(id = id, name = name))
            recipeDao.deleteRecipeComponents(id)
            val componentEntities = components.map { (foodId, weight) ->
                RecipeComponentEntity(recipeId = id, foodId = foodId, weightGrams = weight)
            }
            recipeDao.insertRecipeComponents(componentEntities)
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            mealDao.deleteMealsUsingRecipe(recipe.id)
            recipeDao.deleteRecipe(recipe)
        }
    }

    suspend fun getRecipeImpact(recipe: RecipeEntity): Int {
        return recipeDao.getMealsUsingRecipe(recipe.id).size
    }

    fun addMeal(name: String, timestamp: Long, items: List<Triple<Long?, Long?, Double>>) {
        viewModelScope.launch {
            val mealId = mealDao.insertMeal(MealEntity(name = name, dateTimestamp = timestamp))
            val itemEntities = items.map { (foodId, recipeId, weight) ->
                MealItemEntity(mealId = mealId, foodId = foodId, recipeId = recipeId, weightGrams = weight)
            }
            mealDao.insertMealItems(itemEntities)
        }
    }

    fun updateMeal(id: Long, name: String, timestamp: Long, items: List<Triple<Long?, Long?, Double>>) {
        viewModelScope.launch {
            mealDao.updateMeal(MealEntity(id = id, name = name, dateTimestamp = timestamp))
            mealDao.deleteMealItems(id)
            val itemEntities = items.map { (foodId, recipeId, weight) ->
                MealItemEntity(mealId = id, foodId = foodId, recipeId = recipeId, weightGrams = weight)
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
