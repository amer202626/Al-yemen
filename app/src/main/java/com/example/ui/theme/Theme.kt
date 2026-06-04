package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicSilverScheme = darkColorScheme(
    primary = CosmicSilverPrimary,
    secondary = CosmicSilverSecondary,
    background = CosmicSilverBackground,
    surface = CosmicSilverSurface,
    onPrimary = Color(0xFF131B1E),
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LuxuryGoldScheme = darkColorScheme(
    primary = LuxuryGoldPrimary,
    secondary = LuxuryGoldSecondary,
    background = LuxuryGoldBackground,
    surface = LuxuryGoldSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val ElegantEmeraldScheme = darkColorScheme(
    primary = ElegantEmeraldPrimary,
    secondary = ElegantEmeraldSecondary,
    background = ElegantEmeraldBackground,
    surface = ElegantEmeraldSurface,
    onPrimary = Color(0xFF040C08),
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

fun parseHexColor(hexStr: String, fallback: Color): Color {
    return try {
        val clean = hexStr.replace("#", "").trim()
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#FF$clean"))
        } else if (clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun YemenTheme(
    themeChoice: String = "COSMIC_SILVER",
    customPrimaryHex: String = "#8A9EA7",
    customBgHex: String = "#121A1E",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeChoice.uppercase()) {
        "COSMIC_SILVER" -> CosmicSilverScheme
        "LUXURY_GOLD" -> LuxuryGoldScheme
        "ELEGANT_EMERALD" -> ElegantEmeraldScheme
        "CUSTOM" -> {
            val primaryParsed = parseHexColor(customPrimaryHex, CosmicSilverPrimary)
            val bgParsed = parseHexColor(customBgHex, CosmicSilverBackground)
            darkColorScheme(
                primary = primaryParsed,
                secondary = CosmicSilverSecondary,
                background = bgParsed,
                surface = Color(0xFF1E282E),
                onPrimary = Color.Black,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
        else -> CosmicSilverScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
