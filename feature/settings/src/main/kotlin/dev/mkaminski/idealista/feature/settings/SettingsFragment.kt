package dev.mkaminski.idealista.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import dev.mkaminski.idealista.designsystem.AppLocales
import dev.mkaminski.idealista.designsystem.IdealistaTheme

/**
 * Settings has no ViewModel on purpose: there is no state to hold. The selected language lives in
 * `AppCompatDelegate`, which is where the system reads it from, and applying one recreates the
 * activity — so a ViewModel would only ever mirror a value it does not own (ADR-0009).
 */
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            IdealistaTheme {
                // Recreation is asynchronous, so the radio has to move on tap rather than waiting
                // for the new activity to read the delegate back.
                var selected by remember { mutableStateOf(AppLocales.current()) }
                SettingsScreen(
                    selected = selected,
                    onLanguageSelected = { language ->
                        selected = language
                        AppLocales.apply(language)
                    },
                )
            }
        }
    }
}
