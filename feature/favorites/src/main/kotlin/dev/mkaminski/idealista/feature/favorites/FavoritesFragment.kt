package dev.mkaminski.idealista.feature.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.designsystem.IdealistaTheme

/** A Fragment whose entire content is Compose — the interop seam described in ADR-0006. */
@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private val viewModel: FavoritesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            IdealistaTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                FavoritesScreen(
                    state = state,
                    onRemoveFavorite = viewModel::removeFavorite,
                )
            }
        }
    }
}
