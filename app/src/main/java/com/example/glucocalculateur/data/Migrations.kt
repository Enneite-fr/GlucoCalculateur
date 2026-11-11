package com.example.glucocalculateur.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 2 → 3.
 *
 * - D3 : noms d'aliments nettoyés (BOM, espaces), doublons fusionnés (même nom à la casse près),
 *   puis index unique sur `food.name`. Pour chaque groupe de doublons, on garde l'identifiant le plus
 *   ancien (les références y sont redirigées) et la valeur de glucides la plus récente.
 * - D2 : `meal_items` reçoit une copie du nom et des glucides de l'élément, et ses références vers
 *   `food` / `recipes` passent en `ON DELETE SET NULL` : supprimer un aliment ne supprime plus de repas.
 */
val MIGRATION_2_3: Migration = Migration2To3()

private class Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        mergeDuplicateFoods(db)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_name` ON `food` (`name`)")
        recreateMealItems(db)
    }

    private data class FoodRow(val id: Long, val name: String, val carbs: Double)

    private fun mergeDuplicateFoods(db: SupportSQLiteDatabase) {
        val foods = mutableListOf<FoodRow>()
        db.query("SELECT id, name, carbsPer100g FROM food ORDER BY id").use { c ->
            while (c.moveToNext()) foods += FoodRow(c.getLong(0), c.getString(1), c.getDouble(2))
        }
        foods.groupBy { FoodNames.key(it.name) }.values.forEach { group ->
            val keep = group.first()
            val latest = group.last()
            group.drop(1).forEach { duplicate ->
                db.execSQL("UPDATE recipe_components SET foodId = ? WHERE foodId = ?", arrayOf(keep.id, duplicate.id))
                db.execSQL("UPDATE meal_items SET foodId = ? WHERE foodId = ?", arrayOf(keep.id, duplicate.id))
                db.execSQL("DELETE FROM food WHERE id = ?", arrayOf(duplicate.id))
            }
            val cleanName = FoodNames.clean(keep.name)
            if (cleanName != keep.name || latest.carbs != keep.carbs) {
                db.execSQL(
                    "UPDATE food SET name = ?, carbsPer100g = ? WHERE id = ?",
                    arrayOf(cleanName, latest.carbs, keep.id)
                )
            }
        }
    }

    private fun recreateMealItems(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meal_items_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mealId` INTEGER NOT NULL, " +
                "`foodId` INTEGER, `recipeId` INTEGER, `weightGrams` REAL NOT NULL, " +
                "`snapshotName` TEXT, `snapshotCarbsPer100g` REAL, " +
                "FOREIGN KEY(`mealId`) REFERENCES `meals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`foodId`) REFERENCES `food`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL(
            "INSERT INTO `meal_items_new` (`id`, `mealId`, `foodId`, `recipeId`, `weightGrams`) " +
                "SELECT `id`, `mealId`, `foodId`, `recipeId`, `weightGrams` FROM `meal_items`"
        )
        db.execSQL("DROP TABLE `meal_items`")
        db.execSQL("ALTER TABLE `meal_items_new` RENAME TO `meal_items`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_items_mealId` ON `meal_items` (`mealId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_items_foodId` ON `meal_items` (`foodId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_items_recipeId` ON `meal_items` (`recipeId`)")
    }
}
