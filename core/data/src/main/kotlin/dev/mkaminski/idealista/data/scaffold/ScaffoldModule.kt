package dev.mkaminski.idealista.data.scaffold

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** See [FavoriteEntity] — scaffold only, replaced by the real DI modules at step 3. */
@Module
@InstallIn(SingletonComponent::class)
object ScaffoldModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScaffoldDatabase =
        Room.databaseBuilder(context, ScaffoldDatabase::class.java, "idealista.db").build()

    @Provides
    fun provideFavoriteDao(database: ScaffoldDatabase): FavoriteDao = database.favoriteDao()
}
