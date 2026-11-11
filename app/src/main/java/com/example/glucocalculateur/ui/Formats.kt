package com.example.glucocalculateur.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.glucocalculateur.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Langue courante de l'interface. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

@Composable
@ReadOnlyComposable
fun formatNumber(value: Double): String = Formatter.formatNumber(value, currentLocale())

/** « 150 g » */
@Composable
@ReadOnlyComposable
fun formatGrams(value: Double): String = stringResource(R.string.grams_format, formatNumber(value))

/** « 42 g de glucides » */
@Composable
@ReadOnlyComposable
fun formatCarbs(value: Double): String = stringResource(R.string.carbs_format, formatNumber(value))

/** « 12,5 g / 100 g » */
@Composable
@ReadOnlyComposable
fun formatCarbsPer100g(value: Double): String = stringResource(R.string.carbs_per_100g_short, formatNumber(value))

/** Formats de date et d'heure, selon la langue et le réglage 12/24 h du système. */
class DateFormats(private val locale: Locale, is24Hour: Boolean) {
    private val full = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    private val medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val monthYear = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLLLyyyy"), locale)
    private val time = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hma"), locale
    )

    fun fullDate(date: LocalDate): String = date.format(full).capitalized()
    fun mediumDate(date: LocalDate): String = date.format(medium)
    fun monthYear(date: LocalDate): String = date.format(monthYear).capitalized()
    fun time(time: LocalTime): String = time.format(this.time)
    fun narrowDayOfWeek(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)

    private fun String.capitalized() = replaceFirstChar { it.titlecase(locale) }
}

@Composable
fun rememberDateFormats(): DateFormats {
    val locale = currentLocale()
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) { DateFormats(locale, is24Hour) }
}

/** Nom de repas proposé selon l'heure. */
fun defaultMealNameRes(hour: Int): Int = when (hour) {
    in 5..9 -> R.string.breakfast
    in 10..14 -> R.string.lunch
    in 18..22 -> R.string.dinner
    else -> R.string.snack
}
