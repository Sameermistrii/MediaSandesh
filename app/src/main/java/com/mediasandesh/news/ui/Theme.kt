package com.mediasandesh.news.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mediasandesh.news.AppPrefs

val BrandRed = Color(0xFFD0121A)
val Accent = Color(0xFFE29C04)

private val LightColors = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color.White,
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFEBEEF4),
    onSurfaceVariant = Color(0xFF5F6368),
    outlineVariant = Color(0xFFE6E8EE),
    secondaryContainer = Color(0xFFFFE8E8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF4B4F),
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA8A8A8),
    outlineVariant = Color(0xFF2C2C2C),
    secondaryContainer = Color(0xFF3A1416)
)

val Ink: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val Paper: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val CardWhite: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val Muted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val ChipBg: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val Hairline: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
val RedInk: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary

private fun typography(scheme: ColorScheme) = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, color = scheme.onSurface),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = scheme.onSurface),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = scheme.onSurfaceVariant)
)

@Composable
fun isAppDark(): Boolean = when (AppPrefs.theme) {
    AppPrefs.THEME_DARK -> true
    AppPrefs.THEME_LIGHT -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun MediaSandeshTheme(dark: Boolean = isAppDark(), content: @Composable () -> Unit) {
    val scheme = if (dark) DarkColors else LightColors
    val type = remember(dark) { typography(scheme) }
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
