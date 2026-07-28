package dev.mkaminski.idealista.data.scaffold

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Scaffold-only slice of the favorites feature, present so the build gate in step 1 of
 * docs/PLAN.md exercises Room's KSP processor alongside Hilt's. The real schema lands in
 * :core:data at step 3 and this package is deleted then.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "property_code") val propertyCode: String,
    @ColumnInfo(name = "favorited_at") val favoritedAt: Long,
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY favorited_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = true)
abstract class ScaffoldDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
