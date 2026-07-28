package dev.mkaminski.idealista.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.AdFilters
import dev.mkaminski.idealista.model.applyFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One sealed state per screen, collected with `repeatOnLifecycle`. No LiveData (see CLAUDE.md). */
sealed interface AdListUiState {
    data object Loading : AdListUiState

    data class Content(
        val ads: List<Ad>,
        val isRefreshing: Boolean,
        val totalCount: Int,
    ) : AdListUiState

    /** Nothing cached at all — a different situation from "filters exclude everything". */
    data object Empty : AdListUiState

    /** The cache has ads, but none match. Says so, rather than looking like a failed load. */
    data object NoMatches : AdListUiState

    data class Error(val cause: Throwable) : AdListUiState
}

@HiltViewModel
class AdListViewModel @Inject constructor(
    private val repository: AdRepository,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<Throwable?>(null)
    private val _filters = MutableStateFlow(AdFilters())

    val filters: StateFlow<AdFilters> = _filters.asStateFlow()

    val uiState: StateFlow<AdListUiState> =
        combine(
            repository.observeAds(),
            isRefreshing,
            refreshError,
            _filters,
        ) { ads, refreshing, error, activeFilters ->
            val visible = ads.applyFilters(activeFilters)
            when {
                visible.isNotEmpty() -> AdListUiState.Content(visible, refreshing, ads.size)
                // A cached list beats an error banner: a failed refresh must not hide ads the user
                // can still read offline.
                ads.isNotEmpty() -> AdListUiState.NoMatches
                error != null -> AdListUiState.Error(error)
                refreshing -> AdListUiState.Loading
                else -> AdListUiState.Empty
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdListUiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.refreshAds()
                .onSuccess { refreshError.value = null }
                .onFailure { refreshError.value = it }
            isRefreshing.value = false
        }
    }

    fun toggleFavorite(propertyCode: String) {
        viewModelScope.launch { repository.toggleFavorite(propertyCode) }
    }

    /** Filtering is client-side over the cache, so it works offline and needs no request. */
    fun updateFilters(transform: (AdFilters) -> AdFilters) {
        _filters.value = transform(_filters.value)
    }

    fun clearFilters() {
        _filters.value = AdFilters()
    }
}
