package dev.mkaminski.idealista.data.network

import dev.mkaminski.idealista.data.network.dto.AdDetailDto
import dev.mkaminski.idealista.data.network.dto.AdListItemDto
import dev.mkaminski.idealista.data.network.dto.FeaturesDto
import dev.mkaminski.idealista.data.network.dto.ParkingSpaceDto
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdCharacteristics
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.AdImage
import dev.mkaminski.idealista.model.AdDetail
import dev.mkaminski.idealista.model.EnergyCertificate
import dev.mkaminski.idealista.model.Operation
import dev.mkaminski.idealista.model.ParkingSpace
import java.time.Instant

internal fun AdListItemDto.toDomain(): Ad = Ad(
    propertyCode = propertyCode,
    thumbnailUrl = thumbnail,
    // `priceInfo.price.amount` is authoritative; the flat `price` is a duplicate the API also sends.
    price = priceInfo?.price?.amount ?: price,
    currencySuffix = priceInfo?.price?.currencySuffix.orEmpty(),
    operation = Operation.fromApi(operation),
    propertyType = propertyType.orEmpty(),
    sizeSquareMeters = size,
    rooms = rooms,
    bathrooms = bathrooms,
    exterior = exterior,
    floor = floor,
    address = address,
    neighborhood = neighborhood,
    district = district,
    municipality = municipality,
    province = province,
    latitude = latitude,
    longitude = longitude,
    description = description,
    features = features.toDomain(),
    parking = parkingSpace?.toDomain(),
    images = multimedia?.images.orEmpty().map { AdImage(url = it.url, tag = it.tag) },
)

private fun FeaturesDto?.toDomain(): AdFeatures = AdFeatures(
    hasAirConditioning = this?.hasAirConditioning ?: false,
    hasBoxRoom = this?.hasBoxRoom ?: false,
    hasSwimmingPool = this?.hasSwimmingPool ?: false,
    hasTerrace = this?.hasTerrace ?: false,
    hasGarden = this?.hasGarden ?: false,
)

private fun ParkingSpaceDto.toDomain(): ParkingSpace = ParkingSpace(
    hasParkingSpace = hasParkingSpace ?: false,
    includedInPrice = isParkingSpaceIncludedInPrice ?: false,
)

/**
 * Merges the detail response with the ad the user actually opened.
 *
 * The mock detail endpoint returns ad 1 for every request, so trusting its `adid`, `priceInfo` or
 * `ubication` would show ad 1's identity on ads 2-4. Identity therefore comes from [listAd] and only
 * the rich fields — characteristics, energy certificate, long comment — come from the response.
 *
 * **Photos count as identity.** Each list ad carries its own gallery, while the detail payload's
 * images are ad 1's rooms; taking them from the response put the wrong flat's photos on three of the
 * four ads. The response's gallery is used only when the cached ad has no images of its own.
 * See docs/DECISIONS/ADR-0005-detail-merge-strategy.md.
 *
 * Do not "fix" this by using the response's own id. A regression test guards it.
 */
internal fun AdDetailDto.toDomain(listAd: Ad): AdDetail = AdDetail(
    ad = listAd,
    comment = propertyComment,
    characteristics = AdCharacteristics(
        communityCosts = moreCharacteristics?.communityCosts,
        roomNumber = moreCharacteristics?.roomNumber,
        bathNumber = moreCharacteristics?.bathNumber,
        exterior = moreCharacteristics?.exterior,
        housingFurnitures = moreCharacteristics?.housingFurnitures,
        energyCertificationType = moreCharacteristics?.energyCertificationType,
        flatLocation = moreCharacteristics?.flatLocation,
        modificationDate = moreCharacteristics?.modificationDate?.let(Instant::ofEpochMilli),
        constructedAreaSquareMeters = moreCharacteristics?.constructedArea,
        hasLift = moreCharacteristics?.lift,
        hasBoxroom = moreCharacteristics?.boxroom,
        isDuplex = moreCharacteristics?.isDuplex,
        floor = moreCharacteristics?.floor,
        status = moreCharacteristics?.status,
    ),
    energyCertificate = energyCertification?.let {
        EnergyCertificate(
            title = it.title,
            consumptionType = it.energyConsumption?.type,
            emissionsType = it.emissions?.type,
        )
    },
    gallery = listAd.images.ifEmpty {
        multimedia?.images.orEmpty().map {
            AdImage(url = it.url, tag = it.tag, localizedName = it.localizedName)
        }
    },
)
