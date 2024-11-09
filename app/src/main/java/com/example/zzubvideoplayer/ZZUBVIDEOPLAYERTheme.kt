package com.example.zzubvideoplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MidnightBlue = Color(0xFF003366)
private val ButtonPrimaryColor = Color(0xFF336699)
private val ButtonSecondaryColor = Color(0xFF6699CC)
private val SurfaceColor = Color(0xFF1A1A2E)
private val NavBarColor = Color(0xFF002244)

val CustomDarkColorScheme = darkColorScheme(
    primary = MidnightBlue,
    secondary = ButtonPrimaryColor,
    surface = SurfaceColor,
    onSurface = Color.White,
    background = SurfaceColor,
    onBackground = Color.White,
    primaryContainer = ButtonPrimaryColor,
    secondaryContainer = ButtonSecondaryColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurfaceVariant = Color.Gray,
    inversePrimary = NavBarColor
)

@Composable
fun ZZUBVIDEOPLAYERTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
