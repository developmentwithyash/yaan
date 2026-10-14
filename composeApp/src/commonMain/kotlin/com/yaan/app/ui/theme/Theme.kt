package com.yaan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = BronzeAccentLight,
    onSecondary = OnPrimaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = AmberGoldLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = PrimaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = StatusErrorLight,
    onError = OnPrimaryLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = BronzeAccentDark,
    onSecondary = OnPrimaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = AmberGoldDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = PrimaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = StatusErrorDark,
    onError = OnPrimaryDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

private val LightExtendedColors = YaanExtendedColors(
    sandSurface = SandSurfaceLight,
    champagneTint = ChampagneTintLight,
    bronzeAccent = BronzeAccentLight,
    amberGold = AmberGoldLight,
    delicateBorder = DelicateBorderLight,
    statusSuccess = StatusSuccessLight,
    statusWarning = StatusWarningLight,
    statusError = StatusErrorLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    secondaryContainer = SecondaryContainerLight,
    secondaryFixedDim = SecondaryFixedDimLight,
)

private val DarkExtendedColors = YaanExtendedColors(
    sandSurface = SandSurfaceDark,
    champagneTint = ChampagneTintDark,
    bronzeAccent = BronzeAccentDark,
    amberGold = AmberGoldDark,
    delicateBorder = DelicateBorderDark,
    statusSuccess = StatusSuccessDark,
    statusWarning = StatusWarningDark,
    statusError = StatusErrorDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    secondaryContainer = SecondaryContainerDark,
    secondaryFixedDim = SecondaryFixedDimDark,
)

val LocalYaanColors = staticCompositionLocalOf { LightExtendedColors }
val LocalYaanSpacing = staticCompositionLocalOf { YaanSpacing() }
val LocalYaanElevation = staticCompositionLocalOf { YaanElevation() }
val LocalYaanMotion = staticCompositionLocalOf { YaanMotion() }

object YaanTheme {
    val extendedColors: YaanExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYaanColors.current

    val spacing: YaanSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalYaanSpacing.current

    val elevation: YaanElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalYaanElevation.current

    val motion: YaanMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalYaanMotion.current
}

@Composable
fun YaanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val spacing = YaanSpacing()
    val elevation = YaanElevation()
    val motion = YaanMotion()

    CompositionLocalProvider(
        LocalYaanColors provides extendedColors,
        LocalYaanSpacing provides spacing,
        LocalYaanElevation provides elevation,
        LocalYaanMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = rememberYaanTypography(),
            shapes = YaanShapes,
            content = content
        )
    }
}
