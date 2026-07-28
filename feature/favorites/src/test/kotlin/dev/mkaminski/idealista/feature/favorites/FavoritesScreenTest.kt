package dev.mkaminski.idealista.feature.favorites

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdFeatures
import dev.mkaminski.idealista.model.Operation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** Compose UI tests run on the JVM under Robolectric — no device needed (docs/TESTING.md). */
@RunWith(RobolectricTestRunner::class)
class FavoritesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val favoritedAt: Instant = Instant.parse("2026-07-28T10:15:30Z")

    @Test
    fun `the empty state is shown when nothing is favorited`() {
        composeRule.setContent {
            FavoritesScreen(state = FavoritesUiState.Empty, onRemoveFavorite = {})
        }

        composeRule.onNodeWithTag(TAG_EMPTY).assertIsDisplayed()
    }

    /** The challenge's requirement, asserted in the Compose screen too. */
    @Test
    fun `a favorited ad shows the date it was favorited`() {
        composeRule.setContent {
            FavoritesScreen(
                state = FavoritesUiState.Content(listOf(ad("1", favoritedAt))),
                onRemoveFavorite = {},
            )
        }

        composeRule.onNodeWithText("Favorited on", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Jul 28, 2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the price and address are rendered`() {
        composeRule.setContent {
            FavoritesScreen(
                state = FavoritesUiState.Content(listOf(ad("1", favoritedAt))),
                onRemoveFavorite = {},
            )
        }

        composeRule.onNodeWithText("calle de Lagasca").assertIsDisplayed()
        composeRule.onNodeWithText("€", substring = true).assertIsDisplayed()
    }

    @Test
    fun `tapping remove reports the property code`() {
        val removed = mutableListOf<String>()
        composeRule.setContent {
            FavoritesScreen(
                state = FavoritesUiState.Content(listOf(ad("3", favoritedAt))),
                onRemoveFavorite = { removed += it },
            )
        }

        // The remove control is an icon: it carries a contentDescription, not text.
        composeRule.onNodeWithContentDescription("Remove", substring = true).performClick()

        assertEquals(listOf("3"), removed)
    }

    private fun ad(code: String, favoritedAt: Instant) = Ad(
        propertyCode = code,
        thumbnailUrl = null,
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
        latitude = null,
        longitude = null,
        description = "",
        features = AdFeatures(),
        parking = null,
        images = emptyList(),
        favoritedAt = favoritedAt,
    )
}
