package dev.mkaminski.idealista.feature.settings

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.mkaminski.idealista.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The screen scrolls and the test window is shorter than the list, so `assertExists` — which only
 * needs the node in the semantics tree — is enough for presence, while anything touching pixels
 * (a click, a selection assertion) has to `performScrollTo()` the row first.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every shipped language is offered`() {
        composeRule.setContent {
            SettingsScreen(selected = null, onLanguageSelected = {})
        }

        AppLanguage.entries.forEach { language ->
            composeRule.onNodeWithText(language.endonym).assertExists()
        }
    }

    /**
     * The reason each row is labelled with its endonym: a user stuck in a language they cannot read
     * has to be able to find their own.
     */
    @Test
    fun `each language is named in its own language`() {
        composeRule.setContent {
            SettingsScreen(selected = null, onLanguageSelected = {})
        }

        composeRule.onNodeWithText("Español").assertExists()
        composeRule.onNodeWithText("Français").assertExists()
        composeRule.onNodeWithText("Português").assertExists()
        composeRule.onNodeWithText("Italiano").assertExists()
    }

    @Test
    fun `the current language is the selected row`() {
        composeRule.setContent {
            SettingsScreen(selected = AppLanguage.PORTUGUESE, onLanguageSelected = {})
        }

        composeRule.onNodeWithText("Português").performScrollTo().assertIsSelected()
    }

    @Test
    fun `choosing a language reports it`() {
        var chosen: AppLanguage? = AppLanguage.ENGLISH
        composeRule.setContent {
            SettingsScreen(selected = null, onLanguageSelected = { chosen = it })
        }

        composeRule.onNodeWithText("Italiano").performScrollTo().performClick()

        assertEquals(AppLanguage.ITALIAN, chosen)
    }

    /** "Follow the system" is a real choice, not the absence of one — it has to be selectable back. */
    @Test
    fun `the system default is offered and reports null`() {
        var chosen: AppLanguage? = AppLanguage.FRENCH
        composeRule.setContent {
            SettingsScreen(selected = AppLanguage.FRENCH, onLanguageSelected = { chosen = it })
        }

        composeRule.onNodeWithText("System default").performScrollTo().performClick()

        assertNull(chosen)
    }

    @Test
    fun `the system default is selected when no language is forced`() {
        composeRule.setContent {
            SettingsScreen(selected = null, onLanguageSelected = {})
        }

        composeRule.onNodeWithText("System default").assertIsSelected()
    }

    /** The picker is only useful if it says what it does — this is per-app, not device-wide. */
    @Test
    fun `the screen says the choice applies to this app only`() {
        composeRule.setContent {
            SettingsScreen(selected = null, onLanguageSelected = {})
        }

        composeRule
            .onNodeWithText("Applies to this app only. Your device stays as it is.")
            .assertExists()
    }
}
