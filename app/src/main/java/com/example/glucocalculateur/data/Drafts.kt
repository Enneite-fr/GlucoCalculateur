package com.example.glucocalculateur.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Ingrédient d'une recette en cours d'édition. */
@Parcelize
data class RecipeComponentDraft(val foodId: Long, val weightGrams: Double) : Parcelable

/** Élément d'un repas en cours d'édition. */
sealed interface MealItemDraft : Parcelable {
    val weightGrams: Double
    fun withWeight(weightGrams: Double): MealItemDraft

    @Parcelize
    data class Food(val foodId: Long, override val weightGrams: Double) : MealItemDraft {
        override fun withWeight(weightGrams: Double) = copy(weightGrams = weightGrams)
    }

    @Parcelize
    data class Recipe(val recipeId: Long, override val weightGrams: Double) : MealItemDraft {
        override fun withWeight(weightGrams: Double) = copy(weightGrams = weightGrams)
    }

    /** Élément dont l'aliment ou la recette d'origine a été supprimé : seule sa copie subsiste. */
    @Parcelize
    data class Snapshot(
        val name: String,
        val carbsPer100g: Double,
        override val weightGrams: Double
    ) : MealItemDraft {
        override fun withWeight(weightGrams: Double) = copy(weightGrams = weightGrams)
    }

    companion object {
        fun from(item: MealItemEntity): MealItemDraft = when {
            item.foodId != null -> Food(item.foodId, item.weightGrams)
            item.recipeId != null -> Recipe(item.recipeId, item.weightGrams)
            else -> Snapshot(item.snapshotName.orEmpty(), item.snapshotCarbsPer100g ?: 0.0, item.weightGrams)
        }
    }
}

internal fun MealItemDraft.toEntity(mealId: Long): MealItemEntity = when (this) {
    is MealItemDraft.Food -> MealItemEntity(mealId = mealId, foodId = foodId, weightGrams = weightGrams)
    is MealItemDraft.Recipe -> MealItemEntity(mealId = mealId, recipeId = recipeId, weightGrams = weightGrams)
    is MealItemDraft.Snapshot -> MealItemEntity(
        mealId = mealId,
        weightGrams = weightGrams,
        snapshotName = name,
        snapshotCarbsPer100g = carbsPer100g
    )
}
