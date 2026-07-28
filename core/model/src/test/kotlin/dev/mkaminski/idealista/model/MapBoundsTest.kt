package dev.mkaminski.idealista.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBoundsTest {

    private fun ad(code: String, lat: Double?, lon: Double?) = Ad(
        propertyCode = code,
        thumbnailUrl = null,
        price = 1.0,
        currencySuffix = "€",
        operation = Operation.SALE,
        propertyType = "flat",
        sizeSquareMeters = 1.0,
        rooms = 1,
        bathrooms = 1,
        exterior = true,
        floor = null,
        address = "calle $code",
        neighborhood = null,
        district = null,
        municipality = null,
        province = null,
        latitude = lat,
        longitude = lon,
        description = "",
        features = AdFeatures(),
        parking = null,
        images = emptyList(),
    )

    @Test
    fun `bounds contain every located ad`() {
        val ads = listOf(ad("1", 40.43, -3.68), ad("2", 40.41, -3.71))

        val bounds = MapBounds.around(ads)

        assertTrue(bounds.south < 40.41)
        assertTrue(bounds.north > 40.43)
        assertTrue(bounds.west < -3.71)
        assertTrue(bounds.east > -3.68)
    }

    @Test
    fun `the centre sits between the extremes`() {
        val bounds = MapBounds.around(listOf(ad("1", 40.40, -3.70), ad("2", 40.50, -3.60)))

        assertEquals(40.45, bounds.centerLatitude, 0.0001)
        assertEquals(-3.65, bounds.centerLongitude, 0.0001)
    }

    /** An ad with no coordinates must be skipped, not read as (0, 0) off the coast of Africa. */
    @Test
    fun `ads without coordinates do not drag the bounds to null island`() {
        val bounds = MapBounds.around(listOf(ad("1", 40.43, -3.68), ad("2", null, null)))

        assertTrue("latitude drifted to $bounds", bounds.south > 40.0)
        assertTrue("longitude drifted to $bounds", bounds.east < 0.0)
    }

    @Test
    fun `an empty cache opens on Madrid rather than nowhere`() {
        assertEquals(MapBounds.MADRID, MapBounds.around(emptyList()))
        assertEquals(MapBounds.MADRID, MapBounds.around(listOf(ad("1", null, null))))
    }

    @Test
    fun `a single ad still yields usable bounds`() {
        val bounds = MapBounds.around(listOf(ad("1", 40.43, -3.68)))

        assertTrue(bounds.north > bounds.south)
        assertTrue(bounds.east > bounds.west)
        assertEquals(40.43, bounds.centerLatitude, 0.0001)
    }

    @Test
    fun `only ads that can be placed are offered to the map`() {
        val ads = listOf(ad("1", 40.43, -3.68), ad("2", null, -3.70), ad("3", 40.41, null))

        assertEquals(listOf("1"), ads.withCoordinates().map(Ad::propertyCode))
    }
}
