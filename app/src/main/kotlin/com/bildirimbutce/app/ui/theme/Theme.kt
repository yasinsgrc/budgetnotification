package com.bildirimbutce.app.ui.theme

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

private val Ink = Color(0xFF11151C)
private val Teal = Color(0xFF0E7C66)
private val TealLight = Color(0xFF5FD3B4)
private val Sand = Color(0xFFF6F4EF)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Color(0xFF4A5A55),
    background = Sand,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00382C),
    secondary = Color(0xFFB0CCC4),
    background = Color(0xFF0E1210),
    surface = Color(0xFF171D1A),
    onBackground = Color(0xFFE3E3E0),
    onSurface = Color(0xFFE3E3E0)
)

@Composable
fun BildirimButceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
