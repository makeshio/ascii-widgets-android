package com.makeshio.asciiwidgets.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палитра терминала: почти чёрный фон, мятный акцент и приглушённый серый текст. */
object AsciiColors {
    val Background = Color(0xFF06090B)
    val Surface = Color(0xFF0E1418)
    val SurfaceHigh = Color(0xFF151F25)
    val Outline = Color(0xFF26343B)
    val Accent = Color(0xFF7DF9C0)
    val AccentDim = Color(0xFF2C7158)
    val Text = Color(0xFFE6F0F2)
    val TextMuted = Color(0xFF8FA2AA)
    val Danger = Color(0xFFFF7B72)
}

private val baseTypography = Typography()

private val terminalTypography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp,
    ),
    headlineMedium = baseTypography.headlineMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = baseTypography.titleLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = baseTypography.titleMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = baseTypography.labelLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = FontFamily.Monospace),
    labelSmall = baseTypography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
    ),
)

private val terminalShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AsciiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AsciiColors.Accent,
            onPrimary = AsciiColors.Background,
            primaryContainer = AsciiColors.AccentDim,
            onPrimaryContainer = AsciiColors.Text,
            secondary = AsciiColors.AccentDim,
            onSecondary = AsciiColors.Text,
            background = AsciiColors.Background,
            onBackground = AsciiColors.Text,
            surface = AsciiColors.Surface,
            onSurface = AsciiColors.Text,
            surfaceVariant = AsciiColors.SurfaceHigh,
            onSurfaceVariant = AsciiColors.TextMuted,
            outline = AsciiColors.Outline,
            error = AsciiColors.Danger,
            onError = AsciiColors.Background,
        ),
        typography = terminalTypography,
        shapes = terminalShapes,
        content = content,
    )
}

/** Общий стиль для заголовков разделов. */
@Composable
fun SectionLabel(text: String) {
    androidx.compose.material3.Text(
        text = "// $text",
        style = MaterialTheme.typography.labelLarge.copy(
            color = AsciiColors.Accent,
            letterSpacing = 1.sp,
        ),
    )
}

/** Текст с фиксированным моноширинным начертанием. */
@Composable
fun MonoText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
) {
    androidx.compose.material3.Text(
        text = text,
        style = style.copy(fontFamily = FontFamily.Monospace),
        color = color,
    )
}
