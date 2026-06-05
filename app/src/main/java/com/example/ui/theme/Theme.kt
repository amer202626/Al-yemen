package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicSlateColorScheme = darkColorScheme(
    primary = CosmicSlatePrimary,
    secondary = CosmicSlateAccent,
    background = CosmicSlateBg,
    surface = CosmicSlateSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val CharcoalGoldColorScheme = darkColorScheme(
    primary = CharcoalGoldPrimary,
    secondary = CharcoalGoldAccent,
    background = CharcoalGoldBg,
    surface = CharcoalGoldSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val RoyalEmeraldColorScheme = darkColorScheme(
    primary = RoyalEmeraldPrimary,
    secondary = RoyalEmeraldAccent,
    background = RoyalEmeraldBg,
    surface = RoyalEmeraldSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun YemenDirectoryTheme(
    activeTheme: String = "COSMIC_SLATE",
    content: @Composable () -> Unit
) {
    val colors = when (activeTheme) {
        "CHARCOAL_GOLD" -> CharcoalGoldColorScheme
        "ROYAL_EMERALD" -> RoyalEmeraldColorScheme
        else -> CosmicSlateColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
