package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = PurplePrimary,
  onPrimary = Color.White,
  primaryContainer = PurpleSurface,
  onPrimaryContainer = PurpleDark,
  secondary = PurpleLight,
  onSecondary = Color.White,
  background = MatteSurface,
  onBackground = TextPrimary,
  surface = MatteSurfaceCard,
  onSurface = TextPrimary,
  surfaceVariant = MatteSurfaceContainer,
  onSurfaceVariant = TextSecondary,
  outline = MatteBorder
)

@Composable
fun SuiviTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}
