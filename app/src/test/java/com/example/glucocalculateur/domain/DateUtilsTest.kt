package com.example.glucocalculateur.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DateUtilsTest {
    private val paris = ZoneId.of("Europe/Paris")
    private val newYork = ZoneId.of("America/New_York")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun pickerRoundTripIsZoneIndependent() {
        val date = LocalDate.of(2026, 11, 11)
        assertEquals(date, DateUtils.pickerMillisToDate(DateUtils.dateToPickerMillis(date)))
    }

    @Test
    fun justAfterMidnightInParisStaysOnTheSameDay() {
        // 00:30 à Paris = 23:30 UTC la veille : le jour local doit rester le 11.
        val ts = DateUtils.at(LocalDate.of(2026, 11, 11), LocalTime.of(0, 30), paris)
        assertEquals(LocalDate.of(2026, 11, 11), DateUtils.localDate(ts, paris))
        val pickerMillis = DateUtils.dateToPickerMillis(DateUtils.localDate(ts, paris))
        assertEquals(LocalDate.of(2026, 11, 11), DateUtils.pickerMillisToDate(pickerMillis))
    }

    @Test
    fun negativeOffsetZoneKeepsSelectedDay() {
        val selected = LocalDate.of(2026, 3, 1)
        val pickerMillis = DateUtils.dateToPickerMillis(selected)
        val ts = DateUtils.withDate(DateUtils.at(LocalDate.of(2026, 2, 20), LocalTime.of(20, 15), newYork),
            DateUtils.pickerMillisToDate(pickerMillis), newYork)
        assertEquals(selected, DateUtils.localDate(ts, newYork))
        assertEquals(LocalTime.of(20, 15), DateUtils.localTime(ts, newYork))
    }

    @Test
    fun positiveOffsetZone() {
        val ts = DateUtils.at(LocalDate.of(2026, 1, 1), LocalTime.of(8, 0), tokyo)
        assertEquals(LocalDate.of(2026, 1, 1), DateUtils.localDate(ts, tokyo))
    }

    @Test
    fun withTimeAcrossDaylightSavingChange() {
        // 29 mars 2026 : passage à l'heure d'été à Paris (02:00 -> 03:00).
        val day = LocalDate.of(2026, 3, 29)
        val morning = DateUtils.at(day, LocalTime.of(1, 0), paris)
        val evening = DateUtils.withTime(morning, 21, 45, paris)
        assertEquals(day, DateUtils.localDate(evening, paris))
        assertEquals(LocalTime.of(21, 45), DateUtils.localTime(evening, paris))
        // Heure inexistante (02:30) : décalée à 03:30, même jour.
        assertEquals(LocalTime.of(3, 30), DateUtils.localTime(DateUtils.withTime(morning, 2, 30, paris), paris))
    }
}
