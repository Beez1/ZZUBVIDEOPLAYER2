package com.example.zzubvideoplayer.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val MidnightBlue = Color(0xFF1E3D59)
private val ButtonPrimaryColor = Color(0xFF17A2B8)
private val ButtonSecondaryColor = Color(0xFF6C63FF)
private val SurfaceColor = Color(0xFF121212)
private val NavBarColor = Color(0xFF1A1A1A)
private val AccentColor = Color(0xFF00E676)
private val ErrorColor = Color(0xFFFF5252)
private val SuccessColor = Color(0xFF4CAF50)
private val TextPrimaryColor = Color(0xFFEEEEEE)
private val TextSecondaryColor = Color(0xFFBDBDBD)

val CustomDarkColorScheme = darkColorScheme(
    primary = MidnightBlue,
    secondary = ButtonPrimaryColor,
    surface = SurfaceColor,
    onSurface = TextPrimaryColor,
    background = SurfaceColor,
    onBackground = TextPrimaryColor,
    primaryContainer = ButtonPrimaryColor,
    secondaryContainer = ButtonSecondaryColor,
    onPrimary = TextPrimaryColor,
    onSecondary = TextPrimaryColor,
    onSurfaceVariant = TextSecondaryColor,
    inversePrimary = NavBarColor,
    error = ErrorColor,
    onError = TextPrimaryColor,
    tertiary = AccentColor,
    onTertiary = Color.Black,
    tertiaryContainer = SuccessColor,
    onTertiaryContainer = TextPrimaryColor
)

@Composable
fun ZZUBVIDEOPLAYERTheme(
    content: @Composable () -> Unit
) {
    val animatedColorScheme by animateColorAsState(
        targetValue = CustomDarkColorScheme.primary,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "color"
    )

    MaterialTheme(
        colorScheme = CustomDarkColorScheme.copy(
            primary = animatedColorScheme
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}