package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = PurpleBrand,
  onPrimary = Color.White,
  primaryContainer = PurpleSurface,
  onPrimaryContainer = PurpleBrandDark,
  secondary = PurpleBrandLight,
  onSecondary = Color.White,
  background = BoneBackground,
  onBackground = TextObsidian,
  surface = MatteSurfaceWhite,
  onSurface = TextObsidian,
  surfaceVariant = MatteSurfaceMuted,
  onSurfaceVariant = TextMuted,
  outline = BorderSubtle
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
