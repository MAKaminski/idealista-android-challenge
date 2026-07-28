package dev.mkaminski.idealista.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import dev.mkaminski.idealista.data.local.Converters
import dev.mkaminski.idealista.data.local.IdealistaDatabase
import dev.mkaminski.idealista.data.network.IdealistaApi
import dev.mkaminski.idealista.data.network.dto.AdDetailDto
import dev.mkaminski.idealista.data.network.dto.AdListItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class DefaultAdRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** A fixed instant so the favorited date is asserted exactly rather than approximately. */
    private val favoritedInstant: Instant = Instant.parse("2026-07-28T10:15:30Z")

    private lateinit var database: IdealistaDatabase
    private lateinit var api: FakeIdealistaApi
    private lateinit var repository: DefaultAdRepository

    private class FakeIdealistaApi(
        private val ads: List<AdListItemDto>,
        private val detail: AdDetailDto,
    ) : IdealistaApi {
        var failNextList = false
        var listCallCount = 0

        override suspend fun getAds(): List<AdListItemDto> {
            listCallCount++
            if (failNextList) throw IOException("network down")
            return ads
        }

        override suspend fun getAdDetail(): AdDetailDto = detail
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")).bufferedReader()
            .use { it.readText() }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, IdealistaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeIdealistaApi(
            ads = json.decodeFromString(fixture("list.json")),
            detail = json.decodeFromString(fixture("detail.json")),
        )
        repository = DefaultAdRepository(
            api = api,
            adDao = database.adDao(),
            favoriteDao = database.favoriteDao(),
            converters = Converters(),
            clock = Clock.fixed(favoritedInstant, ZoneOffset.UTC),
            // Dispatchers.Unconfined rather than a TestDispatcher built here: one constructed
            // outside runTest carries its own scheduler, and mixing schedulers makes every
            // dispatch throw. The repository only needs *a* dispatcher to be injectable.
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `refresh puts all four ads in the cache`() = runTest {
        repository.refreshAds().getOrThrow()

        repository.observeAds().test {
            val ads = awaitItem()
            assertEquals(4, ads.size)
            assertEquals(listOf("1", "2", "3", "4"), ads.map { it.propertyCode }.sorted())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a network failure leaves the cached ads intact`() = runTest {
        repository.refreshAds().getOrThrow()

        api.failNextList = true
        val result = repository.refreshAds()

        assertTrue(result.isFailure)
        repository.observeAds().test {
            assertEquals(4, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The load-bearing test from docs/TESTING.md: favoriting is stored once and observed by both
     * screens, so the date on the list and the date on the detail can never disagree.
     */
    @Test
    fun `favoriting an ad surfaces the same date on the list and the detail`() = runTest {
        repository.refreshAds().getOrThrow()

        repository.toggleFavorite("2")

        var listFavoritedAt: Instant? = null
        var detailFavoritedAt: Instant? = null

        repository.observeAds().test {
            listFavoritedAt = awaitItem().first { it.propertyCode == "2" }.favoritedAt
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeAdDetail("2").test {
            detailFavoritedAt = awaitItem().ad.favoritedAt
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(favoritedInstant, listFavoritedAt)
        assertEquals(favoritedInstant, detailFavoritedAt)
        assertEquals(listFavoritedAt, detailFavoritedAt)
    }

    @Test
    fun `toggling twice clears the favorite and its date`() = runTest {
        repository.refreshAds().getOrThrow()

        repository.toggleFavorite("2")
        repository.toggleFavorite("2")

        repository.observeAds().test {
            val ad = awaitItem().first { it.propertyCode == "2" }
            assertFalse(ad.isFavorite)
            assertNull(ad.favoritedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriting one ad does not favorite the others`() = runTest {
        repository.refreshAds().getOrThrow()

        repository.toggleFavorite("3")

        repository.observeAds().test {
            val ads = awaitItem()
            assertEquals(listOf("3"), ads.filter { it.isFavorite }.map { it.propertyCode })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a refresh does not clear existing favorites`() = runTest {
        repository.refreshAds().getOrThrow()
        repository.toggleFavorite("1")

        repository.refreshAds().getOrThrow()

        repository.observeAds().test {
            val ad = awaitItem().first { it.propertyCode == "1" }
            assertTrue(ad.isFavorite)
            assertEquals(favoritedInstant, ad.favoritedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The second load-bearing test: the detail endpoint always returns ad 1, so the repository must
     * take identity from the cached ad. See ADR-0005.
     */
    @Test
    fun `detail for ad 3 shows ad 3 identity not ad 1`() = runTest {
        repository.refreshAds().getOrThrow()

        repository.observeAdDetail("3").test {
            val detail = awaitItem()

            assertEquals("3", detail.ad.propertyCode)
            assertEquals(1_100_000.0, detail.ad.price, 0.0)
            assertNotEquals(1_195_000.0, detail.ad.price, 0.0)
            assertNotEquals("calle de Lagasca", detail.ad.address)
            // ...while the rich content still comes from the detail payload.
            assertEquals(133, detail.characteristics.constructedAreaSquareMeters)
            // Photos, though, are ad 3's own — not the payload's ten pictures of ad 1.
            assertEquals(detail.ad.images.size, detail.gallery.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Regression: the detail payload's gallery is ad 1's rooms, so taking photos from the response
     * put the wrong flat's pictures on ads 2-4. Photos are identity — they come from the opened ad.
     */
    @Test
    fun `the detail gallery shows the opened ad's photos not ad 1's`() = runTest {
        repository.refreshAds().getOrThrow()

        val galleries = mutableMapOf<String, List<String>>()
        listOf("1", "2", "3", "4").forEach { code ->
            repository.observeAdDetail(code).test {
                val detail = awaitItem()
                galleries[code] = detail.gallery.map { it.url }
                // Every photo on this screen belongs to the ad the user opened.
                assertEquals(detail.ad.images.map { it.url }, detail.gallery.map { it.url })
                cancelAndIgnoreRemainingEvents()
            }
        }

        // ...and the four ads genuinely differ, so the assertion above can fail.
        val adOne = galleries.getValue("1")
        listOf("2", "3", "4").forEach { code ->
            assertNotEquals(adOne, galleries.getValue(code))
        }
    }

    @Test
    fun `every gallery photo url belongs to the cached ad`() = runTest {
        repository.refreshAds().getOrThrow()

        repository.observeAdDetail("3").test {
            val detail = awaitItem()
            val cachedUrls = detail.ad.images.map { it.url }.toSet()

            assertTrue(detail.gallery.isNotEmpty())
            detail.gallery.forEach { image -> assertTrue(image.url in cachedUrls) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriting from the detail screen updates the detail without refetching it`() = runTest {
        repository.refreshAds().getOrThrow()
        val callsBefore = api.listCallCount

        repository.observeAdDetail("4").test {
            assertNull(awaitItem().ad.favoritedAt)

            repository.toggleFavorite("4")

            assertEquals(favoritedInstant, awaitItem().ad.favoritedAt)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(callsBefore, api.listCallCount)
    }
}
