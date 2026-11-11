package com.example.glucocalculateur.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.glucocalculateur.data.io.Backup
import com.example.glucocalculateur.data.io.BackupComponent
import com.example.glucocalculateur.data.io.BackupMeal
import com.example.glucocalculateur.data.io.BackupMealItem
import com.example.glucocalculateur.data.io.BackupRecipe
import com.example.glucocalculateur.data.io.FoodRow
import com.example.glucocalculateur.data.io.ImportIssue
import com.example.glucocalculateur.data.io.RecipeRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlucoRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: GlucoRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = GlucoRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addFood(name: String, carbs: Double): Long =
        (repository.saveFood(null, name, carbs) as SaveFoodResult.Saved).id

    @Test
    fun saveFoodRejectsSameNameIgnoringCase() = runBlocking {
        addFood("Épinard", 1.0)
        val result = repository.saveFood(null, "  épinard ", 2.0)
        assertTrue(result is SaveFoodResult.NameTaken)
        assertEquals(1, repository.foods.first().size)
    }

    @Test
    fun deletingFoodKeepsMealsWithSnapshot() = runBlocking {
        val apple = addFood("Pomme", 12.0)
        val rice = addFood("Riz", 28.0)
        val compote = repository.saveRecipe(null, "Compote", listOf(RecipeComponentDraft(apple, 100.0)))
        repository.saveRecipe(null, "Riz nature", listOf(RecipeComponentDraft(rice, 100.0)))
        repository.saveMeal(
            null, "Déjeuner", 1000L,
            listOf(
                MealItemDraft.Food(apple, 150.0),
                MealItemDraft.Recipe(compote, 200.0),
                MealItemDraft.Food(rice, 50.0)
            )
        )
        assertEquals(1, repository.countRecipesUsingFood(apple))

        repository.deleteFood(apple)

        assertEquals(listOf("Riz nature"), repository.recipes.first().map { it.recipe.name })
        val meal = repository.meals.first().single()
        assertEquals(3, meal.items.size)
        val appleItem = meal.items.first { it.weightGrams == 150.0 }
        assertNull(appleItem.foodId)
        assertEquals("Pomme", appleItem.snapshotName)
        assertEquals(12.0, appleItem.snapshotCarbsPer100g!!, 0.0)
        val compoteItem = meal.items.first { it.weightGrams == 200.0 }
        assertNull(compoteItem.recipeId)
        assertEquals("Compote", compoteItem.snapshotName)
        assertEquals(12.0, compoteItem.snapshotCarbsPer100g!!, 1e-9)
        assertEquals(rice, meal.items.first { it.weightGrams == 50.0 }.foodId)
    }

    @Test
    fun deleteAndRestoreMeal() = runBlocking {
        val apple = addFood("Pomme", 12.0)
        val id = repository.saveMeal(null, "Goûter", 1000L, listOf(MealItemDraft.Food(apple, 100.0)))
        val deleted = repository.deleteMeal(id)!!
        assertTrue(repository.meals.first().isEmpty())
        repository.restoreMeal(deleted)
        assertEquals(deleted, repository.meals.first().single())
    }

    @Test
    fun importRecipeWithUnknownIngredientLeavesExistingRecipeUntouched() = runBlocking {
        val apple = addFood("Pomme", 12.0)
        repository.saveRecipe(null, "Compote", listOf(RecipeComponentDraft(apple, 300.0)))

        val report = repository.importRecipes(
            listOf(RecipeRow("compote", "Pomme", 200.0), RecipeRow("Compote", "Sucre inconnu", 20.0))
        )

        assertEquals(ImportIssue.Type.UNKNOWN_INGREDIENTS, report.issues.single().type)
        assertEquals("Sucre inconnu", report.issues.single().detail)
        val recipe = repository.recipes.first().single()
        assertEquals(300.0, recipe.components.single().weightGrams, 0.0)
    }

    @Test
    fun importFoodsUpsertsIgnoringCase() = runBlocking {
        addFood("Pomme", 12.0)
        val report = repository.importFoods(listOf(FoodRow("POMME", 13.0), FoodRow("Poire", 10.0), FoodRow("poire", 11.0)))
        assertEquals(1, report.added)
        assertEquals(1, report.updated)
        val foods = repository.foods.first().associate { it.name to it.carbsPer100g }
        // Dans un même fichier, la dernière ligne d'un nom l'emporte (orthographe comprise).
        assertEquals(mapOf("poire" to 11.0, "Pomme" to 13.0), foods)
    }

    @Test
    fun backupExportImportRoundTripWithoutDuplicates() = runBlocking {
        val apple = addFood("Pomme", 12.0)
        val compote = repository.saveRecipe(null, "Compote", listOf(RecipeComponentDraft(apple, 300.0)))
        repository.saveMeal(null, "Goûter", 1000L, listOf(MealItemDraft.Recipe(compote, 150.0)))
        val backup = repository.exportBackup()

        val emptyDb = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
        try {
            val other = GlucoRepository(emptyDb)
            val first = other.importBackup(backup)
            assertEquals(3, first.added)
            assertEquals(backup, other.exportBackup())

            val second = other.importBackup(backup)
            assertEquals(0, second.added)
            assertEquals(1, second.alreadyPresent)
            assertEquals(1, other.meals.first().size)
        } finally {
            emptyDb.close()
        }
    }

    @Test
    fun importBackupFallsBackToSnapshotForMissingSources() = runBlocking {
        val backup = Backup(
            foods = emptyList(),
            recipes = listOf(BackupRecipe("Orpheline", listOf(BackupComponent("Absent", 10.0)))),
            meals = listOf(
                BackupMeal(
                    "Dîner", 2000L,
                    listOf(BackupMealItem(foodName = "Absent", label = "Absent", carbsPer100g = 20.0, weightGrams = 100.0))
                )
            )
        )
        val report = repository.importBackup(backup)
        assertEquals(1, report.added)
        assertEquals(ImportIssue.Type.UNKNOWN_INGREDIENTS, report.issues.single().type)
        val item = repository.meals.first().single().items.single()
        assertEquals("Absent", item.snapshotName)
        assertEquals(20.0, item.snapshotCarbsPer100g!!, 0.0)
    }
}
