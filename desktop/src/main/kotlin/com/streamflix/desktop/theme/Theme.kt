package com.streamflix.desktop.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF0F0F12)
val SidebarDark = Color(0xFF16161C)
val SurfaceDark = Color(0xFF1E1E26)
val SurfaceHighlight = Color(0xFF2B2B36)
val AccentRed = Color(0xFFE50914)
val AccentBlue = Color(0xFF007AFF)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = AccentBlue,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceHighlight,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun StreamFlixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
