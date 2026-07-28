package dev.mkaminski.idealista.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The Compose half of the design system, built from the same tokens as `themes.xml`, so a Compose
 * screen and an XML screen are visually indistinguishable (ADR-0006).
 */
private val IdealistaGreen = Color(0xFF1E9E5A)
private val IdealistaGreenDark = Color(0xFF14713F)

private val LightColors = lightColorScheme(
    primary = IdealistaGreen,
    secondary = IdealistaGreenDark,
)

private val DarkColors = darkColorScheme(
    primary = IdealistaGreen,
    secondary = IdealistaGreenDark,
)

@Composable
fun IdealistaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
