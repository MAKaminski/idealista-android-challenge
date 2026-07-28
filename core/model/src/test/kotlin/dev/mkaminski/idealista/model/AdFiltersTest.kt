package dev.mkaminski.idealista.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Filtering is pure, so it is tested on the JVM with no Android, no Room and no device. */
class AdFiltersTest {

    private val ads = listOf(
        ad("1", Operation.SALE, price = 1_195_000.0, rooms = 3, baths = 2, size = 133.0),
        ad("2", Operation.RENT, price = 2_750_000.0, rooms = 5, baths = 3, size = 241.0, parking = true),
        ad("3", Operation.SALE, price = 1_100_000.0, rooms = 4, baths = 3, size = 164.0, exterior = true),
        ad(
            "4",
            Operation.RENT,
            price = 889_000.0,
            rooms = 2,
            baths = 1,
            size = 94.0,
            amenities = setOf(Amenity.SWIMMING_POOL, Amenity.TERRACE),
        ),
    )

    @Test
    fun `no filters returns everything in the original order`() {
        assertEquals(ads.map { it.propertyCode }, ads.applyFilters(AdFilters()).map { it.propertyCode })
    }

    @Test
    fun `operation narrows to sale or rent`() {
        assertEquals(
            listOf("1", "3"),
            ads.applyFilters(AdFilters(operation = Operation.SALE)).map { it.propertyCode },
        )
        assertEquals(
            listOf("2", "4"),
            ads.applyFilters(AdFilters(operation = Operation.RENT)).map { it.propertyCode },
        )
    }

    @Test
    fun `minimum rooms is inclusive`() {
        assertEquals(
            listOf("1", "2", "3"),
            ads.applyFilters(AdFilters(minRooms = 3)).map { it.propertyCode },
        )
    }

    @Test
    fun `maximum price is inclusive`() {
        assertEquals(
            listOf("3", "4"),
            ads.applyFilters(AdFilters(maxPrice = 1_100_000.0)).map { it.propertyCode },
        )
    }

    @Test
    fun `minimum size filters on square meters`() {
        assertEquals(
            listOf("2", "3"),
            ads.applyFilters(AdFilters(minSizeSquareMeters = 150.0)).map { it.propertyCode },
        )
    }

    @Test
    fun `exterior only keeps exterior properties`() {
        assertEquals(listOf("3"), ads.applyFilters(AdFilters(exteriorOnly = true)).map { it.propertyCode })
    }

    @Test
    fun `parking filter uses the parking block not a feature flag`() {
        assertEquals(listOf("2"), ads.applyFilters(AdFilters(withParking = true)).map { it.propertyCode })
    }

    /** Two amenities mean both, the way a property search behaves. */
    @Test
    fun `amenities are combined with AND`() {
        val both = AdFilters(amenities = setOf(Amenity.SWIMMING_POOL, Amenity.TERRACE))
        assertEquals(listOf("4"), ads.applyFilters(both).map { it.propertyCode })

        val impossible = AdFilters(amenities = setOf(Amenity.SWIMMING_POOL, Amenity.GARDEN))
        assertTrue(ads.applyFilters(impossible).isEmpty())
    }

    @Test
    fun `criteria stack`() {
        val filters = AdFilters(operation = Operation.SALE, minRooms = 4, maxPrice = 1_200_000.0)

        assertEquals(listOf("3"), ads.applyFilters(filters).map { it.propertyCode })
    }

    @Test
    fun `favorites only keeps saved properties`() {
        val withFavorite = ads.map {
            if (it.propertyCode == "2") it.copy(favoritedAt = Instant.parse("2026-07-28T10:00:00Z")) else it
        }

        assertEquals(
            listOf("2"),
            withFavorite.applyFilters(AdFilters(favoritesOnly = true)).map { it.propertyCode },
        )
    }

    @Test
    fun `sorting by price runs both ways`() {
        assertEquals(
            listOf("4", "3", "1", "2"),
            ads.applyFilters(AdFilters(sort = AdSort.PRICE_LOW_TO_HIGH)).map { it.propertyCode },
        )
        assertEquals(
            listOf("2", "1", "3", "4"),
            ads.applyFilters(AdFilters(sort = AdSort.PRICE_HIGH_TO_LOW)).map { it.propertyCode },
        )
    }

    @Test
    fun `sorting by size and rooms uses the right field`() {
        assertEquals("2", ads.applyFilters(AdFilters(sort = AdSort.SIZE_LARGEST)).first().propertyCode)
        assertEquals("2", ads.applyFilters(AdFilters(sort = AdSort.ROOMS_MOST)).first().propertyCode)
    }

    @Test
    fun `sorting and filtering compose`() {
        val filters = AdFilters(operation = Operation.SALE, sort = AdSort.PRICE_LOW_TO_HIGH)

        assertEquals(listOf("3", "1"), ads.applyFilters(filters).map { it.propertyCode })
    }

    @Test
    fun `active count reflects how many criteria are set`() {
        assertFalse(AdFilters().isActive)
        assertEquals(0, AdFilters().activeCount)

        val filters = AdFilters(
            operation = Operation.SALE,
            minRooms = 3,
            amenities = setOf(Amenity.TERRACE, Amenity.GARDEN),
        )

        assertTrue(filters.isActive)
        assertEquals(4, filters.activeCount)
    }

    private fun ad(
        code: String,
        operation: Operation,
        price: Double,
        rooms: Int,
        baths: Int,
        size: Double,
        exterior: Boolean = false,
        parking: Boolean = false,
        amenities: Set<Amenity> = emptySet(),
    ) = Ad(
        propertyCode = code,
        thumbnailUrl = null,
        price = price,
        currencySuffix = "€",
        operation = operation,
        propertyType = "flat",
        sizeSquareMeters = size,
        rooms = rooms,
        bathrooms = baths,
        exterior = exterior,
        floor = "2",
        address = "calle $code",
        neighborhood = null,
        district = "Barrio de Salamanca",
        municipality = "Madrid",
        province = "Madrid",
        latitude = 40.4362687,
        longitude = -3.6833686,
        description = "",
        features = AdFeatures(
            hasAirConditioning = Amenity.AIR_CONDITIONING in amenities,
            hasBoxRoom = Amenity.BOX_ROOM in amenities,
            hasSwimmingPool = Amenity.SWIMMING_POOL in amenities,
            hasTerrace = Amenity.TERRACE in amenities,
            hasGarden = Amenity.GARDEN in amenities,
        ),
        parking = if (parking) ParkingSpace(hasParkingSpace = true, includedInPrice = true) else null,
        images = emptyList(),
    )
}
