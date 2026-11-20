package com.asocexamprep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep teal (radio panel) with a signal-amber accent.
private val Light = lightColorScheme(
    primary = Color(0xFF0F5E5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEBE8),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF2B1700),
    background = Color(0xFFF8F6F1),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFF8F6F1),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFDDE4E2),
    onSurfaceVariant = Color(0xFF414847),
    surfaceContainer = Color(0xFFEFEDE7),
    surfaceContainerHigh = Color(0xFFE8E6E0),
    outline = Color(0xFF717877),
    error = Color(0xFFBA1A1A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8ED3D0),
    onPrimary = Color(0xFF003736),
    primaryContainer = Color(0xFF004F4E),
    onPrimaryContainer = Color(0xFFBDEBE8),
    secondary = Color(0xFFF2B544),
    onSecondary = Color(0xFF482A00),
    secondaryContainer = Color(0xFF684300),
    onSecondaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF101414),
    onBackground = Color(0xFFE0E3E1),
    surface = Color(0xFF101414),
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    surfaceContainer = Color(0xFF1C2020),
    surfaceContainerHigh = Color(0xFF262B2A),
    outline = Color(0xFF889391),
    error = Color(0xFFFFB4AB),
)

@Composable
fun RadioPrepTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
