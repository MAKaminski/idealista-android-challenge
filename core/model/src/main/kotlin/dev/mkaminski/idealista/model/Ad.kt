package dev.mkaminski.idealista.model

import java.time.Instant

/**
 * A property ad as the app understands it, independent of which endpoint it came from.
 *
 * [favoritedAt] is null when the ad is not favorited. It is populated by the repository from the
 * favorites table rather than by any API response — see ADR-0004.
 */
data class Ad(
    val propertyCode: String,
    val thumbnailUrl: String?,
    val price: Double,
    val currencySuffix: String,
    val operation: Operation,
    val propertyType: String,
    val sizeSquareMeters: Double,
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
    val features: AdFeatures,
    val parking: ParkingSpace?,
    val images: List<AdImage>,
    val favoritedAt: Instant? = null,
) {
    val isFavorite: Boolean get() = favoritedAt != null
}

enum class Operation {
    SALE,
    RENT,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(raw: String?): Operation = when (raw?.lowercase()) {
            "sale" -> SALE
            "rent" -> RENT
            else -> UNKNOWN
        }
    }
}

/**
 * Amenity flags. The API sends a different subset of keys per ad — `hasSwimmingPool`, `hasTerrace`
 * and `hasGarden` appear on ad 4 only — so every flag defaults to false rather than being required.
 * See docs/API.md.
 */
data class AdFeatures(
    val hasAirConditioning: Boolean = false,
    val hasBoxRoom: Boolean = false,
    val hasSwimmingPool: Boolean = false,
    val hasTerrace: Boolean = false,
    val hasGarden: Boolean = false,
)

data class ParkingSpace(
    val hasParkingSpace: Boolean,
    val includedInPrice: Boolean,
)

data class AdImage(
    val url: String,
    val tag: String?,
    /** Present only on detail responses; the list endpoint sends `url` and `tag` alone. */
    val localizedName: String? = null,
)
