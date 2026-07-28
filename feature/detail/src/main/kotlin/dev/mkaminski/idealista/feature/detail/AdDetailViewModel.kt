package dev.mkaminski.idealista.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mkaminski.idealista.data.AdRepository
import dev.mkaminski.idealista.data.translate.AdTextTranslator
import dev.mkaminski.idealista.data.translate.CurrentLanguage
import dev.mkaminski.idealista.model.AdDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface AdDetailUiState {
    data object Loading : AdDetailUiState

    /**
     * [translatedComment] is the description in the user's language, or `null` when it is already
     * Spanish, still translating, or could not be translated. The screen falls back to the original
     * in every one of those cases — the listing text never disappears.
     */
    data class Content(
        val detail: AdDetail,
        val translatedComment: String? = null,
    ) : AdDetailUiState

    data class Error(val cause: Throwable) : AdDetailUiState
}

@HiltViewModel
class AdDetailViewModel @Inject constructor(
    private val repository: AdRepository,
    private val translator: AdTextTranslator,
    private val language: CurrentLanguage,
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
        // The description arrives in Spanish whatever the UI language is. Translating downstream of
        // the content emission means the screen renders immediately and gains the translation when
        // it is ready, rather than waiting on a model download to show anything at all.
        .transformLatest { state ->
            emit(state)
            if (state !is AdDetailUiState.Content) return@transformLatest
            val target = language() ?: return@transformLatest
            val source = state.detail.comment.ifBlank { state.detail.ad.description }
            translator.translate(source, target)?.let { emit(state.copy(translatedComment = it)) }
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
