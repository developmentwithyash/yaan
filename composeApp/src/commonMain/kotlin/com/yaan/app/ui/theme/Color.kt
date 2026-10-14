package com.yaan.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ── Light Theme Tokens (Executive Kinetic Minimalism) ────────────────────────
val SurfaceLight = Color(0xFFFDF9F3)
val SurfaceBrightLight = Color(0xFFFDF9F3)
val SurfaceDimLight = Color(0xFFDDD9D4)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F3EE)
val SurfaceContainerLight = Color(0xFFF2EDE8)
val SurfaceContainerHighLight = Color(0xFFECE7E2)
val SurfaceContainerHighestLight = Color(0xFFE6E2DD)
val SandSurfaceLight = Color(0xFFF5F2EB)
val ChampagneTintLight = Color(0xFFFEF9E7)

val OnSurfaceLight = Color(0xFF141311)          // Obsidian Charcoal
val OnSurfaceVariantLight = Color(0xFF4A4845)   // Deep Graphite / Slate

val PrimaryLight = Color(0xFF785600)            // Authoritative Bronze Gold
val PrimaryContainerLight = Color(0xFF986D00)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryContainerLight = Color(0xFFFEF9E7)

val BronzeAccentLight = Color(0xFF966F14)
val AmberGoldLight = Color(0xFFD4AF37)
val SecondaryContainerLight = Color(0xFFF2E0C6)
val OnSecondaryContainerLight = Color(0xFF504532)

val DelicateBorderLight = Color(0xFFDAD4CA)     // Precision Hairline
val OutlineVariantLight = Color(0xFFD3C4AF)
val OutlineLight = Color(0xFF817563)

val StatusSuccessLight = Color(0xFF1E8E3E)
val StatusWarningLight = Color(0xFFE37400)
val StatusErrorLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF93000A)

// ── Dark Theme Tokens ─────────────────────────────────────────────────────────
val SurfaceDark = Color(0xFF141311)
val SurfaceBrightDark = Color(0xFF262421)
val SurfaceDimDark = Color(0xFF0F0E0C)
val SurfaceContainerLowestDark = Color(0xFF1B1A17)
val SurfaceContainerLowDark = Color(0xFF22201D)
val SurfaceContainerDark = Color(0xFF2A2824)
val SurfaceContainerHighDark = Color(0xFF33302B)
val SurfaceContainerHighestDark = Color(0xFF3E3B35)
val SandSurfaceDark = Color(0xFF23211D)
val ChampagneTintDark = Color(0xFF2B261A)

val OnSurfaceDark = Color(0xFFF4F0EB)
val OnSurfaceVariantDark = Color(0xFFA8A298)

val PrimaryDark = Color(0xFFF7BD48)
val PrimaryContainerDark = Color(0xFF6B4D00)
val OnPrimaryDark = Color(0xFF271900)
val OnPrimaryContainerDark = Color(0xFFFFDEA6)

val BronzeAccentDark = Color(0xFFE5B84B)
val AmberGoldDark = Color(0xFFF0CE68)
val SecondaryContainerDark = Color(0xFF3F3523)
val OnSecondaryContainerDark = Color(0xFFD5C4AB)

val DelicateBorderDark = Color(0xFF3A362F)
val OutlineVariantDark = Color(0xFF4C473E)
val OutlineDark = Color(0xFF8A8275)

val StatusSuccessDark = Color(0xFF4EDE80)
val StatusWarningDark = Color(0xFFFBBF24)
val StatusErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SecondaryFixedDimLight = Color(0xFFD5C4AB)
val SecondaryFixedDimDark = Color(0xFF504532)

/**
 * Extended custom tokens beyond standard Material 3
 */
@Immutable
data class YaanExtendedColors(
    val sandSurface: Color,
    val champagneTint: Color,
    val bronzeAccent: Color,
    val amberGold: Color,
    val delicateBorder: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val secondaryContainer: Color,
    val secondaryFixedDim: Color,
)
