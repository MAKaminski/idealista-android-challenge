package dev.mkaminski.idealista.data.network

import dev.mkaminski.idealista.data.network.dto.AdDetailDto
import dev.mkaminski.idealista.data.network.dto.AdListItemDto
import dev.mkaminski.idealista.model.Operation
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AdMappersTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun listAds(): List<AdListItemDto> =
        json.decodeFromString(fixture("list.json"))

    private fun detailDto(): AdDetailDto =
        json.decodeFromString(fixture("detail.json"))

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing fixture $name"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `list dto maps identity price and operation`() {
        val ad = listAds().first { it.propertyCode == "1" }.toDomain()

        assertEquals("1", ad.propertyCode)
        assertEquals(1_195_000.0, ad.price, 0.0)
        assertEquals("€", ad.currencySuffix)
        assertEquals(Operation.SALE, ad.operation)
        assertEquals("calle de Lagasca", ad.address)
        assertEquals("Barrio de Salamanca", ad.district)
        assertEquals(3, ad.rooms)
        assertEquals(133.0, ad.sizeSquareMeters, 0.0)
    }

    @Test
    fun `rent operation maps distinctly from sale`() {
        assertEquals(Operation.RENT, listAds().first { it.propertyCode == "2" }.toDomain().operation)
    }

    @Test
    fun `an unrecognised operation degrades to UNKNOWN rather than throwing`() {
        assertEquals(Operation.UNKNOWN, Operation.fromApi("timeshare"))
        assertEquals(Operation.UNKNOWN, Operation.fromApi(null))
    }

    @Test
    fun `ad without optional feature keys maps to false rather than crashing`() {
        val ad = listAds().first { it.propertyCode == "1" }.toDomain()

        assertTrue(ad.features.hasAirConditioning)
        assertFalse(ad.features.hasSwimmingPool)
        assertFalse(ad.features.hasTerrace)
        assertFalse(ad.features.hasGarden)
        assertNull(ad.parking)
    }

    @Test
    fun `ad carrying the full feature set maps every flag`() {
        val ad = listAds().first { it.propertyCode == "4" }.toDomain()

        assertTrue(ad.features.hasAirConditioning)
        assertFalse(ad.features.hasSwimmingPool)
        assertFalse(ad.features.hasGarden)
    }

    @Test
    fun `parking space maps when present`() {
        val parking = listAds().first { it.propertyCode == "2" }.toDomain().parking

        assertEquals(true, parking?.hasParkingSpace)
        assertEquals(true, parking?.includedInPrice)
    }

    @Test
    fun `a newly mapped ad is not favorited`() {
        val ad = listAds().first().toDomain()

        assertNull(ad.favoritedAt)
        assertFalse(ad.isFavorite)
    }

    @Test
    fun `detail maps characteristics energy certificate and gallery`() {
        val listAd = listAds().first { it.propertyCode == "1" }.toDomain()

        val detail = detailDto().toDomain(listAd)

        assertEquals(133, detail.characteristics.constructedAreaSquareMeters)
        assertEquals(true, detail.characteristics.hasLift)
        assertEquals(330.0, detail.characteristics.communityCosts!!, 0.0)
        assertEquals(Instant.ofEpochMilli(1_727_683_968_000), detail.characteristics.modificationDate)
        assertEquals("e", detail.energyCertificate?.consumptionType)
        assertEquals(10, detail.gallery.size)
        assertEquals("Salón", detail.gallery.first().localizedName)
        assertTrue(detail.comment.isNotBlank())
    }

    /**
     * The load-bearing guard from docs/TESTING.md. The mock detail endpoint always returns ad 1, so
     * opening ad 3 must still render ad 3's identity — see ADR-0005.
     */
    @Test
    fun `detail for ad 3 never shows ad 1 identity`() {
        val adOne = listAds().first { it.propertyCode == "1" }.toDomain()
        val adThree = listAds().first { it.propertyCode == "3" }.toDomain()

        val detail = detailDto().toDomain(listAd = adThree)

        assertEquals("3", detail.ad.propertyCode)
        assertEquals(adThree.price, detail.ad.price, 0.0)
        assertEquals(adThree.address, detail.ad.address)
        assertEquals(adThree.operation, detail.ad.operation)
        // ...and specifically not ad 1's, which is what the raw response carries.
        assertNotEquals(adOne.price, detail.ad.price)
        assertNotEquals(adOne.address, detail.ad.address)
        assertNotEquals(1, detail.ad.propertyCode.toInt())
    }
}
