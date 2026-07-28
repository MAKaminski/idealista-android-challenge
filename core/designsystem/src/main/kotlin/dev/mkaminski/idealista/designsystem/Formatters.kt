package dev.mkaminski.idealista.designsystem

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formatting shared by every screen so the same number or date never renders two ways.
 *
 * Locale and zone are parameters rather than globals so tests can pin them (ADR-0007).
 */
object Formatters {

    /** `1.195.000 €` — the API sends the symbol as a *suffix*, so it is appended, not prefixed. */
    fun price(amount: Double, currencySuffix: String, locale: Locale = Locale.getDefault()): String {
        val number = NumberFormat.getIntegerInstance(locale).format(amount)
        return if (currencySuffix.isBlank()) number else "$number $currencySuffix"
    }

    fun favoritedDate(
        instant: Instant,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(zone)
        .format(instant)

    fun area(squareMeters: Double, locale: Locale = Locale.getDefault()): String =
        "${NumberFormat.getIntegerInstance(locale).format(squareMeters)} m²"
}
