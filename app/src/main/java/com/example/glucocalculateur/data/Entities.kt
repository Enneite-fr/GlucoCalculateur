package com.example.glucocalculateur.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Représente un aliment de base avec son taux de glucides pour 100 g.
 * Le nom est unique (voir [FoodNames] pour la normalisation appliquée avant écriture).
 */
@Entity(
    tableName = "food",
    indices = [Index(value = ["name"], unique = true)]
)
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
 * Élément d'un repas : un aliment ou une recette, avec son poids.
 *
 * Quand l'aliment ou la recette référencé est supprimé, la référence passe à `null`
 * et l'élément conserve une copie de son nom et de ses glucides pour 100 g
 * ([snapshotName], [snapshotCarbsPer100g]) : l'historique des repas n'est pas perdu.
 */
@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("mealId"), Index("foodId"), Index("recipeId")]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long? = null,
    val recipeId: Long? = null,
    val weightGrams: Double,
    val snapshotName: String? = null,
    val snapshotCarbsPer100g: Double? = null
)
