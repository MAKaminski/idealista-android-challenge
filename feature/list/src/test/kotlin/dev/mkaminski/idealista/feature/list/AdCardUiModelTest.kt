package dev.mkaminski.idealista.feature.list

import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.AdImage
import dev.mkaminski.idealista.model.Operation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Image-to-ad alignment, asserted against the **real** `list.json`.
 *
 * A card showing another property's photo is a data bug, not a rendering detail, so it is caught
 * here on the JVM rather than by someone noticing the pictures look wrong.
 */
class AdCardUiModelTest {

    private val fixtureAds: List<Pair<String, String>> = // propertyCode to thumbnail url
        Json.parseToJsonElement(
            checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/list.json"))
                .bufferedReader().use { it.readText() },
        ).jsonArray.map { element ->
            val obj = element.jsonObject
            obj["propertyCode"]!!.jsonPrimitive.content to obj["thumbnail"]!!.jsonPrimitive.content
        }

    @Test
    fun `every card uses its own ad's thumbnail`() {
        assertEquals(4, fixtureAds.size)

        fixtureAds.forEach { (propertyCode, thumbnail) ->
            val model = ad(propertyCode, thumbnail).toCardUiModel()

            assertEquals(propertyCode, model.propertyCode)
            assertEquals(thumbnail, model.imageUrl)
        }
    }

    /** The four fixtures genuinely have four different photos, so the test above can fail. */
    @Test
    fun `the fixture ads have distinct thumbnails`() {
        val urls = fixtureAds.map { it.second }

        assertEquals(urls.size, urls.toSet().size)
    }

    @Test
    fun `no two cards share an image url`() {
        val models = fixtureAds.map { (code, thumb) -> ad(code, thumb).toCardUiModel() }

        models.forEach { model ->
            val others = models.filter { it.propertyCode != model.propertyCode }
            others.forEach { other -> assertNotEquals(model.imageUrl, other.imageUrl) }
        }
    }

    @Test
    fun `an ad without a thumbnail yields a null url rather than borrowing one`() {
        val model = ad("9", thumbnail = null).toCardUiModel()

        assertNull(model.imageUrl)
    }

    @Test
    fun `the model carries the property code a click will report`() {
        val model = ad("3", "https://example.test/3.webp").toCardUiModel()

        // The click callback and the image are derived from the same model, so they cannot disagree.
        assertEquals("3", model.propertyCode)
    }

    @Test
    fun `favorite state and date are carried through`() {
        val favoritedAt = Instant.parse("2026-07-28T10:15:30Z")

        val model = ad("1", "https://example.test/1.webp", favoritedAt).toCardUiModel()

        assertTrue(model.isFavorite)
        assertEquals(favoritedAt, model.favoritedAt)
    }

    private fun ad(
        propertyCode: String,
        thumbnail: String?,
        favoritedAt: Instant? = null,
    ) = Ad(
        propertyCode = propertyCode,
        thumbnailUrl = thumbnail,
        price = 1_000.0,
        currencySuffix = "€",
        operation = Operation.SALE,
        propertyType = "flat",
        sizeSquareMeters = 100.0,
        rooms = 3,
        bathrooms = 2,
        exterior = true,
        floor = "2",
        address = "calle $propertyCode",
        neighborhood = null,
        district = "Barrio de Salamanca",
        municipality = "Madrid",
        province = "Madrid",
        latitude = null,
        longitude = null,
        description = "",
        features = AdFeatures(),
        parking = null,
        images = listOfNotNull(thumbnail?.let { AdImage(it, "livingRoom") }),
        favoritedAt = favoritedAt,
    )
}
