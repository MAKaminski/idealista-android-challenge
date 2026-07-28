package dev.mkaminski.idealista.feature.list

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class AdListViewModelTest {

    private val repository = FakeAdRepository()

    private class FakeAdRepository : AdRepository {
        val ads = MutableStateFlow<List<Ad>>(emptyList())
        var refreshResult: Result<Unit> = Result.success(Unit)
        val toggled = mutableListOf<String>()

        override fun observeAds(): Flow<List<Ad>> = ads
        override suspend fun refreshAds(): Result<Unit> = refreshResult
        override fun observeAdDetail(propertyCode: String): Flow<AdDetail> = flow { }
        override suspend fun toggleFavorite(propertyCode: String) {
            toggled += propertyCode
            ads.update { list ->
                list.map {
                    if (it.propertyCode == propertyCode) {
                        it.copy(favoritedAt = it.favoritedAt?.let { null } ?: FAVORITED_AT)
                    } else {
                        it
                    }
                }
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `content is emitted once ads arrive`() = runTest {
        val viewModel = AdListViewModel(repository)

        repository.ads.value = listOf(ad("1"), ad("2"))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is AdListUiState.Content)
            assertEquals(2, (state as AdListUiState.Content).ads.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty cache with no error shows the empty state`() = runTest {
        val viewModel = AdListViewModel(repository)

        viewModel.uiState.test {
            assertEquals(AdListUiState.Empty, expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a refresh failure with an empty cache surfaces the error`() = runTest {
        repository.refreshResult = Result.failure(IOException("network down"))

        val viewModel = AdListViewModel(repository)

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is AdListUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A failed refresh must not hide ads the user can still read offline. */
    @Test
    fun `a refresh failure with a populated cache still shows the ads`() = runTest {
        repository.ads.value = listOf(ad("1"))
        repository.refreshResult = Result.failure(IOException("network down"))

        val viewModel = AdListViewModel(repository)

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is AdListUiState.Content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling a favorite reaches the repository and updates the state`() = runTest {
        repository.ads.value = listOf(ad("1"))
        val viewModel = AdListViewModel(repository)

        viewModel.toggleFavorite("1")

        assertEquals(listOf("1"), repository.toggled)
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AdListUiState.Content
            assertEquals(FAVORITED_AT, state.ads.single().favoritedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun ad(code: String) = Ad(
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
        latitude = null,
        longitude = null,
        description = "",
        features = AdFeatures(),
        parking = null,
        images = emptyList(),
    )

    private companion object {
        val FAVORITED_AT: Instant = Instant.parse("2026-07-28T10:15:30Z")
    }
}
