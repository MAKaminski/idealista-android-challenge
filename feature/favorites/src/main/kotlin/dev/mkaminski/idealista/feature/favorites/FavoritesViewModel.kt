package dev.mkaminski.idealista.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.model.Ad
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data object Empty : FavoritesUiState
    data class Content(val favorites: List<Ad>) : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: AdRepository,
) : ViewModel() {

    /**
     * Reads the same `observeAds()` the XML screens read, filtered to favorites and ordered by when
     * they were saved. No separate favorites query exists, so the three screens cannot drift.
     */
    val uiState: StateFlow<FavoritesUiState> = repository.observeAds()
        .map { ads ->
            val favorites = ads.filter { it.isFavorite }
                .sortedByDescending { it.favoritedAt }
            if (favorites.isEmpty()) FavoritesUiState.Empty else FavoritesUiState.Content(favorites)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading,
        )

    fun removeFavorite(propertyCode: String) {
        viewModelScope.launch { repository.toggleFavorite(propertyCode) }
    }
}
