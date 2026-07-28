package dev.mkaminski.idealista.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.data.translate.AdTextTranslator
import dev.mkaminski.idealista.data.translate.MlKitAdTextTranslator
import dev.mkaminski.idealista.data.DefaultAdRepository
import dev.mkaminski.idealista.data.local.AdDao
import dev.mkaminski.idealista.data.local.Converters
import dev.mkaminski.idealista.data.local.FavoriteDao
import dev.mkaminski.idealista.data.local.IdealistaDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

/** Dispatchers are injected so no class under test is pinned to a real one. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IdealistaDatabase =
        Room.databaseBuilder(context, IdealistaDatabase::class.java, "idealista.db").build()

    @Provides
    fun provideAdDao(database: IdealistaDatabase): AdDao = database.adDao()

    @Provides
    fun provideFavoriteDao(database: IdealistaDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun provideConverters(): Converters = Converters()

    /** Injected rather than `Instant.now()` so favorite timestamps are deterministic in tests. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAdRepository(impl: DefaultAdRepository): AdRepository

    @Binds
    @Singleton
    abstract fun bindAdTextTranslator(impl: MlKitAdTextTranslator): AdTextTranslator
}
