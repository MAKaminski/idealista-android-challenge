package dev.mkaminski.idealista.data.local

import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.AdImage
import dev.mkaminski.idealista.model.Operation
import dev.mkaminski.idealista.model.ParkingSpace
import java.time.Instant

internal fun Ad.toEntity(images: String): AdEntity = AdEntity(
    propertyCode = propertyCode,
    thumbnailUrl = thumbnailUrl,
    price = price,
    currencySuffix = currencySuffix,
    operation = operation.name,
    propertyType = propertyType,
    size = sizeSquareMeters,
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
    hasAirConditioning = features.hasAirConditioning,
    hasBoxRoom = features.hasBoxRoom,
    hasSwimmingPool = features.hasSwimmingPool,
    hasTerrace = features.hasTerrace,
    hasGarden = features.hasGarden,
    hasParking = parking?.hasParkingSpace,
    parkingIncludedInPrice = parking?.includedInPrice,
    images = images,
)

/**
 * [favoritedAt] is passed in from the favorites table rather than stored on the ad row, so a cache
 * refresh can never overwrite a user's favorite.
 */
internal fun AdEntity.toDomain(images: List<AdImage>, favoritedAt: Instant?): Ad = Ad(
    propertyCode = propertyCode,
    thumbnailUrl = thumbnailUrl,
    price = price,
    currencySuffix = currencySuffix,
    operation = runCatching { Operation.valueOf(operation) }.getOrDefault(Operation.UNKNOWN),
    propertyType = propertyType,
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
    features = AdFeatures(
        hasAirConditioning = hasAirConditioning,
        hasBoxRoom = hasBoxRoom,
        hasSwimmingPool = hasSwimmingPool,
        hasTerrace = hasTerrace,
        hasGarden = hasGarden,
    ),
    parking = hasParking?.let {
        ParkingSpace(hasParkingSpace = it, includedInPrice = parkingIncludedInPrice ?: false)
    },
    images = images,
    favoritedAt = favoritedAt,
)
