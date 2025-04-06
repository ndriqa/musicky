package com.ndriqa.musicky.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LightColorScheme = lightColorScheme(
    primary = MintGreen60,
    onPrimary = TextPrimary,
    primaryContainer = SoftGray30,
    onPrimaryContainer = TekheletAccent10,
    secondary = TekheletAccent10,
    onSecondary = Color.White,
    background = MintGreen60,
    onBackground = TextPrimary,
    surface = MintGreen60,
    onSurface = TextPrimary,
)

val DarkColorScheme = darkColorScheme(
    primary = TekheletAccent10,
    onPrimary = Color.White,
    primaryContainer = BackgroundDark,
    onPrimaryContainer = MintGreen60,
    secondary = MintGreen60,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = MintGreen60,
    surface = TekheletAccent10,
    onSurface = Color.White,
)

@Composable
fun MusickyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NdriqaTypography,
        content = content
    )
}