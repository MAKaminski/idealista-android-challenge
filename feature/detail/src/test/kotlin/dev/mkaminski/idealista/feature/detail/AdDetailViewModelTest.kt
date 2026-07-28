package dev.mkaminski.idealista.feature.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdCharacteristics
import dev.mkaminski.idealista.model.AdDetail
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.Operation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class AdDetailViewModelTest {

    private val repository = FakeAdRepository()

    private class FakeAdRepository : AdRepository {
        val ads = MutableStateFlow(listOf(ad("1", 1_195_000.0, "calle de Lagasca"), ad("3", 1_100_000.0, "calle de Serrano")))
        var failDetail = false
        val toggled = mutableListOf<String>()

        override fun observeAds(): Flow<List<Ad>> = ads

        override suspend fun refreshAds(): Result<Unit> = Result.success(Unit)

        /** Mirrors the real repository: identity from the cached ad, rich fields from the payload. */
        override fun observeAdDetail(propertyCode: String): Flow<AdDetail> =
            if (failDetail) {
                flow { throw IOException("network down") }
            } else {
                ads.map { list ->
                    val cached = list.first { it.propertyCode == propertyCode }
                    AdDetail(
                        ad = cached,
                        comment = "long comment from ad 1",
                        characteristics = CHARACTERISTICS,
                        energyCertificate = null,
                        gallery = emptyList(),
                    )
                }
            }

        override suspend fun toggleFavorite(propertyCode: String) {
            toggled += propertyCode
            ads.update { list ->
                list.map {
                    if (it.propertyCode == propertyCode) it.copy(favoritedAt = FAVORITED_AT) else it
                }
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(propertyCode: String) = AdDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(
            mapOf(AdDetailViewModel.ARG_PROPERTY_CODE to propertyCode),
        ),
    )

    /**
     * The screen-level half of the ADR-0005 guard: the detail payload always describes ad 1, so
     * opening ad 3 must still show ad 3's price and address.
     */
    @Test
    fun `opening ad 3 shows ad 3 identity not ad 1`() = runTest {
        viewModel("3").uiState.test {
            val state = expectMostRecentItem() as AdDetailUiState.Content

            assertEquals("3", state.detail.ad.propertyCode)
            assertEquals(1_100_000.0, state.detail.ad.price, 0.0)
            assertEquals("calle de Serrano", state.detail.ad.address)
            assertNotEquals(1_195_000.0, state.detail.ad.price, 0.0)
            assertNotEquals("calle de Lagasca", state.detail.ad.address)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failure surfaces the error state`() = runTest {
        repository.failDetail = true

        viewModel("1").uiState.test {
            assertTrue(expectMostRecentItem() is AdDetailUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-subscribes after a failure`() = runTest {
        repository.failDetail = true
        val viewModel = viewModel("1")

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is AdDetailUiState.Error)

            repository.failDetail = false
            viewModel.retry()

            assertTrue(expectMostRecentItem() is AdDetailUiState.Content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriting from the detail screen stamps the date on this screen`() = runTest {
        val viewModel = viewModel("3")

        viewModel.uiState.test {
            assertNull((expectMostRecentItem() as AdDetailUiState.Content).detail.ad.favoritedAt)

            viewModel.toggleFavorite()

            val state = expectMostRecentItem() as AdDetailUiState.Content
            assertEquals(FAVORITED_AT, state.detail.ad.favoritedAt)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("3"), repository.toggled)
    }

    @Test
    fun `the rich fields still come from the detail payload`() = runTest {
        viewModel("3").uiState.test {
            val state = expectMostRecentItem() as AdDetailUiState.Content

            assertEquals("long comment from ad 1", state.detail.comment)
            assertEquals(133, state.detail.characteristics.constructedAreaSquareMeters)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private val FAVORITED_AT: Instant = Instant.parse("2026-07-28T10:15:30Z")

private val CHARACTERISTICS = AdCharacteristics(
    communityCosts = 330.0,
    roomNumber = 3,
    bathNumber = 2,
    exterior = false,
    housingFurnitures = null,
    energyCertificationType = "e",
    flatLocation = "internal",
    modificationDate = null,
    constructedAreaSquareMeters = 133,
    hasLift = true,
    hasBoxroom = false,
    isDuplex = false,
    floor = "2",
    status = "renew",
)

private fun ad(code: String, price: Double, address: String) = Ad(
    propertyCode = code,
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
)
