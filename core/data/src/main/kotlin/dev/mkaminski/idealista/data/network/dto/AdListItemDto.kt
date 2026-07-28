package dev.mkaminski.idealista.data.network.dto

import kotlinx.serialization.Serializable

/**
 * `list.json` item.
 *
 * Deliberately **not** shared with [AdDetailDto]: the id is a `String` here and an `Int` there, and
 * the price is nested one level deeper. Unifying them would hide exactly the difference that
 * ADR-0005 exists to handle. Every optional field is nullable with a safe default because the API
 * omits keys per ad — see docs/API.md.
 */
@Serializable
internal data class AdListItemDto(
    val propertyCode: String,
    val thumbnail: String? = null,
    val floor: String? = null,
    val price: Double = 0.0,
    val priceInfo: ListPriceInfoDto? = null,
    val propertyType: String? = null,
    val operation: String? = null,
    val size: Double = 0.0,
    val exterior: Boolean = false,
    val rooms: Int = 0,
    val bathrooms: Int = 0,
    val address: String = "",
    val province: String? = null,
    val municipality: String? = null,
    val district: String? = null,
    val neighborhood: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String = "",
    val features: FeaturesDto? = null,
    val parkingSpace: ParkingSpaceDto? = null,
    val multimedia: ListMultimediaDto? = null,
)

/** `priceInfo.price.amount` — note the extra nesting relative to the detail endpoint. */
@Serializable
internal data class ListPriceInfoDto(
    val price: PriceAmountDto? = null,
)

@Serializable
internal data class PriceAmountDto(
    val amount: Double = 0.0,
    val currencySuffix: String = "",
)

@Serializable
internal data class FeaturesDto(
    val hasAirConditioning: Boolean? = null,
    val hasBoxRoom: Boolean? = null,
    val hasSwimmingPool: Boolean? = null,
    val hasTerrace: Boolean? = null,
    val hasGarden: Boolean? = null,
)

@Serializable
internal data class ParkingSpaceDto(
    val hasParkingSpace: Boolean? = null,
    val isParkingSpaceIncludedInPrice: Boolean? = null,
)

@Serializable
internal data class ListMultimediaDto(
    val images: List<ListImageDto> = emptyList(),
)

@Serializable
internal data class ListImageDto(
    val url: String,
    val tag: String? = null,
)
