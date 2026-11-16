package com.example.smartdialer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SmartDialerFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

fun createAppTypography(scaleFactor: Float): Typography {
    fun scaled(size: Float) = (size * scaleFactor).sp
    return Typography(
        displaySmall = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(34f),
            lineHeight = scaled(40f),
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(26f),
            lineHeight = scaled(32f),
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(22f),
            lineHeight = scaled(28f)
        ),
        titleLarge = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(20f),
            lineHeight = scaled(26f)
        ),
        titleMedium = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(16f),
            lineHeight = scaled(22f)
        ),
        titleSmall = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(14f),
            lineHeight = scaled(20f)
        ),
        bodyLarge = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(16f),
            lineHeight = scaled(22f)
        ),
        bodyMedium = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(14f),
            lineHeight = scaled(20f)
        ),
        bodySmall = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(12f),
            lineHeight = scaled(16f)
        ),
        labelLarge = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(14f),
            lineHeight = scaled(20f)
        ),
        labelMedium = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(12f),
            lineHeight = scaled(16f)
        ),
        labelSmall = TextStyle(
            fontFamily = SmartDialerFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(11f),
            lineHeight = scaled(14f)
        )
    )
}
