package dev.mkaminski.idealista.feature.map

import app.cash.turbine.test
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdDetail
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.Operation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapViewModelTest {

    private val repository = FakeAdRepository()

    private class FakeAdRepository : AdRepository {
        val ads = MutableStateFlow<List<Ad>>(emptyList())
        var refreshes = 0

        override fun observeAds(): Flow<List<Ad>> = ads
        override suspend fun refreshAds(): Result<Unit> {
            refreshes++
            return Result.success(Unit)
        }
        override fun observeAdDetail(propertyCode: String): Flow<AdDetail> = flow { }
        override suspend fun toggleFavorite(propertyCode: String) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `every located ad becomes a pin`() = runTest {
        repository.ads.value = listOf(ad("1", 40.43, -3.68), ad("2", 40.41, -3.71))

        MapViewModel(repository).uiState.test {
            val state = expectMostRecentItem() as MapUiState.Content
            assertEquals(2, state.ads.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A pin at (0, 0) would drop the map in the Atlantic, so an unplaceable ad is left out. */
    @Test
    fun `an ad without coordinates is not placed`() = runTest {
        repository.ads.value = listOf(ad("1", 40.43, -3.68), ad("2", null, null))

        MapViewModel(repository).uiState.test {
            val state = expectMostRecentItem() as MapUiState.Content
            assertEquals(listOf("1"), state.ads.map(Ad::propertyCode))
            assertTrue(state.bounds.centerLatitude > 40.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing placeable shows the empty state`() = runTest {
        repository.ads.value = listOf(ad("1", null, null))

        MapViewModel(repository).uiState.test {
            assertEquals(MapUiState.Empty, expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Opening the map first on a cold start has to fill it, not wait for the list to be visited. */
    @Test
    fun `the map refreshes the cache itself`() = runTest {
        MapViewModel(repository)

        assertEquals(1, repository.refreshes)
    }

    /** The map reads the same cache as the list, so a favorite is already true when it arrives. */
    @Test
    fun `the map follows the cache as it changes`() = runTest {
        val viewModel = MapViewModel(repository)

        viewModel.uiState.test {
            assertEquals(MapUiState.Empty, expectMostRecentItem())

            repository.ads.value = listOf(ad("1", 40.43, -3.68))

            val state = expectMostRecentItem() as MapUiState.Content
            assertEquals(1, state.ads.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun ad(code: String, lat: Double?, lon: Double?) = Ad(
        propertyCode = code,
        thumbnailUrl = null,
        price = 1000.0,
        currencySuffix = "€",
        operation = Operation.SALE,
        propertyType = "flat",
        sizeSquareMeters = 100.0,
        rooms = 3,
        bathrooms = 2,
        exterior = true,
        floor = "2",
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
}
