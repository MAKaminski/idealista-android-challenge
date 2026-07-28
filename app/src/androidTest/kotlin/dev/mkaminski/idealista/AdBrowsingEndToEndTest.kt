package dev.mkaminski.idealista

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end journey: browse → open a detail → favorite → see it in the Compose favorites screen.
 *
 * **Authored but not executed in this repository's automated runs.** It needs a device or emulator,
 * and neither the development container nor the CI job has one (docs/TESTING.md). Run it with:
 *
 * ```
 * ./gradlew connectedDebugAndroidTest
 * ```
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AdBrowsingEndToEndTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = androidx.test.ext.junit.rules.ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun tappingAnAdOpensItsDetailAndFavoritingSurfacesItInFavorites() {
        onView(withId(dev.mkaminski.idealista.feature.list.R.id.adList))
            .check(matches(isDisplayed()))

        // Open the third ad — the one whose identity the mock detail endpoint would get wrong.
        onView(withId(dev.mkaminski.idealista.feature.list.R.id.adList))
            .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(2, click()))

        onView(withId(dev.mkaminski.idealista.feature.detail.R.id.favoriteFab))
            .check(matches(isDisplayed()))
            .perform(click())

        onView(withId(dev.mkaminski.idealista.feature.detail.R.id.favoritedOn))
            .check(matches(withText(containsString("Favorited on"))))

        onView(withId(dev.mkaminski.idealista.R.id.bottomNav)).perform(click())
    }
}
