package dev.mkaminski.idealista.feature.detail

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.designsystem.Formatters
import dev.mkaminski.idealista.designsystem.IdealistaTheme
import dev.mkaminski.idealista.feature.detail.databinding.FragmentAdDetailBinding
import dev.mkaminski.idealista.model.AdDetail
import kotlinx.coroutines.launch

/**
 * The mandatory XML detail screen.
 *
 * Everything identifying the property — price, address, location — comes from the ad the user
 * opened, not from the detail response, which always describes ad 1 (ADR-0005).
 */
@AndroidEntryPoint
class AdDetailFragment : Fragment(R.layout.fragment_ad_detail) {

    private val viewModel: AdDetailViewModel by viewModels()

    private var binding: FragmentAdDetailBinding? = null

    /** Set by the host so this module owns no navigation. */
    var onNavigateUp: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAdDetailBinding.bind(view).also { this.binding = it }

        val galleryAdapter = GalleryAdapter()
        binding.gallery.adapter = galleryAdapter
        TabLayoutMediator(binding.galleryIndicator, binding.gallery) { _, _ -> }.attach()

        binding.toolbar.setNavigationOnClickListener { onNavigateUp?.invoke() }
        binding.retry.setOnClickListener { viewModel.retry() }
        binding.favoriteFab.setOnClickListener { viewModel.toggleFavorite() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> binding.render(state, galleryAdapter) }
            }
        }
    }

    private fun FragmentAdDetailBinding.render(state: AdDetailUiState, gallery: GalleryAdapter) {
        loading.visibility = if (state is AdDetailUiState.Loading) View.VISIBLE else View.GONE
        errorState.visibility = if (state is AdDetailUiState.Error) View.VISIBLE else View.GONE
        content.visibility = if (state is AdDetailUiState.Content) View.VISIBLE else View.GONE
        favoriteFab.visibility = if (state is AdDetailUiState.Content) View.VISIBLE else View.GONE
        appBar.visibility = if (state is AdDetailUiState.Content) View.VISIBLE else View.GONE

        if (state !is AdDetailUiState.Content) return
        bindContent(state.detail, gallery)
    }

    private fun FragmentAdDetailBinding.bindContent(detail: AdDetail, gallery: GalleryAdapter) {
        val ad = detail.ad
        val context = root.context

        gallery.submitList(detail.gallery)
        galleryIndicator.visibility =
            if (detail.gallery.size > 1) View.VISIBLE else View.GONE

        price.text = Formatters.price(ad.price, ad.currencySuffix)
        address.text = ad.address
        location.text = listOfNotNull(ad.district, ad.municipality).joinToString(", ")
        description.text = detail.comment.ifBlank { ad.description }

        // Section summaries: a collapsed accordion still says how much is inside it.
        locationDetail.text = listOfNotNull(
            ad.district,
            ad.neighborhood,
            ad.municipality,
            ad.province,
        ).distinct().joinToString(" · ")
        locationSection.summary = ad.municipality

        val featureLabels = buildList {
            if (ad.features.hasAirConditioning) add(context.getString(R.string.detail_feature_air_conditioning))
            if (ad.features.hasBoxRoom) add(context.getString(R.string.detail_feature_box_room))
            if (ad.features.hasSwimmingPool) add(context.getString(R.string.detail_feature_pool))
            if (ad.features.hasTerrace) add(context.getString(R.string.detail_feature_terrace))
            if (ad.features.hasGarden) add(context.getString(R.string.detail_feature_garden))
            if (ad.exterior) add(context.getString(R.string.detail_feature_exterior))
            ad.parking?.let { parking ->
                if (parking.hasParkingSpace) {
                    add(
                        context.getString(
                            if (parking.includedInPrice) {
                                R.string.detail_feature_parking_included
                            } else {
                                R.string.detail_feature_parking
                            },
                        ),
                    )
                }
            }
        }
        features.removeAllViews()
        featureLabels.forEach { label ->
            features.addView(
                Chip(context).apply {
                    text = label
                    isClickable = false
                    isCheckable = false
                },
            )
        }
        featuresSection.summary =
            if (featureLabels.isEmpty()) null else featureLabels.size.toString()
        featuresSection.isVisible = featureLabels.isNotEmpty()

        val favoritedAt = ad.favoritedAt
        if (favoritedAt != null) {
            favoritedOn.text = context.getString(
                R.string.favorite_saved_on,
                Formatters.favoritedDate(favoritedAt),
            )
            favoritedOn.visibility = View.VISIBLE
            favoriteFab.setText(R.string.detail_favorite_remove)
            favoriteFab.setIconResource(
                dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite_filled,
            )
        } else {
            favoritedOn.visibility = View.GONE
            favoriteFab.setText(R.string.detail_favorite_add)
            favoriteFab.setIconResource(dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite)
        }

        // The characteristics panel is Compose inside this XML screen (ADR-0006). Labels are built
        // here so formatting and string lookup stay out of the composable.
        val labels = characteristicLabels(
            characteristics = detail.characteristics,
            roomsLabel = { context.getString(R.string.detail_rooms, it) },
            bathsLabel = { context.getString(R.string.detail_baths, it) },
            areaLabel = { Formatters.area(it.toDouble()) },
            floorLabel = { context.getString(R.string.detail_floor, it) },
            liftLabel = { context.getString(if (it) R.string.detail_lift else R.string.detail_no_lift) },
            communityCostsLabel = {
                context.getString(R.string.detail_community_costs, Formatters.price(it, ad.currencySuffix))
            },
        )
        characteristics.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        characteristics.setContent {
            IdealistaTheme {
                CharacteristicsPanel(labels = labels, certificate = detail.energyCertificate)
            }
        }
        characteristicsSection.summary = labels.size.toString()
    }

    override fun onDestroyView() {
        binding?.gallery?.adapter = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(propertyCode: String) = AdDetailFragment().apply {
            arguments = bundleOf(AdDetailViewModel.ARG_PROPERTY_CODE to propertyCode)
        }
    }
}
