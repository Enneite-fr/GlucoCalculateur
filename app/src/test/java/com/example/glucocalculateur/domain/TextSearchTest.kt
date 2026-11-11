package com.example.glucocalculateur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TextSearchTest {
    @Test
    fun ignoresAccentsCaseAndWordOrder() {
        val name = TextSearch.normalize("Pâte brisée, crue")
        assertTrue(TextSearch.matches(name, "pate"))
        assertTrue(TextSearch.matches(name, "CRUE  brisee"))
        assertFalse(TextSearch.matches(name, "feuilletée"))
    }

    @Test
    fun handlesLigatures() {
        assertTrue(TextSearch.matches(TextSearch.normalize("Œuf, dur"), "oeuf"))
    }

    @Test
    fun collatorSortsAccentedLettersInPlace() {
        val sorted = listOf("Zeste", "Épinard", "Abricot", "endive").sortedWith(TextSearch.collator(Locale.FRANCE))
        assertEquals(listOf("Abricot", "endive", "Épinard", "Zeste"), sorted)
    }
}
