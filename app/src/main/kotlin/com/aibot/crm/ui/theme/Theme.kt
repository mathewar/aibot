package com.aibot.crm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val primaryColor = Color(0xFF1A6B5A)
private val secondaryColor = Color(0xFF4A9D8A)

private val DarkColors = darkColorScheme(
    primary = primaryColor,
    secondary = secondaryColor,
    surface = Color(0xFF121212),
)

private val LightColors = lightColorScheme(
    primary = primaryColor,
    secondary = secondaryColor,
)

@Composable
fun CrmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
