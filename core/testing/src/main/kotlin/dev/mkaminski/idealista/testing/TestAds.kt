package dev.mkaminski.idealista.testing

import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.Operation
import java.time.Instant

/** Shared fixtures so each test module stops hand-rolling the same 20-field Ad. */
object TestAds {

    val FAVORITED_AT: Instant = Instant.parse("2026-07-28T10:15:30Z")

    fun ad(
        propertyCode: String = "1",
        price: Double = 1_195_000.0,
        address: String = "calle de Lagasca",
        favoritedAt: Instant? = null,
    ) = Ad(
        propertyCode = propertyCode,
        thumbnailUrl = null,
        price = price,
        currencySuffix = "€",
        operation = Operation.SALE,
        propertyType = "flat",
        sizeSquareMeters = 133.0,
        rooms = 3,
        bathrooms = 2,
        exterior = false,
        floor = "2",
        address = address,
        neighborhood = null,
        district = "Barrio de Salamanca",
        municipality = "Madrid",
        province = "Madrid",
        latitude = null,
        longitude = null,
        description = "",
        features = AdFeatures(),
        parking = null,
        images = emptyList(),
        favoritedAt = favoritedAt,
    )
}
