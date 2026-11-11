package com.example.glucocalculateur.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FormatterTest {
    private val fr = Locale.FRANCE
    private val en = Locale.US

    @Test
    fun formatsWithLocaleSeparatorAndTwoDecimals() {
        assertEquals("12,35", Formatter.formatNumber(12.345, fr))
        assertEquals("12.35", Formatter.formatNumber(12.345, en))
        assertEquals("1500", Formatter.formatNumber(1500.0, fr))
        assertEquals("0", Formatter.formatNumber(-0.001, fr))
        assertEquals("0", Formatter.formatNumber(Double.NaN, fr))
    }

    @Test
    fun parsesCommaOrDot() {
        assertEquals(12.5, Formatter.parseDouble("12,5")!!, 0.0)
        assertEquals(12.5, Formatter.parseDouble(" 12.5 ")!!, 0.0)
        assertNull(Formatter.parseDouble(""))
        assertNull(Formatter.parseDouble("abc"))
        assertNull(Formatter.parseDouble("1e400"))
    }

    @Test
    fun sanitizesDecimalInput() {
        assertEquals("12,5", Formatter.sanitizeDecimalInput("12.5", fr))
        assertEquals("12.5", Formatter.sanitizeDecimalInput("12,5", en))
        assertEquals("12,34", Formatter.sanitizeDecimalInput("12,345", fr))
        assertEquals("1,23", Formatter.sanitizeDecimalInput("1.2.3", fr))
        assertNull(Formatter.sanitizeDecimalInput("12a", fr))
        assertNull(Formatter.sanitizeDecimalInput("-3", fr))
    }
}
