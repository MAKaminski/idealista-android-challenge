package dev.mkaminski.idealista.feature.favorites

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dev.mkaminski.idealista.designsystem.IdealistaTheme
import dev.mkaminski.idealista.testing.Screenshots
import dev.mkaminski.idealista.testing.ShowcaseAds
import dev.mkaminski.idealista.testing.TestAds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavoritesScreenshotTest {

    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        Screenshots.assumeEnabled()
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setTheme(dev.mkaminski.idealista.designsystem.R.style.Theme_Idealista)
        Screenshots.installPhotoLoader(activity)
    }

    @Test
    fun `favorites screen`() {
        val favorites = listOf(
            ShowcaseAds.THREE.copy(favoritedAt = TestAds.FAVORITED_AT),
            ShowcaseAds.ONE.copy(favoritedAt = TestAds.FAVORITED_AT),
        )
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                IdealistaTheme {
                    FavoritesScreen(
                        state = FavoritesUiState.Content(favorites),
                        onRemoveFavorite = {},
                    )
                }
            }
        }

        Screenshots.capture(activity, view, "04-favorites", height = 340)
    }
}
