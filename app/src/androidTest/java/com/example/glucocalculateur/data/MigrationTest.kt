package com.example.glucocalculateur.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate2To3_keepsDataAndMergesDuplicateFoods() {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO food (id, name, carbsPer100g) VALUES (1, 'Pomme', 12.0)")
            execSQL("INSERT INTO food (id, name, carbsPer100g) VALUES (2, '﻿pomme ', 13.0)")
            execSQL("INSERT INTO food (id, name, carbsPer100g) VALUES (3, 'Riz', 28.0)")
            execSQL("INSERT INTO recipes (id, name) VALUES (1, 'Compote')")
            execSQL("INSERT INTO recipe_components (recipeId, foodId, weightGrams) VALUES (1, 2, 100.0)")
            execSQL("INSERT INTO meals (id, name, dateTimestamp) VALUES (1, 'Déjeuner', 1000)")
            execSQL("INSERT INTO meal_items (id, mealId, foodId, weightGrams) VALUES (1, 1, 2, 150.0)")
            execSQL("INSERT INTO meal_items (id, mealId, recipeId, weightGrams) VALUES (2, 1, 1, 200.0)")
            execSQL("INSERT INTO meal_items (id, mealId, foodId, weightGrams) VALUES (3, 1, 3, 50.0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        db.query("SELECT id, name, carbsPer100g FROM food ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals(1L, c.getLong(0))
            assertEquals("Pomme", c.getString(1))
            assertEquals(13.0, c.getDouble(2), 0.0)
        }
        db.query("SELECT foodId FROM recipe_components").use { c ->
            c.moveToFirst()
            assertEquals(1L, c.getLong(0))
        }
        db.query("SELECT id, foodId, recipeId, weightGrams, snapshotName FROM meal_items ORDER BY id").use { c ->
            assertEquals(3, c.count)
            c.moveToFirst()
            assertEquals(1L, c.getLong(1))
            assertEquals(150.0, c.getDouble(3), 0.0)
            assertNull(c.getString(4))
            c.moveToNext()
            assertEquals(1L, c.getLong(2))
            c.moveToNext()
            assertEquals(3L, c.getLong(1))
        }
        db.query("SELECT COUNT(*) FROM meals").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }
}
