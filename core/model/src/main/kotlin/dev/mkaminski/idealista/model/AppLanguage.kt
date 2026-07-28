package dev.mkaminski.idealista.model

/**
 * The languages the app ships translations for.
 *
 * [endonym] is each language's name **in that language** on purpose: someone who lands in Italian by
 * accident can still find their way out, which they cannot if every option is written in Italian.
 *
 * A pure enum with no Android types, so tag parsing is testable on the JVM. Applying a selection is
 * `AppLocales`' job in `:core:designsystem`.
 */
enum class AppLanguage(val tag: String, val endonym: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    PORTUGUESE("pt", "Português"),
    ITALIAN("it", "Italiano"),
    ;

    companion object {
        /**
         * Resolves a BCP-47 tag to a shipped language, or `null` for "follow the system".
         *
         * Matches on the **primary subtag** so regional variants land somewhere sensible: `es-419`
         * and `es-ES` are both Spanish, and `pt-BR` is Portuguese even though the translations are
         * European. An empty tag means no explicit choice has been made.
         */
        fun fromTag(tag: String?): AppLanguage? {
            val primary = tag?.substringBefore('-')?.substringBefore('_')?.lowercase()
            if (primary.isNullOrBlank()) return null
            return entries.firstOrNull { it.tag == primary }
        }
    }
}
