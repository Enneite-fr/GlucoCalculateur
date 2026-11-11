package com.example.glucocalculateur.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Conversions de dates.
 *
 * Les `DatePicker` Material manipulent des millisecondes à **minuit UTC** ; les repas sont horodatés
 * en millisecondes absolues et affichés dans le **fuseau local**. On ne passe de l'un à l'autre que
 * par un [LocalDate], jamais en relisant des millisecondes dans le mauvais fuseau.
 */
object DateUtils {
    fun pickerMillisToDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun dateToPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun localDate(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun localTime(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalTime =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()

    /** Horodatage du jour [date] à l'heure [time], dans le fuseau [zone]. */
    fun at(date: LocalDate, time: LocalTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()

    /** Même heure locale, autre jour. */
    fun withDate(timestamp: Long, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        at(date, localTime(timestamp, zone), zone)

    /** Même jour local, autre heure. */
    fun withTime(timestamp: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        at(localDate(timestamp, zone), LocalTime.of(hour, minute), zone)
}
