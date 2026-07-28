package dev.mkaminski.idealista.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import dev.mkaminski.idealista.model.AdImage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Dao
internal interface AdDao {

    @Query("SELECT * FROM ads")
    fun observeAll(): Flow<List<AdEntity>>

    @Query("SELECT * FROM ads WHERE property_code = :propertyCode")
    fun observeByCode(propertyCode: String): Flow<AdEntity?>

    /** Upsert rather than replace: a refresh must not clear rows the response no longer carries. */
    @Upsert
    suspend fun upsertAll(ads: List<AdEntity>)
}

@Dao
internal interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY favorited_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE property_code = :propertyCode")
    suspend fun deleteByCode(propertyCode: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE property_code = :propertyCode)")
    suspend fun isFavorite(propertyCode: String): Boolean
}

/**
 * Images are stored as JSON through a storage-local record rather than by making [AdImage] itself
 * `@Serializable`: `:core:model` stays free of framework annotations so it keeps testing as plain
 * Kotlin (ADR-0002).
 */
internal class Converters {

    @Serializable
    private data class ImageRecord(val url: String, val tag: String?, val localizedName: String?)

    @TypeConverter
    fun imagesToJson(images: List<AdImage>): String =
        json.encodeToString(images.map { ImageRecord(it.url, it.tag, it.localizedName) })

    @TypeConverter
    fun jsonToImages(value: String): List<AdImage> =
        json.decodeFromString<List<ImageRecord>>(value)
            .map { AdImage(url = it.url, tag = it.tag, localizedName = it.localizedName) }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Database(
    entities = [AdEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
internal abstract class IdealistaDatabase : RoomDatabase() {
    abstract fun adDao(): AdDao
    abstract fun favoriteDao(): FavoriteDao
}
