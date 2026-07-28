package dev.mkaminski.idealista.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.mkaminski.idealista.model.AppLanguage

/**
 * Settings, in Compose. The language rows are a `selectableGroup` of radio buttons rather than a
 * dropdown: five options fit on screen, and a list a user can read without opening it is easier to
 * recover from when the app is currently in a language they cannot read.
 *
 * `null` is a real option here — "follow the system" — not the absence of one.
 */
@Composable
fun SettingsScreen(
    selected: AppLanguage?,
    onLanguageSelected: (AppLanguage?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A Column rather than a LazyColumn: six rows, fixed for the life of the app, and one
    // `selectableGroup` around the whole set is what makes it a single radio group for screen
    // readers. Recycling would buy nothing and break that grouping.
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
            .testTag(TAG_SETTINGS),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.settings_language_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.settings_language_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        Column(modifier = Modifier.selectableGroup()) {
            // The system option carries no endonym: it is named in whatever language is in force,
            // which is exactly what it means.
            LanguageRow(
                label = stringResource(R.string.settings_language_system),
                supporting = null,
                isSelected = selected == null,
                onSelect = { onLanguageSelected(null) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))

            AppLanguage.entries.forEach { language ->
                LanguageRow(
                    label = language.endonym,
                    supporting = stringResource(language.labelRes()),
                    isSelected = selected == language,
                    onSelect = { onLanguageSelected(language) },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    supporting: String?,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Selectable on the whole row, with Role.RadioButton, so the accessible target is the
            // row rather than a 20dp circle — and the radio itself stays non-clickable.
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            // The endonym plus the name in the *current* language: "Français" alone is no help if
            // you do not read French.
            if (supporting != null && supporting != label) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Each language's name in whichever language the app is currently showing. */
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.ENGLISH -> R.string.settings_language_english
    AppLanguage.SPANISH -> R.string.settings_language_spanish
    AppLanguage.FRENCH -> R.string.settings_language_french
    AppLanguage.PORTUGUESE -> R.string.settings_language_portuguese
    AppLanguage.ITALIAN -> R.string.settings_language_italian
    AppLanguage.CHINESE -> R.string.settings_language_chinese
}

internal const val TAG_SETTINGS = "settings_screen"
