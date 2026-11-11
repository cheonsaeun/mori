package com.cheonsaeun.mori.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MoriPalette(private val dark: Boolean = false) {
    val Ink = if(dark) Color(0xFFECECF7) else Color(0xFF202132)
    val Secondary = if(dark) Color(0xFFC1C4D8) else Color(0xFF62677F)
    val Muted = if(dark) Color(0xFFC1C4D8) else Color(0xFF767B91)
    val Canvas = if(dark) Color(0xFF141625) else Color(0xFFF5F5FC)
    val Surface = if(dark) Color(0xFF1D2033) else Color.White
    val Soft = if(dark) Color(0xFF282B43) else Color(0xFFF0EFF9)
    val Outline = if(dark) Color(0xFF4A4F6A) else Color(0xFFE0E2EF)
    val Primary = if(dark) Color(0xFFADB5FF) else Color(0xFF5058D8)
    val Blue = Color(0xFF748FFF)
    val Lavender = Color(0xFFC5A9F4)
    val Navy = Color(0xFF171A36)
    val Heat = if(dark) Color(0xFFFFB4AA) else Color(0xFFB94745)
    val HeatSoft = if(dark) Color(0xFF462A2B) else Color(0xFFFFEFEB)
    val Cool = if(dark) Color(0xFF98D7FF) else Color(0xFF216F97)
    val CoolSoft = if(dark) Color(0xFF1B394B) else Color(0xFFE8F5FC)
    val Success = if(dark) Color(0xFFA0DDC1) else Color(0xFF22785B)
    val SuccessSoft = if(dark) Color(0xFF1D3B32) else Color(0xFFE9F5EF)
    val OnPrimary = if(dark) Color(0xFF171A36) else Color.White
    val BrandGradient = Brush.linearGradient(listOf(Blue, Primary, Lavender))
    val AmbientGradient = if(dark) Brush.linearGradient(listOf(Canvas,Surface)) else Brush.linearGradient(listOf(Color(0xFFF2F3FF), Color(0xFFFAF8FD), Color(0xFFF1F3FD)))
    val MediaGradient = Brush.linearGradient(listOf(Color(0xFF30345D), Navy, Color(0xFF252347)))
}

private val LocalMoriPalette = staticCompositionLocalOf { MoriPalette() }
val MoriColors: MoriPalette
    @Composable get() = LocalMoriPalette.current

object MoriSpace {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Page = 32.dp
    val Touch = 80.dp
}

object MoriShapes {
    val Panel = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(22.dp)
    val Item = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(100.dp)
}

object MoriMotion {
    const val SelectionMillis = 220
    const val PollMillis = 1000L
}

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    lineHeight = height.sp,
    fontWeight = weight,
)

val MoriTypography = Typography(
    displaySmall = type(36, 48, FontWeight.Bold),
    headlineLarge = type(32, 42, FontWeight.Bold),
    headlineMedium = type(28, 38, FontWeight.Bold),
    titleLarge = type(24, 34, FontWeight.SemiBold),
    titleMedium = type(20, 28, FontWeight.SemiBold),
    titleSmall = type(20, 28, FontWeight.SemiBold),
    bodyLarge = type(22, 32),
    bodyMedium = type(20, 30),
    bodySmall = type(18, 27),
    labelLarge = type(20, 28, FontWeight.SemiBold),
    labelMedium = type(18, 26, FontWeight.Medium),
    labelSmall = type(16, 24, FontWeight.Medium),
)

@Composable
fun MoriTheme(darkOverride: Boolean? = null, content: @Composable () -> Unit) {
    val dark=darkOverride ?: isSystemInDarkTheme()
    val palette=remember(dark) {MoriPalette(dark)}
    val base=if(dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalMoriPalette provides palette) {
        MaterialTheme(colorScheme=base.copy(primary=palette.Primary,onPrimary=palette.OnPrimary,background=palette.Canvas,onBackground=palette.Ink,surface=palette.Surface,onSurface=palette.Ink,onSurfaceVariant=palette.Secondary,outline=palette.Outline),typography=MoriTypography,content=content)
    }
}
