package com.example.glucocalculateur.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food ORDER BY name ASC")
    fun getAllFood(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food")
    suspend fun getAllFoodSync(): List<FoodEntity>

    @Query("SELECT * FROM food WHERE name = :name LIMIT 1")
    suspend fun getFoodByNameSync(name: String): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllFoods(foods: List<FoodEntity>)

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("SELECT r.* FROM recipes r JOIN recipe_components rc ON r.id = rc.recipeId WHERE rc.foodId = :foodId")
    suspend fun getRecipesUsingFood(foodId: Long): List<RecipeEntity>

    @Query("SELECT m.* FROM meals m JOIN meal_items mi ON m.id = mi.mealId WHERE mi.foodId = :foodId")
    suspend fun getMealsUsingFood(foodId: Long): List<MealEntity>
}

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipesWithComponents(): Flow<List<RecipeWithComponents>>

    @Query("SELECT * FROM recipes WHERE name = :name LIMIT 1")
    suspend fun getRecipeByNameSync(name: String): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeComponents(components: List<RecipeComponentEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipe_components WHERE recipeId = :recipeId")
    suspend fun deleteRecipeComponents(recipeId: Long)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("SELECT m.* FROM meals m JOIN meal_items mi ON m.id = mi.mealId WHERE mi.recipeId = :recipeId")
    suspend fun getMealsUsingRecipe(recipeId: Long): List<MealEntity>
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

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("DELETE FROM meal_items WHERE mealId = :mealId")
    suspend fun deleteMealItems(mealId: Long)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM recipes WHERE id IN (SELECT recipeId FROM recipe_components WHERE foodId = :foodId)")
    suspend fun deleteRecipesUsingFood(foodId: Long)

    @Query("DELETE FROM meals WHERE id IN (SELECT mealId FROM meal_items WHERE foodId = :foodId)")
    suspend fun deleteMealsUsingFood(foodId: Long)

    @Query("DELETE FROM meals WHERE id IN (SELECT mealId FROM meal_items WHERE recipeId = :recipeId)")
    suspend fun deleteMealsUsingRecipe(recipeId: Long)
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
