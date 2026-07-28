package dev.mkaminski.idealista.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.model.AdDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface AdDetailUiState {
    data object Loading : AdDetailUiState
    data class Content(val detail: AdDetail) : AdDetailUiState
    data class Error(val cause: Throwable) : AdDetailUiState
}

@HiltViewModel
class AdDetailViewModel @Inject constructor(
    private val repository: AdRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Survives process death, so a backgrounded detail screen reopens on the right ad. */
    private val propertyCode: String = checkNotNull(savedStateHandle[ARG_PROPERTY_CODE]) {
        "AdDetailFragment requires a $ARG_PROPERTY_CODE argument"
    }

    private val retries = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    // `catch` sits *inside* flatMapLatest on purpose: outside it, a failure completes the whole
    // chain, `retries` stops being collected and retry() can never re-subscribe.
    val uiState: StateFlow<AdDetailUiState> = retries
        .flatMapLatest {
            repository.observeAdDetail(propertyCode)
                .map<AdDetail, AdDetailUiState> { AdDetailUiState.Content(it) }
                .catch { emit(AdDetailUiState.Error(it)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdDetailUiState.Loading,
        )

    fun retry() {
        retries.value += 1
    }

    fun toggleFavorite() {
        viewModelScope.launch { repository.toggleFavorite(propertyCode) }
    }

    companion object {
        const val ARG_PROPERTY_CODE = "propertyCode"
    }
}
