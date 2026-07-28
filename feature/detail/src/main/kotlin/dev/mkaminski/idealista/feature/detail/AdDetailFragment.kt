package dev.mkaminski.idealista.feature.detail

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.designsystem.ExternalLinks
import dev.mkaminski.idealista.feature.detail.databinding.FragmentAdDetailBinding
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

        val galleryAdapter = GalleryAdapter(
            onImageClick = { image -> ExternalLinks.openInBrowser(requireContext(), image.url) },
        )
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
