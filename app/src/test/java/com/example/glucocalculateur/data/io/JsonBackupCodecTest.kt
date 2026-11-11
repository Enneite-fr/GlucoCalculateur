package com.example.glucocalculateur.data.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonBackupCodecTest {

    @Test
    fun roundTrip() {
        val backup = Backup(
            foods = listOf(FoodRow("Pomme", 12.5), FoodRow("Riz", 28.0)),
            recipes = listOf(BackupRecipe("Riz aux pommes", listOf(BackupComponent("Riz", 100.0), BackupComponent("Pomme", 50.0)))),
            meals = listOf(
                BackupMeal(
                    name = "Déjeuner",
                    timestamp = 1_700_000_000_000,
                    items = listOf(
                        BackupMealItem(foodName = "Pomme", label = "Pomme", carbsPer100g = 12.5, weightGrams = 150.0),
                        BackupMealItem(recipeName = "Riz aux pommes", label = "Riz aux pommes", carbsPer100g = 22.83, weightGrams = 200.0),
                        BackupMealItem(label = "Pain (supprimé)", carbsPer100g = 50.0, weightGrams = 40.0)
                    )
                )
            )
        )
        val decoded = JsonBackupCodec.decode(JsonBackupCodec.encode(backup))
        assertEquals(backup, decoded.backup)
        assertTrue(decoded.issues.isEmpty())
    }

    @Test
    fun readsVersion1Files() {
        val v1 = """
            {
              "foods": [{"name": "Pomme", "carbsPer100g": 12.5}, {"name": "", "carbsPer100g": 3}],
              "recipes": [{"name": "Compote", "components": [{"foodName": "Pomme", "weightGrams": 300}]}]
            }
        """.trimIndent()
        val decoded = JsonBackupCodec.decode(v1)
        assertEquals(listOf(FoodRow("Pomme", 12.5)), decoded.backup.foods)
        assertEquals(listOf(BackupRecipe("Compote", listOf(BackupComponent("Pomme", 300.0)))), decoded.backup.recipes)
        assertTrue(decoded.backup.meals.isEmpty())
        assertEquals(1, decoded.issues.size)
    }

    @Test
    fun encodesVersion() {
        assertTrue(JsonBackupCodec.encode(Backup()).contains("\"version\": 2"))
    }
}
