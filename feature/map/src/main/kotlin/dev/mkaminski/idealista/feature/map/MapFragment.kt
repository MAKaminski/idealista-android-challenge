package dev.mkaminski.idealista.feature.map

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.designsystem.Formatters
import dev.mkaminski.idealista.feature.map.databinding.FragmentMapBinding
import dev.mkaminski.idealista.model.Ad
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * A pannable, zoomable map of every cached ad.
 *
 * OpenStreetMap tiles through osmdroid rather than Google Maps: a Maps SDK key is a secret, and a
 * submission should not carry one. The trade is that tiles come from the OSM foundation's servers,
 * so the user agent is set as their usage policy requires (ADR-0010).
 */
@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    private val viewModel: MapViewModel by viewModels()

    private var binding: FragmentMapBinding? = null

    /** Set by the host, so this module — like every other feature — owns no navigation. */
    var onAdSelected: ((String) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentMapBinding.bind(view).also { this.binding = it }

        // OSM's tile policy requires an identifying user agent; the default is the library's name,
        // which would get the whole app rate-limited alongside every other osmdroid client.
        Configuration.getInstance().userAgentValue = requireContext().packageName

        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT,
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { binding.render(it) }
            }
        }
    }

    private fun FragmentMapBinding.render(state: MapUiState) {
        mapEmpty.isVisible = state is MapUiState.Empty
        if (state !is MapUiState.Content) return

        mapSummary.text = resources.getQuantityString(
            R.plurals.map_pin_count,
            state.ads.size,
            state.ads.size,
        )

        map.overlays.clear()
        state.ads.forEach { ad -> map.overlays.add(marker(ad)) }

        // zoomToBoundingBox before layout is a no-op, so it is posted onto the map's own queue.
        map.post {
            map.zoomToBoundingBox(
                BoundingBox(state.bounds.north, state.bounds.east, state.bounds.south, state.bounds.west),
                false,
            )
        }
        map.invalidate()
    }

    private fun FragmentMapBinding.marker(ad: Ad) = Marker(map).apply {
        position = GeoPoint(ad.latitude ?: 0.0, ad.longitude ?: 0.0)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = Formatters.price(ad.price, ad.currencySuffix)
        snippet = ad.address
        icon = androidx.core.content.ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_map_pin,
        )
        setOnMarkerClickListener { _, _ ->
            showSelection(ad)
            true
        }
    }

    private fun FragmentMapBinding.showSelection(ad: Ad) {
        selectionPrice.text = Formatters.price(ad.price, ad.currencySuffix)
        selectionAddress.text = listOfNotNull(ad.address, ad.district).joinToString(", ")
        selectionSummary.text = getString(
            R.string.map_selection_summary,
            ad.rooms,
            ad.bathrooms,
            Formatters.area(ad.sizeSquareMeters),
        )
        selectionCard.isVisible = true
        // The card is a shortcut into the same detail screen the list opens — one destination, two
        // ways in, so nothing about the detail screen has to know where the user came from.
        selectionCard.setOnClickListener { onAdSelected?.invoke(ad.propertyCode) }
    }

    override fun onResume() {
        super.onResume()
        binding?.map?.onResume()
    }

    override fun onPause() {
        binding?.map?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        // The MapView holds tile threads and a bitmap cache; leaking it leaks both.
        binding?.map?.onDetach()
        binding = null
        super.onDestroyView()
    }
}
