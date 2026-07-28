package dev.mkaminski.idealista.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.mkaminski.idealista.data.network.IdealistaApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val BASE_URL = "https://idealista.github.io/android-challenge/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The API adds keys per ad (features, parkingSpace) and could add more upstream; an
        // unexpected key must never crash the app. See docs/API.md.
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideIdealistaApi(retrofit: Retrofit): IdealistaApi = retrofit.create(IdealistaApi::class.java)
}
