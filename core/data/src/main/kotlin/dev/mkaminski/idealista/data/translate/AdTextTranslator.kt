package dev.mkaminski.idealista.data.translate

import dev.mkaminski.idealista.model.AppLanguage

/**
 * Translates the ad text the API returns.
 *
 * The mock payloads are Spanish — descriptions, room names, the long comment — and localizing the
 * UI strings does not help someone who cannot read the listing itself. This is the seam that fixes
 * that, kept as an interface so ViewModels are testable without a translation engine (ADR-0011).
 */
interface AdTextTranslator {

    /**
     * Returns [text] in [target], or `null` when it cannot be translated — no model, no network on
     * first use, or the text is already in that language. `null` means "show the original", never
     * "show nothing".
     */
    suspend fun translate(text: String, target: AppLanguage): String?

    companion object {
        /** The language the mock API writes its ad content in. */
        val SOURCE: AppLanguage = AppLanguage.SPANISH
    }
}
