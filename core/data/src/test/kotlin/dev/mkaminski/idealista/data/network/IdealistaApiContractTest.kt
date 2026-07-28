package dev.mkaminski.idealista.data.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Parses the **real** committed payloads, served over a local server, through the real Retrofit
 * stack. If the upstream schema drifts, these fail here rather than in a user's hands.
 */
class IdealistaApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: IdealistaApi

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IdealistaApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing fixture $name — is the syncApiFixtures task wired up?"
        }.bufferedReader().use { it.readText() }

    private fun enqueue(name: String) {
        server.enqueue(MockResponse.Builder().code(200).body(fixture(name)).build())
    }

    @Test
    fun `list endpoint parses all four ads`() = runTest {
        enqueue("list.json")

        val ads = api.getAds()

        assertEquals(4, ads.size)
        assertEquals(listOf("1", "2", "3", "4"), ads.map { it.propertyCode })
    }

    @Test
    fun `list price comes from the doubly nested priceInfo`() = runTest {
        enqueue("list.json")

        val first = api.getAds().first()

        assertEquals(1_195_000.0, first.priceInfo?.price?.amount)
        assertEquals("€", first.priceInfo?.price?.currencySuffix)
    }

    @Test
    fun `optional feature keys absent on an ad parse as null rather than failing`() = runTest {
        enqueue("list.json")

        val ads = api.getAds()

        // Ad 1 has no pool/terrace/garden keys at all; ad 4 is the only one that carries them.
        assertNull(ads.first { it.propertyCode == "1" }.features?.hasSwimmingPool)
        assertEquals(false, ads.first { it.propertyCode == "4" }.features?.hasSwimmingPool)
        // parkingSpace appears on ad 2 only.
        assertNull(ads.first { it.propertyCode == "1" }.parkingSpace)
        assertNotNull(ads.first { it.propertyCode == "2" }.parkingSpace)
    }

    @Test
    fun `detail endpoint parses characteristics and energy certificate`() = runTest {
        enqueue("detail.json")

        val detail = api.getAdDetail()

        assertEquals(1, detail.adid)
        assertEquals(133, detail.moreCharacteristics?.constructedArea)
        assertEquals(true, detail.moreCharacteristics?.lift)
        assertEquals("e", detail.energyCertification?.energyConsumption?.type)
        assertEquals(10, detail.multimedia?.images?.size)
        assertEquals("Salón", detail.multimedia?.images?.first()?.localizedName)
        assertTrue(detail.propertyComment.isNotBlank())
    }

    @Test
    fun `detail price is one level shallower than the list price`() = runTest {
        enqueue("detail.json")

        val detail = api.getAdDetail()

        assertEquals(1_195_000.0, detail.priceInfo?.amount)
    }

    @Test
    fun `an unknown field added upstream does not break parsing`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""[{"propertyCode":"99","somethingNewUpstream":{"nested":true}}]""")
                .build(),
        )

        val ads = api.getAds()

        assertEquals("99", ads.single().propertyCode)
    }
}
