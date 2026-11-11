package com.example.glucocalculateur.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodEntity::class,
        RecipeEntity::class,
        RecipeComponentEntity::class,
        MealEntity::class,
        MealItemEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao
    abstract fun mealDao(): MealDao

    companion object {
        private const val NAME = "gluco_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_2_3)
                // La version 1 n'a jamais été diffusée avec un schéma exporté : pas de migration possible.
                .fallbackToDestructiveMigrationFrom(1)
                .addCallback(SeedCallback)
                .build()
    }

    /**
     * Insère la base d'aliments initiale chaque fois que la table `food` est vide à l'ouverture
     * (première installation, ou base recréée), sans dépendre de l'instance Room.
     */
    private object SeedCallback : Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            val isEmpty = db.query("SELECT COUNT(*) FROM food").use { c -> c.moveToFirst() && c.getInt(0) == 0 }
            if (isEmpty) seedFoods(db)
        }
    }
}

internal fun seedFoods(db: SupportSQLiteDatabase) {
    db.beginTransaction()
    try {
        val statement = db.compileStatement("INSERT OR IGNORE INTO food (name, carbsPer100g) VALUES (?, ?)")
        INITIAL_FOODS.forEach { food ->
            statement.bindString(1, FoodNames.clean(food.name))
            statement.bindDouble(2, food.carbsPer100g)
            statement.executeInsert()
            statement.clearBindings()
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}
