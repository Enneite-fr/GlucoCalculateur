package com.example.glucocalculateur.data.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {

    @Test
    fun excelFileWithBomHeaderAndDecimalComma() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "Nom;Glucides\r\nPomme;12,5\r\n\"Pâte; fraîche\";30\r\n".toByteArray(Charsets.UTF_8)
        val parsed = CsvCodec.parseFoods(TextDecoding.decode(bytes))
        assertEquals(listOf(FoodRow("Pomme", 12.5), FoodRow("Pâte; fraîche", 30.0)), parsed.rows)
        assertTrue(parsed.issues.isEmpty())
    }

    @Test
    fun bomDoesNotSurviveWithoutHeader() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Pomme;12".toByteArray()
        assertEquals("Pomme", CsvCodec.parseFoods(TextDecoding.decode(bytes)).rows.single().name)
    }

    @Test
    fun latin1FallbackKeepsAccents() {
        val bytes = "Pâté;1,5\nÉpinard;2\n".toByteArray(Charsets.ISO_8859_1)
        val rows = CsvCodec.parseFoods(TextDecoding.decode(bytes)).rows
        assertEquals(listOf("Pâté", "Épinard"), rows.map { it.name })
    }

    @Test
    fun invalidLinesAreReportedWithLineNumbers() {
        val text = "Pomme;12\nPoire\nKiwi;abc\nFraise;150\n# commentaire\n\n  Banane ; 20  "
        val parsed = CsvCodec.parseFoods(text)
        assertEquals(listOf(FoodRow("Pomme", 12.0), FoodRow("Banane", 20.0)), parsed.rows)
        assertEquals(listOf(2, 3, 4), parsed.issues.map { it.line })
        assertEquals(
            listOf(ImportIssue.Type.MALFORMED_LINE, ImportIssue.Type.INVALID_NUMBER, ImportIssue.Type.INVALID_NUMBER),
            parsed.issues.map { it.type }
        )
    }

    @Test
    fun wholeLineQuotedAsInOldExports() {
        assertEquals(FoodRow("Pomme", 12.0), CsvCodec.parseFoods("\"Pomme;12\"").rows.single())
    }

    @Test
    fun recipesParse() {
        val parsed = CsvCodec.parseRecipes("Recette;Ingrédient;Poids\nGratin;Pomme de terre;500\nGratin;Crème;0\n")
        assertEquals(listOf(RecipeRow("Gratin", "Pomme de terre", 500.0)), parsed.rows)
        assertEquals(1, parsed.issues.size)
    }

    @Test
    fun writeThenReadRoundTrip() {
        val foods = listOf(FoodRow("Pomme", 12.5), FoodRow("Sauce \"maison\"; tomate", 7.0))
        val text = CsvCodec.writeFoods(foods, listOf("Aliment", "Glucides"), ',')
        assertTrue(text.startsWith("\uFEFFAliment;Glucides\r\n"))
        assertTrue(text.contains("Pomme;12,5\r\n"))
        assertEquals(foods, CsvCodec.parseFoods(TextDecoding.decode(text.toByteArray())).rows)

        val recipes = listOf(RecipeRow("Gratin", "Crème", 200.0))
        val recipeText = CsvCodec.writeRecipes(recipes, listOf("R", "I", "P"), '.')
        assertEquals(recipes, CsvCodec.parseRecipes(recipeText).rows)
    }

    @Test
    fun numbers() {
        assertEquals(1234.5, FileNumbers.parse(" 1 234,5 ")!!, 1e-9)
        assertEquals(null, FileNumbers.parse("NaN"))
        assertEquals("12,5", FileNumbers.format(12.50, ','))
        assertEquals("100", FileNumbers.format(100.0, ','))
        assertEquals("0.001", FileNumbers.format(0.001, '.'))
    }
}
