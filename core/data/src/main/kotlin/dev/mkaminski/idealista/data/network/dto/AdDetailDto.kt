package dev.mkaminski.idealista.data.network.dto

import kotlinx.serialization.Serializable

/**
 * `detail.json`.
 *
 * The endpoint ignores the requested id and always returns ad 1, so [adid], [priceInfo] and
 * [ubication] are parsed but **discarded** during mapping — the identity of the opened ad comes
 * from the cached list ad instead. See ADR-0005.
 */
@Serializable
internal data class AdDetailDto(
    val adid: Int = 0,
    val price: Double = 0.0,
    val priceInfo: DetailPriceInfoDto? = null,
    val operation: String? = null,
    val propertyType: String? = null,
    val extendedPropertyType: String? = null,
    val homeType: String? = null,
    val state: String? = null,
    val multimedia: DetailMultimediaDto? = null,
    val propertyComment: String = "",
    val ubication: UbicationDto? = null,
    val country: String? = null,
    val moreCharacteristics: MoreCharacteristicsDto? = null,
    val energyCertification: EnergyCertificationDto? = null,
)

/** `priceInfo.amount` — one level shallower than the list endpoint's `priceInfo.price.amount`. */
@Serializable
internal data class DetailPriceInfoDto(
    val amount: Double = 0.0,
    val currencySuffix: String = "",
)

@Serializable
internal data class UbicationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
internal data class DetailMultimediaDto(
    val images: List<DetailImageDto> = emptyList(),
)

@Serializable
internal data class DetailImageDto(
    val url: String,
    val tag: String? = null,
    val localizedName: String? = null,
    val multimediaId: Long? = null,
)

@Serializable
internal data class MoreCharacteristicsDto(
    val communityCosts: Double? = null,
    val roomNumber: Int? = null,
    val bathNumber: Int? = null,
    val exterior: Boolean? = null,
    val housingFurnitures: String? = null,
    val agencyIsABank: Boolean? = null,
    val energyCertificationType: String? = null,
    val flatLocation: String? = null,
    /** Epoch millis. */
    val modificationDate: Long? = null,
    val constructedArea: Int? = null,
    val lift: Boolean? = null,
    val boxroom: Boolean? = null,
    val isDuplex: Boolean? = null,
    val floor: String? = null,
    val status: String? = null,
)

@Serializable
internal data class EnergyCertificationDto(
    val title: String? = null,
    val energyConsumption: EnergyGradeDto? = null,
    val emissions: EnergyGradeDto? = null,
)

@Serializable
internal data class EnergyGradeDto(
    val type: String? = null,
)
