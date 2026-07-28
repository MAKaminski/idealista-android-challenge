package dev.mkaminski.idealista.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.model.Ad
import dev.mkaminski.idealista.model.MapBounds
import dev.mkaminski.idealista.model.withCoordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MapUiState {
    data object Loading : MapUiState

    data class Content(val ads: List<Ad>, val bounds: MapBounds) : MapUiState

    /** Nothing placeable — a different thing from "nothing cached", and it says so. */
    data object Empty : MapUiState
}

/**
 * The map reads the **same Room cache** the list does, so a favorite toggled on one screen is
 * already true on the other and the map needs no request of its own.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: AdRepository,
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = repository.observeAds()
        .map { ads ->
            val placeable = ads.withCoordinates()
            if (placeable.isEmpty()) {
                MapUiState.Empty
            } else {
                MapUiState.Content(placeable, MapBounds.around(placeable))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState.Loading,
        )

    init {
        // Opening the map first, on a cold start, must still fill it.
        viewModelScope.launch { repository.refreshAds() }
    }
}
