package dev.mkaminski.idealista.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.mkaminski.idealista.data.translate.CurrentLanguage
import dev.mkaminski.idealista.designsystem.AppLocales
import javax.inject.Singleton

/**
 * Joins the two halves of the language feature.
 *
 * `:core:data` declares what it needs (a language to translate into) and `:core:designsystem` owns
 * how the platform stores it; only `:app` sees both, so the binding lives here rather than forcing
 * a dependency between them.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocaleModule {

    @Provides
    @Singleton
    fun provideCurrentLanguage(): CurrentLanguage = CurrentLanguage { AppLocales.current() }
}
