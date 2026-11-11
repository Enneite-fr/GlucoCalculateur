package com.example.glucocalculateur.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodNamesTest {
    @Test
    fun cleanRemovesBomAndExtraSpaces() {
        assertEquals("Pâte brisée", FoodNames.clean("\uFEFF  Pâte   brisée "))
    }

    @Test
    fun keyIgnoresCaseIncludingAccentedLetters() {
        assertEquals(FoodNames.key("ÉPINARD cru"), FoodNames.key(" épinard  Cru"))
    }
}
