package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NabdLightColorScheme = lightColorScheme(
    primary = NabdPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECE7FF),
    onPrimaryContainer = NabdDeepViolet,
    secondary = NabdDeepViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9F5),
    onSecondaryContainer = NabdDeepViolet,
    tertiary = NabdMineralGray,
    onTertiary = NabdObsidian,
    background = NabdBone,
    onBackground = NabdObsidian,
    surface = NabdSurfaceWhite,
    onSurface = NabdObsidian,
    surfaceVariant = NabdSurfaceSubtle,
    onSurfaceVariant = NabdObsidian,
    outline = NabdBorder,
    outlineVariant = Color(0xFFE4DFD5)
)

private val NabdDarkColorScheme = darkColorScheme(
    primary = NabdPurpleLight,
    onPrimary = NabdObsidian,
    primaryContainer = NabdDeepViolet,
    onPrimaryContainer = Color.White,
    secondary = NabdPurple,
    onSecondary = Color.White,
    background = NabdObsidian,
    onBackground = NabdBone,
    surface = Color(0xFF221F29),
    onSurface = NabdBone,
    surfaceVariant = Color(0xFF2B2834),
    onSurfaceVariant = NabdBone,
    outline = Color(0xFF3F3B49)
)

@Composable
fun GestionAtelierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NabdDarkColorScheme else NabdLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NabdTypography,
        content = content
    )
}
