package dev.mkaminski.idealista.feature.detail

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.designsystem.Formatters
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

        gallery.submitList(detail.gallery.ifEmpty { ad.images })
        galleryIndicator.visibility =
            if (detail.gallery.size > 1) View.VISIBLE else View.GONE

        price.text = Formatters.price(ad.price, ad.currencySuffix)
        address.text = ad.address
        location.text = listOfNotNull(ad.district, ad.municipality).joinToString(", ")
        description.text = detail.comment.ifBlank { ad.description }

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

        characteristics.removeAllViews()
        buildList {
            detail.characteristics.roomNumber?.let { add(context.getString(R.string.detail_rooms, it)) }
            detail.characteristics.bathNumber?.let { add(context.getString(R.string.detail_baths, it)) }
            detail.characteristics.constructedAreaSquareMeters?.let {
                add(Formatters.area(it.toDouble()))
            }
            detail.characteristics.floor?.let { add(context.getString(R.string.detail_floor, it)) }
            detail.characteristics.hasLift?.let {
                add(context.getString(if (it) R.string.detail_lift else R.string.detail_no_lift))
            }
            detail.characteristics.communityCosts?.let {
                add(context.getString(R.string.detail_community_costs, Formatters.price(it, ad.currencySuffix)))
            }
        }.forEach { label ->
            characteristics.addView(
                Chip(context).apply {
                    text = label
                    isClickable = false
                    isCheckable = false
                },
            )
        }

        val certificate = detail.energyCertificate
        if (certificate?.consumptionType == null && certificate?.emissionsType == null) {
            energyHeading.visibility = View.GONE
            energy.visibility = View.GONE
        } else {
            energyHeading.visibility = View.VISIBLE
            energy.visibility = View.VISIBLE
            energy.text = listOfNotNull(
                certificate.consumptionType?.let {
                    context.getString(R.string.detail_energy_consumption, it.uppercase())
                },
                certificate.emissionsType?.let {
                    context.getString(R.string.detail_energy_emissions, it.uppercase())
                },
            ).joinToString(" · ")
        }
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
