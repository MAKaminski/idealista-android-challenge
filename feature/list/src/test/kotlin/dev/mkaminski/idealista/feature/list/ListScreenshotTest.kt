package dev.mkaminski.idealista.feature.list

import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dev.mkaminski.idealista.feature.list.databinding.FragmentAdListBinding
import dev.mkaminski.idealista.model.AdFilters
import dev.mkaminski.idealista.model.Operation
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
 * Renders the real list layout, the real adapter and the real chip row to PNGs for the README.
 *
 * Opt-in — see [Screenshots]. Skipped by an ordinary `testDebugUnitTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ListScreenshotTest {

    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        Screenshots.assumeEnabled()
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setTheme(dev.mkaminski.idealista.designsystem.R.style.Theme_Idealista)
        Screenshots.installPhotoLoader(activity)
    }

    @Test
    fun `list screen`() {
        val binding = bind(AdFilters())

        Screenshots.capture(activity, binding.root, "01-list", height = 1500)
    }

    /** The same screen with two chips active, so the filter row is visibly doing something. */
    @Test
    fun `list screen filtered`() {
        val filters = AdFilters(operation = Operation.RENT, minRooms = 3)
        val binding = bind(filters, ads = ShowcaseAds.ALL.filter { it.operation == Operation.RENT })

        Screenshots.capture(activity, binding.root, "02-list-filtered", height = 900)
    }

    private fun bind(
        filters: AdFilters,
        ads: List<dev.mkaminski.idealista.model.Ad> = ShowcaseAds.ALL,
    ): FragmentAdListBinding {
        val binding = FragmentAdListBinding.inflate(LayoutInflater.from(activity))
        val adapter = AdListAdapter(onAdClick = {}, onFavoriteClick = {})
        binding.adList.layoutManager = LinearLayoutManager(activity)
        binding.adList.adapter = adapter
        binding.filterChips.bindFilters(filters, onToggle = {}, onClear = {})
        binding.resultCount.text = activity.resources.getQuantityString(
            R.plurals.list_result_count,
            ads.size,
            ads.size,
        )
        adapter.submitList(ads)
        ShadowLooper.idleMainLooper()
        return binding
    }
}
