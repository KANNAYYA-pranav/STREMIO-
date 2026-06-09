package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NetstreamRed,
    secondary = NetstreamDarkRed,
    background = NetstreamPureBlack,
    surface = NetstreamBlack,
    surfaceVariant = NetstreamGrey,
    onPrimary = NetstreamWhite,
    onSecondary = NetstreamWhite,
    onBackground = NetstreamWhite,
    onSurface = NetstreamWhite,
    onSurfaceVariant = NetstreamLightGrey
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force movie theater dark mode by default
    dynamicColor: Boolean = false, // Keep NetStream identity color branding intact
    content: @Composable () -> Unit,
) {
    // We use the custom dark brand theme to guarantee Netflix signature style
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
