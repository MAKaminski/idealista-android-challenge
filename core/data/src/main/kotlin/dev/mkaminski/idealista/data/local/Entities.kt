package dev.mkaminski.idealista.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached `list.json` ad. Room is the single source of truth: the network writes here and the UI
 * reads from here, which is what gives offline browsing and instant favorite feedback (ADR-0004).
 */
@Entity(tableName = "ads")
internal data class AdEntity(
    @PrimaryKey @ColumnInfo(name = "property_code") val propertyCode: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
    val price: Double,
    @ColumnInfo(name = "currency_suffix") val currencySuffix: String,
    val operation: String,
    @ColumnInfo(name = "property_type") val propertyType: String,
    val size: Double,
    val rooms: Int,
    val bathrooms: Int,
    val exterior: Boolean,
    val floor: String?,
    val address: String,
    val neighborhood: String?,
    val district: String?,
    val municipality: String?,
    val province: String?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String,
    @ColumnInfo(name = "has_air_conditioning") val hasAirConditioning: Boolean,
    @ColumnInfo(name = "has_box_room") val hasBoxRoom: Boolean,
    @ColumnInfo(name = "has_swimming_pool") val hasSwimmingPool: Boolean,
    @ColumnInfo(name = "has_terrace") val hasTerrace: Boolean,
    @ColumnInfo(name = "has_garden") val hasGarden: Boolean,
    @ColumnInfo(name = "has_parking") val hasParking: Boolean?,
    @ColumnInfo(name = "parking_included_in_price") val parkingIncludedInPrice: Boolean?,
    /** JSON, via [Converters]. A separate images table would buy nothing for a read-only cache. */
    val images: String,
)

/**
 * A favorited ad and **when** it was favorited — the date the challenge requires on screen.
 * Kept in its own table so a cache refresh can never drop a user's favorites.
 */
@Entity(tableName = "favorites")
internal data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "property_code") val propertyCode: String,
    /** Epoch millis; rendered with java.time (ADR-0007). */
    @ColumnInfo(name = "favorited_at") val favoritedAt: Long,
)
