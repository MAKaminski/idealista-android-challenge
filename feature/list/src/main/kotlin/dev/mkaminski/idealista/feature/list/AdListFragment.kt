package dev.mkaminski.idealista.feature.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.feature.list.databinding.FragmentAdListBinding
import kotlinx.coroutines.launch

/**
 * The mandatory XML list screen. ViewBinding, no findViewById, one StateFlow collected under
 * `repeatOnLifecycle` (see CLAUDE.md §7).
 */
@AndroidEntryPoint
class AdListFragment : Fragment(R.layout.fragment_ad_list) {

    private val viewModel: AdListViewModel by viewModels()

    private var binding: FragmentAdListBinding? = null

    /** Set by the host so this feature module never depends on `:feature:detail`. */
    var onAdSelected: ((String) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAdListBinding.bind(view).also { this.binding = it }

        val adapter = AdListAdapter(
            onAdClick = { onAdSelected?.invoke(it.propertyCode) },
            onFavoriteClick = { viewModel.toggleFavorite(it.propertyCode) },
        )
        binding.adList.layoutManager = LinearLayoutManager(requireContext())
        binding.adList.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retry.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> binding.render(state, adapter) }
            }
        }
    }

    private fun FragmentAdListBinding.render(state: AdListUiState, adapter: AdListAdapter) {
        loading.visibility = if (state is AdListUiState.Loading) View.VISIBLE else View.GONE
        errorState.visibility = if (state is AdListUiState.Error) View.VISIBLE else View.GONE
        emptyState.visibility = if (state is AdListUiState.Empty) View.VISIBLE else View.GONE
        adList.visibility = if (state is AdListUiState.Content) View.VISIBLE else View.GONE

        when (state) {
            is AdListUiState.Content -> {
                swipeRefresh.isRefreshing = state.isRefreshing
                adapter.submitList(state.ads)
            }
            else -> swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        // The RecyclerView outlives the binding otherwise, leaking the view hierarchy.
        binding?.adList?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
