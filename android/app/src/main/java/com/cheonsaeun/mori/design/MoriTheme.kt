package com.cheonsaeun.mori.design

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

object MoriColors {
    val Ink = Color(0xFF202132)
    val Secondary = Color(0xFF62677F)
    val Muted = Color(0xFF767B91)
    val Canvas = Color(0xFFF5F5FC)
    val Surface = Color.White
    val Soft = Color(0xFFF0EFF9)
    val Outline = Color(0xFFE0E2EF)
    val Primary = Color(0xFF555DEB)
    val Blue = Color(0xFF748FFF)
    val Lavender = Color(0xFFC5A9F4)
    val Navy = Color(0xFF171A36)
    val Heat = Color(0xFFB94745)
    val HeatSoft = Color(0xFFFFEFEB)
    val Cool = Color(0xFF216F97)
    val CoolSoft = Color(0xFFE8F5FC)
    val Success = Color(0xFF22785B)
    val SuccessSoft = Color(0xFFE9F5EF)
    val BrandGradient = Brush.linearGradient(listOf(Blue, Primary, Lavender))
    val AmbientGradient = Brush.linearGradient(listOf(Color(0xFFF2F3FF), Color(0xFFFAF8FD), Color(0xFFF1F3FD)))
    val MediaGradient = Brush.linearGradient(listOf(Color(0xFF30345D), Navy, Color(0xFF252347)))
}

object MoriSpace {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Page = 32.dp
    val Touch = 64.dp
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
    titleSmall = type(17, 24, FontWeight.SemiBold),
    bodyLarge = type(18, 28),
    bodyMedium = type(16, 24),
    bodySmall = type(14, 21),
    labelLarge = type(16, 24, FontWeight.SemiBold),
    labelMedium = type(14, 20, FontWeight.Medium),
    labelSmall = type(12, 18, FontWeight.Medium),
)

@Composable
fun MoriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MoriColors.Primary,
            onPrimary = Color.White,
            background = MoriColors.Canvas,
            onBackground = MoriColors.Ink,
            surface = MoriColors.Surface,
            onSurface = MoriColors.Ink,
            onSurfaceVariant = MoriColors.Secondary,
            outline = MoriColors.Outline,
        ),
        typography = MoriTypography,
        content = content,
    )
}
