package com.example.glucocalculateur.data

import androidx.room.withTransaction
import com.example.glucocalculateur.data.io.Backup
import com.example.glucocalculateur.data.io.BackupComponent
import com.example.glucocalculateur.data.io.BackupMeal
import com.example.glucocalculateur.data.io.BackupMealItem
import com.example.glucocalculateur.data.io.BackupRecipe
import com.example.glucocalculateur.data.io.FoodRow
import com.example.glucocalculateur.data.io.ImportIssue
import com.example.glucocalculateur.data.io.ImportReport
import com.example.glucocalculateur.data.io.RecipeRow
import com.example.glucocalculateur.domain.CarbCalculator
import kotlinx.coroutines.flow.Flow

sealed interface SaveFoodResult {
    data class Saved(val id: Long) : SaveFoodResult
    /** Un autre aliment porte déjà ce nom (à la casse près). */
    data class NameTaken(val existing: FoodEntity) : SaveFoodResult
}

/**
 * Point d'accès unique aux données : le ViewModel ne manipule ni les DAO ni [AppDatabase].
 * Toute écriture en plusieurs étapes est faite dans une transaction.
 */
class GlucoRepository(private val db: AppDatabase) {
    private val foodDao = db.foodDao()
    private val recipeDao = db.recipeDao()
    private val mealDao = db.mealDao()

    val foods: Flow<List<FoodEntity>> = foodDao.observeAll()
    val recipes: Flow<List<RecipeWithComponents>> = recipeDao.observeAllWithComponents()
    val meals: Flow<List<MealWithItems>> = mealDao.observeAllWithItems()

    // --- Aliments ---

    suspend fun findFoodByName(name: String, excludingId: Long? = null): FoodEntity? {
        val key = FoodNames.key(name)
        return foodDao.getAll().firstOrNull { it.id != excludingId && FoodNames.key(it.name) == key }
    }

    /** Crée (`id == null`) ou modifie un aliment ; refuse un nom déjà porté par un autre aliment. */
    suspend fun saveFood(id: Long?, name: String, carbsPer100g: Double): SaveFoodResult = db.withTransaction {
        val cleanName = FoodNames.clean(name)
        val conflict = findFoodByName(cleanName, excludingId = id)
        if (conflict != null) {
            SaveFoodResult.NameTaken(conflict)
        } else if (id == null) {
            SaveFoodResult.Saved(foodDao.insert(FoodEntity(name = cleanName, carbsPer100g = carbsPer100g)))
        } else {
            foodDao.update(FoodEntity(id = id, name = cleanName, carbsPer100g = carbsPer100g))
            SaveFoodResult.Saved(id)
        }
    }

    suspend fun countRecipesUsingFood(foodId: Long): Int = foodDao.getRecipesUsingFood(foodId).size

    /**
     * Supprime l'aliment et les recettes qui l'utilisent. Les repas sont conservés : leurs éléments
     * gardent une copie du nom et des glucides de l'aliment ou de la recette supprimés.
     */
    suspend fun deleteFood(foodId: Long) = db.withTransaction {
        val foodMap = foodDao.getAll().associateBy { it.id }
        val food = foodMap[foodId] ?: return@withTransaction
        val recipeIds = foodDao.getRecipesUsingFood(foodId).map { it.id }.toSet()
        recipeDao.getAllWithComponents()
            .filter { it.recipe.id in recipeIds }
            .forEach { detachAndDeleteRecipe(it, foodMap) }
        mealDao.detachFood(food.id, food.name, food.carbsPer100g)
        foodDao.deleteById(food.id)
    }

    // --- Recettes ---

    suspend fun saveRecipe(id: Long?, name: String, components: List<RecipeComponentDraft>): Long =
        recipeDao.upsertRecipeWithComponents(id, FoodNames.clean(name), components)

    suspend fun countMealsUsingRecipe(recipeId: Long): Int = recipeDao.countMealsUsingRecipe(recipeId)

    /** Supprime la recette ; les repas qui l'utilisent en gardent une copie. */
    suspend fun deleteRecipe(recipeId: Long) = db.withTransaction {
        val foodMap = foodDao.getAll().associateBy { it.id }
        val recipe = recipeDao.getAllWithComponents().firstOrNull { it.recipe.id == recipeId }
            ?: return@withTransaction
        detachAndDeleteRecipe(recipe, foodMap)
    }

    private suspend fun detachAndDeleteRecipe(recipe: RecipeWithComponents, foodMap: Map<Long, FoodEntity>) {
        val carbsPer100g = CarbCalculator.recipeCarbsPer100g(recipe.components, foodMap)
        mealDao.detachRecipe(recipe.recipe.id, recipe.recipe.name, carbsPer100g)
        recipeDao.deleteById(recipe.recipe.id)
    }

    // --- Repas ---

    suspend fun saveMeal(id: Long?, name: String, timestamp: Long, items: List<MealItemDraft>): Long =
        mealDao.upsertMealWithItems(id, FoodNames.clean(name), timestamp, items)

    /** Supprime le repas et renvoie son contenu, pour pouvoir annuler. */
    suspend fun deleteMeal(mealId: Long): MealWithItems? = db.withTransaction {
        val meal = mealDao.getWithItems(mealId)
        mealDao.deleteById(mealId)
        meal
    }

    suspend fun restoreMeal(meal: MealWithItems) = mealDao.restore(meal)

    // --- Export ---

    /** Instantané complet de la base, lu directement dans les DAO. */
    suspend fun exportBackup(): Backup = db.withTransaction {
        val foods = foodDao.getAll()
        val foodMap = foods.associateBy { it.id }
        val recipes = recipeDao.getAllWithComponents()
        val recipeMap = recipes.associateBy { it.recipe.id }
        Backup(
            foods = foods.map { FoodRow(it.name, it.carbsPer100g) },
            recipes = recipes.map { recipe ->
                BackupRecipe(
                    name = recipe.recipe.name,
                    components = recipe.components.mapNotNull { comp ->
                        foodMap[comp.foodId]?.let { BackupComponent(it.name, comp.weightGrams) }
                    }
                )
            },
            meals = mealDao.getAllWithItems().map { meal ->
                BackupMeal(
                    name = meal.meal.name,
                    timestamp = meal.meal.dateTimestamp,
                    items = meal.items.map { item ->
                        BackupMealItem(
                            foodName = item.foodId?.let { foodMap[it]?.name },
                            recipeName = item.recipeId?.let { recipeMap[it]?.recipe?.name },
                            label = MealLabels.of(item, foodMap, recipeMap),
                            carbsPer100g = CarbCalculator.mealItemCarbsPer100g(item, foodMap, recipeMap),
                            weightGrams = item.weightGrams
                        )
                    }
                )
            }
        )
    }

    // --- Import ---

    suspend fun importFoods(rows: List<FoodRow>): ImportReport = db.withTransaction { upsertFoods(rows) }

    suspend fun importRecipes(rows: List<RecipeRow>): ImportReport = db.withTransaction {
        val recipes = rows.groupBy { FoodNames.key(it.recipeName) }.values.map { group ->
            BackupRecipe(group.first().recipeName, group.map { BackupComponent(it.foodName, it.weightGrams) })
        }
        upsertRecipes(recipes)
    }

    suspend fun importBackup(backup: Backup): ImportReport = db.withTransaction {
        upsertFoods(backup.foods) + upsertRecipes(backup.recipes) + insertMeals(backup.meals)
    }

    private suspend fun upsertFoods(rows: List<FoodRow>): ImportReport {
        val existing = foodDao.getAll().associateBy { FoodNames.key(it.name) }.toMutableMap()
        var added = 0
        var updated = 0
        var unchanged = 0
        // Dans un même fichier, la dernière ligne d'un nom l'emporte.
        rows.associateBy { FoodNames.key(it.name) }.forEach { (key, row) ->
            val current = existing[key]
            when {
                current == null -> {
                    val id = foodDao.insert(FoodEntity(name = FoodNames.clean(row.name), carbsPer100g = row.carbsPer100g))
                    existing[key] = FoodEntity(id, FoodNames.clean(row.name), row.carbsPer100g)
                    added++
                }
                current.carbsPer100g != row.carbsPer100g -> {
                    foodDao.update(current.copy(carbsPer100g = row.carbsPer100g))
                    updated++
                }
                else -> unchanged++
            }
        }
        return ImportReport(added = added, updated = updated, unchanged = unchanged)
    }

    /**
     * Tous les ingrédients sont résolus avant d'écrire : une recette dont un ingrédient est inconnu
     * est laissée intacte et signalée.
     */
    private suspend fun upsertRecipes(recipes: List<BackupRecipe>): ImportReport {
        val foodsByKey = foodDao.getAll().associateBy { FoodNames.key(it.name) }
        val existing = recipeDao.getAllWithComponents().associateBy { FoodNames.key(it.recipe.name) }
        var added = 0
        var updated = 0
        var unchanged = 0
        val issues = mutableListOf<ImportIssue>()
        recipes.associateBy { FoodNames.key(it.name) }.forEach { (key, recipe) ->
            val missing = recipe.components.filter { foodsByKey[FoodNames.key(it.foodName)] == null }
            if (missing.isNotEmpty() || recipe.components.isEmpty()) {
                issues += ImportIssue(
                    ImportIssue.Type.UNKNOWN_INGREDIENTS,
                    subject = recipe.name,
                    detail = missing.map { it.foodName }.distinct().joinToString(", ")
                )
                return@forEach
            }
            val drafts = recipe.components.map {
                RecipeComponentDraft(foodsByKey.getValue(FoodNames.key(it.foodName)).id, it.weightGrams)
            }
            val current = existing[key]
            val currentDrafts = current?.components?.map { RecipeComponentDraft(it.foodId, it.weightGrams) }
            when {
                current == null -> {
                    recipeDao.upsertRecipeWithComponents(null, FoodNames.clean(recipe.name), drafts)
                    added++
                }
                currentDrafts == drafts -> unchanged++
                else -> {
                    recipeDao.upsertRecipeWithComponents(current.recipe.id, current.recipe.name, drafts)
                    updated++
                }
            }
        }
        return ImportReport(added = added, updated = updated, unchanged = unchanged, issues = issues)
    }

    /** Ajoute les repas absents (même nom, même horodatage = déjà présent). */
    private suspend fun insertMeals(meals: List<BackupMeal>): ImportReport {
        if (meals.isEmpty()) return ImportReport()
        val foodsByKey = foodDao.getAll().associateBy { FoodNames.key(it.name) }
        val recipesByKey = recipeDao.getAllWithComponents().associateBy { FoodNames.key(it.recipe.name) }
        val existing = mealDao.getAllWithItems()
            .map { FoodNames.key(it.meal.name) to it.meal.dateTimestamp }
            .toMutableSet()
        var added = 0
        var alreadyPresent = 0
        val issues = mutableListOf<ImportIssue>()
        meals.forEach { meal ->
            val identity = FoodNames.key(meal.name) to meal.timestamp
            if (identity in existing) {
                alreadyPresent++
                return@forEach
            }
            val items = meal.items.mapNotNull { item ->
                val food = item.foodName?.let { foodsByKey[FoodNames.key(it)] }
                val recipe = item.recipeName?.let { recipesByKey[FoodNames.key(it)] }
                when {
                    food != null -> MealItemDraft.Food(food.id, item.weightGrams)
                    recipe != null -> MealItemDraft.Recipe(recipe.recipe.id, item.weightGrams)
                    item.carbsPer100g != null -> MealItemDraft.Snapshot(
                        name = item.label ?: item.foodName ?: item.recipeName.orEmpty(),
                        carbsPer100g = item.carbsPer100g,
                        weightGrams = item.weightGrams
                    )
                    else -> null
                }
            }
            if (items.isEmpty()) {
                issues += ImportIssue(ImportIssue.Type.EMPTY_MEAL, subject = meal.name)
                return@forEach
            }
            if (items.size < meal.items.size) {
                issues += ImportIssue(ImportIssue.Type.PARTIAL_MEAL, subject = meal.name)
            }
            mealDao.upsertMealWithItems(null, FoodNames.clean(meal.name), meal.timestamp, items)
            existing += identity
            added++
        }
        return ImportReport(added = added, alreadyPresent = alreadyPresent, issues = issues)
    }
}

/** Libellé d'un élément de repas : nom de l'aliment, de la recette, ou copie conservée. */
object MealLabels {
    fun of(
        item: MealItemEntity,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): String? = item.foodId?.let { foodMap[it]?.name }
        ?: item.recipeId?.let { recipeMap[it]?.recipe?.name }
        ?: item.snapshotName

    fun of(
        draft: MealItemDraft,
        foodMap: Map<Long, FoodEntity>,
        recipeMap: Map<Long, RecipeWithComponents>
    ): String? = when (draft) {
        is MealItemDraft.Food -> foodMap[draft.foodId]?.name
        is MealItemDraft.Recipe -> recipeMap[draft.recipeId]?.recipe?.name
        is MealItemDraft.Snapshot -> draft.name
    }
}
