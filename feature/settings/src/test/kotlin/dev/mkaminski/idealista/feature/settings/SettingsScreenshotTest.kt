package dev.mkaminski.idealista.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dev.mkaminski.idealista.designsystem.IdealistaTheme
import dev.mkaminski.idealista.model.AppLanguage
import dev.mkaminski.idealista.testing.Screenshots
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {

    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        Screenshots.assumeEnabled()
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setTheme(dev.mkaminski.idealista.designsystem.R.style.Theme_Idealista)
    }

    @Test
    fun `settings screen`() {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                IdealistaTheme {
                    SettingsScreen(selected = AppLanguage.SPANISH, onLanguageSelected = {})
                }
            }
        }

        Screenshots.capture(activity, view, "05-settings", height = 700)
    }
}
