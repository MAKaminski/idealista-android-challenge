package dev.mkaminski.idealista.data.translate

import dev.mkaminski.idealista.model.AppLanguage

/**
 * The language the UI is currently showing, or `null` when the app is following the system.
 *
 * Declared here because this is where it is consumed, and implemented in `:app`, which is the only
 * module that knows about both the data layer and the design system. That keeps `:core:data` free
 * of AppCompat while still letting a ViewModel ask what language to translate into.
 */
fun interface CurrentLanguage {
    operator fun invoke(): AppLanguage?
}
