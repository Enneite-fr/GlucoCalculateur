package com.example.glucocalculateur.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food ORDER BY name ASC")
    fun getAllFood(): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Delete
    suspend fun deleteFood(food: FoodEntity)
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipesWithComponents(): Flow<List<RecipeWithComponents>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeWithComponents(recipeId: Long): RecipeWithComponents

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeComponents(components: List<RecipeComponentEntity>)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)
}

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY dateTimestamp DESC")
    fun getAllMealsWithItems(): Flow<List<MealWithItems>>

    @Insert
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealItemEntity>)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)
}

data class RecipeWithComponents(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val components: List<RecipeComponentEntity>
)

data class MealWithItems(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val items: List<MealItemEntity>
)
