package dev.mkaminski.idealista.designsystem

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.mkaminski.idealista.model.AppLanguage

/**
 * Per-app language, through the platform's own mechanism rather than a home-made one.
 *
 * `AppCompatDelegate.setApplicationLocales` is stored **by the system** on API 33+ (so the choice
 * also appears in Android's own per-app language settings) and backported by AppCompat below that,
 * via the `autoStoreLocales` service declared in the app manifest. Either way persistence is not
 * ours to implement — a private SharedPreference would have been a second source of truth that
 * disagrees with the system screen (ADR-0009).
 *
 * Applying a locale recreates the activity; that is the framework's doing and is why nothing here
 * needs to push a change into the UI.
 */
object AppLocales {

    /** The language in force, or `null` when the app is following the system. */
    fun current(): AppLanguage? =
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    /** Passing `null` clears the override and hands the choice back to the system. */
    fun apply(language: AppLanguage?) {
        AppCompatDelegate.setApplicationLocales(
            language?.let { LocaleListCompat.forLanguageTags(it.tag) }
                ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }
}
