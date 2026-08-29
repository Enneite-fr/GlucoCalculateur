package com.example.glucocalculateur.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Représente un aliment de base avec son taux de glucides pour 100g.
 */
@Entity(tableName = "food")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val carbsPer100g: Double
)

/**
 * Représente une recette (plat composé de plusieurs aliments).
 */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/**
 * Liaison entre une recette et un aliment (ingrédient).
 */
@Entity(
    tableName = "recipe_components",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId"), Index("foodId")]
)
data class RecipeComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: Long,
    val weightGrams: Double
)

/**
 * Représente un repas consommé à une date donnée.
 */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateTimestamp: Long = System.currentTimeMillis()
)

/**
 * Liaison entre un repas et des aliments ou recettes.
 */
@Entity(tableName = "meal_items")
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long? = null,
    val recipeId: Long? = null,
    val weightGrams: Double
)
