package dev.mkaminski.idealista.feature.detail

import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dev.mkaminski.idealista.feature.detail.databinding.FragmentAdDetailBinding
import dev.mkaminski.idealista.testing.Screenshots
import dev.mkaminski.idealista.testing.ShowcaseAds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Renders the real detail screen — the extracted [render] the Fragment itself calls, so this cannot
 * drift into a screenshot of a screen the app does not have.
 *
 * Ad 3 on purpose: its identity is ad 3's while its characteristics come from a response that
 * always describes ad 1, so the screenshot doubles as a picture of ADR-0005 working.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetailScreenshotTest {

    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        Screenshots.assumeEnabled()
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setTheme(dev.mkaminski.idealista.designsystem.R.style.Theme_Idealista)
        Screenshots.installPhotoLoader(activity)
    }

    @Test
    fun `detail screen`() {
        val binding = FragmentAdDetailBinding.inflate(LayoutInflater.from(activity))
        val gallery = GalleryAdapter(onImageClick = {})
        binding.gallery.adapter = gallery
        binding.gallery.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        binding.toolbar.inflateMenu(R.menu.detail)
        TabLayoutMediator(binding.galleryIndicator, binding.gallery) { _, _ -> }.attach()

        binding.render(
            AdDetailUiState.Content(ShowcaseAds.DETAIL_THREE),
            gallery,
        )
        // One section opened, the rest closed: the screenshot then shows both what an expanded
        // section holds and that the others collapse.
        binding.featuresSection.toggle()
        ShadowLooper.idleMainLooper()

        Screenshots.capture(activity, binding.root, "03-detail", height = 1400)
    }
}
