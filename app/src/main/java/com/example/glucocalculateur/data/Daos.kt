package com.example.glucocalculateur.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FoodDao {
    @Query("SELECT * FROM food ORDER BY name ASC")
    abstract fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food ORDER BY name ASC")
    abstract suspend fun getAll(): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAllIgnoringExisting(foods: List<FoodEntity>)

    @Update
    abstract suspend fun update(food: FoodEntity)

    @Query("DELETE FROM food WHERE id = :foodId")
    abstract suspend fun deleteById(foodId: Long)

    @Query(
        "SELECT DISTINCT r.* FROM recipes r " +
            "JOIN recipe_components rc ON r.id = rc.recipeId WHERE rc.foodId = :foodId"
    )
    abstract suspend fun getRecipesUsingFood(foodId: Long): List<RecipeEntity>
}

@Dao
abstract class RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    abstract fun observeAllWithComponents(): Flow<List<RecipeWithComponents>>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    abstract suspend fun getAllWithComponents(): List<RecipeWithComponents>

    @Insert
    abstract suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    abstract suspend fun updateRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertComponents(components: List<RecipeComponentEntity>)

    @Query("DELETE FROM recipe_components WHERE recipeId = :recipeId")
    abstract suspend fun deleteComponents(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    abstract suspend fun deleteById(recipeId: Long)

    @Query("SELECT COUNT(DISTINCT mealId) FROM meal_items WHERE recipeId = :recipeId")
    abstract suspend fun countMealsUsingRecipe(recipeId: Long): Int

    /**
     * Crée la recette (`recipeId == null`) ou la met à jour, et remplace ses ingrédients,
     * en une seule transaction.
     */
    @Transaction
    open suspend fun upsertRecipeWithComponents(
        recipeId: Long?,
        name: String,
        components: List<RecipeComponentDraft>
    ): Long {
        val id = if (recipeId == null) {
            insertRecipe(RecipeEntity(name = name))
        } else {
            updateRecipe(RecipeEntity(id = recipeId, name = name))
            deleteComponents(recipeId)
            recipeId
        }
        insertComponents(
            components.map { RecipeComponentEntity(recipeId = id, foodId = it.foodId, weightGrams = it.weightGrams) }
        )
        return id
    }
}

@Dao
abstract class MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY dateTimestamp DESC")
    abstract fun observeAllWithItems(): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meals ORDER BY dateTimestamp ASC")
    abstract suspend fun getAllWithItems(): List<MealWithItems>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    abstract suspend fun getWithItems(mealId: Long): MealWithItems?

    @Insert
    abstract suspend fun insertMeal(meal: MealEntity): Long

    @Update
    abstract suspend fun updateMeal(meal: MealEntity)

    @Insert
    abstract suspend fun insertItems(items: List<MealItemEntity>)

    @Query("DELETE FROM meal_items WHERE mealId = :mealId")
    abstract suspend fun deleteItems(mealId: Long)

    @Query("DELETE FROM meals WHERE id = :mealId")
    abstract suspend fun deleteById(mealId: Long)

    @Query(
        "UPDATE meal_items SET foodId = NULL, snapshotName = :name, snapshotCarbsPer100g = :carbsPer100g " +
            "WHERE foodId = :foodId"
    )
    abstract suspend fun detachFood(foodId: Long, name: String, carbsPer100g: Double)

    @Query(
        "UPDATE meal_items SET recipeId = NULL, snapshotName = :name, snapshotCarbsPer100g = :carbsPer100g " +
            "WHERE recipeId = :recipeId"
    )
    abstract suspend fun detachRecipe(recipeId: Long, name: String, carbsPer100g: Double)

    /** Crée le repas (`mealId == null`) ou le met à jour, et remplace ses éléments, en une seule transaction. */
    @Transaction
    open suspend fun upsertMealWithItems(
        mealId: Long?,
        name: String,
        timestamp: Long,
        items: List<MealItemDraft>
    ): Long {
        val id = if (mealId == null) {
            insertMeal(MealEntity(name = name, dateTimestamp = timestamp))
        } else {
            updateMeal(MealEntity(id = mealId, name = name, dateTimestamp = timestamp))
            deleteItems(mealId)
            mealId
        }
        insertItems(items.map { it.toEntity(id) })
        return id
    }

    /** Réinsère un repas supprimé, avec ses identifiants d'origine (annulation). */
    @Transaction
    open suspend fun restore(meal: MealWithItems) {
        insertMeal(meal.meal)
        insertItems(meal.items)
    }
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
