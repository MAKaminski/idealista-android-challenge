package dev.mkaminski.idealista.testing

import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdCharacteristics
import dev.mkaminski.idealista.model.AdDetail
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.AdImage
import dev.mkaminski.idealista.model.EnergyCertificate
import dev.mkaminski.idealista.model.Operation
import dev.mkaminski.idealista.model.ParkingSpace
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

/**
 * The four real ads from `list.json`, transcribed as domain models.
 *
 * Used to render the README screenshots, so what a reviewer sees is the production payload — four
 * distinct Madrid flats with four distinct photos — rather than "Lorem ipsum, 3 rooms" four times.
 */
object ShowcaseAds {

    val ONE = Ad(
        propertyCode = "1",
        thumbnailUrl = IMG + "e1/0e/5e/1459427188.webp",
        price = 1_195_000.0,
        currencySuffix = "€",
        operation = Operation.SALE,
        propertyType = "flat",
        sizeSquareMeters = 133.0,
        rooms = 3,
        bathrooms = 2,
        exterior = false,
        floor = "2",
        address = "calle de Lagasca",
        neighborhood = null,
        district = "Barrio de Salamanca",
        municipality = "Madrid",
        province = "Madrid",
        latitude = 40.4362687,
        longitude = -3.6833686,
        description = "",
        features = AdFeatures(hasAirConditioning = true),
        parking = null,
        images = listOf(
            AdImage(IMG + "e1/0e/5e/1459427188.webp", "livingRoom", "Salón"),
            AdImage(IMG + "0c/1c/0c/1368894539.webp", "livingRoom", "Salón"),
            AdImage(IMG + "e1/0e/5e/1392239936.webp", "kitchen", "Cocina"),
        ),
    )

    val TWO = ONE.copy(
        propertyCode = "2",
        thumbnailUrl = IMG + "26/c1/f6/1458542592.webp",
        price = 1_200.0,
        currencySuffix = "€/mes",
        operation = Operation.RENT,
        sizeSquareMeters = 241.0,
        rooms = 4,
        bathrooms = 4,
        exterior = true,
        floor = "6",
        address = "calle de Fortuny",
        district = "Chamberí",
        latitude = 40.432172,
        longitude = -3.6902537,
        features = AdFeatures(hasAirConditioning = true, hasBoxRoom = true),
        parking = ParkingSpace(hasParkingSpace = true, includedInPrice = true),
        images = listOf(AdImage(IMG + "26/c1/f6/1458542592.webp", "livingRoom", "Salón")),
    )

    val THREE = ONE.copy(
        propertyCode = "3",
        thumbnailUrl = IMG + "d8/7c/b2/1392008194.webp",
        price = 1_100_000.0,
        sizeSquareMeters = 164.0,
        exterior = true,
        floor = "4",
        address = "calle de Bailén",
        district = "Centro",
        latitude = 40.4107968,
        longitude = -3.7147657,
        features = AdFeatures(hasAirConditioning = true, hasBoxRoom = true),
        images = listOf(AdImage(IMG + "d8/7c/b2/1392008194.webp", "livingRoom", "Salón")),
    )

    val FOUR = ONE.copy(
        propertyCode = "4",
        thumbnailUrl = IMG + "23/3b/b7/1451541217.webp",
        price = 1_000.0,
        currencySuffix = "€/mes",
        operation = Operation.RENT,
        sizeSquareMeters = 94.0,
        exterior = false,
        floor = "4",
        address = "calle de la Povedilla",
        latitude = 40.4226243,
        longitude = -3.6719939,
        features = AdFeatures(hasAirConditioning = true),
        images = listOf(AdImage(IMG + "23/3b/b7/1451541217.webp", "livingRoom", "Salón")),
    )

    /** Ad 3 is favorited, so the screenshots show the date badge the brief asks for. */
    val ALL: List<Ad> = listOf(
        ONE,
        TWO,
        THREE.copy(favoritedAt = TestAds.FAVORITED_AT),
        FOUR,
    )

    /**
     * Ad 3's detail. Identity is ad 3's and the gallery is ad 3's photos, while the characteristics
     * and comment come from the response — which always describes ad 1 (ADR-0005). A screenshot of
     * this screen is therefore also a screenshot of the merge working.
     */
    val DETAIL_THREE = AdDetail(
        ad = THREE.copy(favoritedAt = TestAds.FAVORITED_AT),
        comment = "Venta. Piso EN EXCLUSIVA. Castellana. Se ofrece en venta vivienda de 133 m² en " +
            "el exclusivo Barrio de Salamanca, zona Castellana, con 3 dormitorios (uno en suite), " +
            "2 baños, amplio salón comedor y cocina office totalmente equipada.",
        characteristics = AdCharacteristics(
            communityCosts = 330.0,
            roomNumber = 3,
            bathNumber = 2,
            exterior = false,
            housingFurnitures = "unknown",
            energyCertificationType = "e",
            flatLocation = "internal",
            modificationDate = null,
            constructedAreaSquareMeters = 133,
            hasLift = true,
            hasBoxroom = false,
            isDuplex = false,
            floor = "2",
            status = "renew",
        ),
        energyCertificate = EnergyCertificate(
            title = "Certificado energético",
            consumptionType = "e",
            emissionsType = "e",
        ),
        gallery = THREE.images,
    )

    /** Every photo URL the screenshots need, so the loader can be primed in one call. */
    val PHOTO_URLS: List<String> =
        ALL.flatMap { it.images.map(AdImage::url) + listOfNotNull(it.thumbnailUrl) } +
            DETAIL_THREE.gallery.map(AdImage::url)
}

private const val IMG = "https://img4.idealista.com/blur/591_420_mq/0/id.pro.es.image.master/"
