package com.example.smartdialer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.smartdialer.data.preferences.AccentColorOption
import com.example.smartdialer.data.preferences.FontSizeScale
import com.example.smartdialer.data.preferences.ThemeMode

@Immutable
data class DialerExtendedColors(
    val callAccept: Color,
    val callMissed: Color,
    val callIncoming: Color,
    val callOutgoing: Color,
    val starGold: Color,
    val fontScale: Float
)

val LocalDialerExtendedColors = staticCompositionLocalOf {
    DialerExtendedColors(
        callAccept = CallAcceptGreen,
        callMissed = CallMissedRed,
        callIncoming = CallIncomingGreen,
        callOutgoing = CallOutgoingBlue,
        starGold = StarGold,
        fontScale = 1.0f
    )
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SmartDialerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColorOption = AccentColorOption.OCEAN_BLUE,
    fontSizeScale: FontSizeScale = FontSizeScale.NORMAL,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        accentColor == AccentColorOption.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) {
                dynamicDarkColorScheme(context).copy(
                    background = AmoledBackground,
                    surface = AmoledSurface,
                    surfaceContainer = AmoledSurfaceContainer
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        useDarkTheme -> buildAmoledDarkColorScheme(accentColor)
        else -> buildLightColorScheme(accentColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    val extendedColors = DialerExtendedColors(
        callAccept = if (useDarkTheme) CallAcceptGreenDark else CallAcceptGreen,
        callMissed = CallMissedRed,
        callIncoming = CallIncomingGreen,
        callOutgoing = CallOutgoingBlue,
        starGold = StarGold,
        fontScale = fontSizeScale.multiplier
    )

    CompositionLocalProvider(LocalDialerExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = createAppTypography(fontSizeScale.multiplier),
            shapes = AppShapes,
            content = content
        )
    }
}

private fun buildAmoledDarkColorScheme(accent: AccentColorOption): ColorScheme {
    val (primary, primaryContainer, onPrimaryContainer) = when (accent) {
        AccentColorOption.OCEAN_BLUE, AccentColorOption.DYNAMIC -> Triple(
            OceanBluePrimaryDark,
            OceanBlueContainerDark,
            OceanBlueContainer
        )
        AccentColorOption.EMERALD_GREEN -> Triple(
            EmeraldPrimaryDark,
            EmeraldContainerDark,
            EmeraldContainer
        )
        AccentColorOption.ROYAL_PURPLE -> Triple(
            RoyalPurplePrimaryDark,
            RoyalPurpleContainerDark,
            RoyalPurpleContainer
        )
        AccentColorOption.AMBER_ORANGE -> Triple(
            AmberGoldPrimaryDark,
            AmberGoldContainerDark,
            AmberGoldContainer
        )
    }
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF061326),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = Color(0xFF061326),
        secondaryContainer = AmoledSurfaceVariant,
        onSecondaryContainer = AmoledOnSurface,
        background = AmoledBackground,
        onBackground = AmoledOnBackground,
        surface = AmoledSurface,
        onSurface = AmoledOnSurface,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = AmoledOnSurfaceVariant,
        surfaceContainerLowest = AmoledBackground,
        surfaceContainerLow = AmoledSurface,
        surfaceContainer = AmoledSurfaceContainer,
        surfaceContainerHigh = AmoledSurfaceVariant,
        surfaceContainerHighest = Color(0xFF393634),
        outline = AmoledOutline,
        outlineVariant = Color(0xFF33302E),
        error = CallMissedRed,
        onError = Color.White
    )
}

private fun buildLightColorScheme(accent: AccentColorOption): ColorScheme {
    val (primary, primaryContainer, onPrimaryContainer) = when (accent) {
        AccentColorOption.OCEAN_BLUE, AccentColorOption.DYNAMIC -> Triple(
            OceanBluePrimary,
            OceanBlueContainer,
            Color(0xFF092A5E)
        )
        AccentColorOption.EMERALD_GREEN -> Triple(
            EmeraldPrimary,
            EmeraldContainer,
            Color(0xFF05331E)
        )
        AccentColorOption.ROYAL_PURPLE -> Triple(
            RoyalPurplePrimary,
            RoyalPurpleContainer,
            Color(0xFF21005D)
        )
        AccentColorOption.AMBER_ORANGE -> Triple(
            AmberGoldPrimary,
            AmberGoldContainer,
            Color(0xFF422204)
        )
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = Color.White,
        secondaryContainer = LightSurfaceVariant,
        onSecondaryContainer = LightOnSurface,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceContainerLowest = LightSurface,
        surfaceContainerLow = LightSurfaceContainer,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceVariant,
        surfaceContainerHighest = Color(0xFFE3DCEB),
        outline = LightOutline,
        outlineVariant = Color(0xFFD9D3E0),
        error = CallMissedRed,
        onError = Color.White
    )
}
