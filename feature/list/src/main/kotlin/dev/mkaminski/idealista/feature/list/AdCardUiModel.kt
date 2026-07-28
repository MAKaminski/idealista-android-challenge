package dev.mkaminski.idealista.feature.list

import dev.mkaminski.idealista.designsystem.Formatters
import dev.mkaminski.idealista.model.Ad
import java.time.Instant

/**
 * Everything a list card renders, derived from one [Ad].
 *
 * This exists so the binding is testable without a device or an image loader: an image showing the
 * wrong property is a data-alignment bug, and it should be caught by a plain JVM assertion rather
 * than by a human noticing that the photos look off. [propertyCode] travels with the model so a
 * click can be checked against the same source as the picture.
 */
internal data class AdCardUiModel(
    val propertyCode: String,
    val imageUrl: String?,
    val price: String,
    val address: String,
    val roomCount: Int,
    val bathCount: Int,
    val area: String,
    val favoritedAt: Instant?,
    val isFavorite: Boolean,
)

internal fun Ad.toCardUiModel(): AdCardUiModel = AdCardUiModel(
    propertyCode = propertyCode,
    // The list payload gives every ad its own thumbnail; never substitute another ad's image.
    imageUrl = thumbnailUrl,
    price = Formatters.price(price, currencySuffix),
    address = listOfNotNull(address, district).joinToString(", "),
    roomCount = rooms,
    bathCount = bathrooms,
    area = Formatters.area(sizeSquareMeters),
    favoritedAt = favoritedAt,
    isFavorite = isFavorite,
)
