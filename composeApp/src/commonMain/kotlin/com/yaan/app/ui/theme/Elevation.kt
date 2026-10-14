package com.yaan.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Surface Elevation Tokens for Executive Kinetic Minimalism
 * Maps directly to Stitch MCP elevation specifications (Level 0 through Level 5)
 */
@Immutable
data class ElevationLevel(
    val elevationDp: Dp,
    val containerColorLight: Color,
    val containerColorDark: Color,
    val borderColorLight: Color,
    val borderColorDark: Color,
)

@Immutable
data class YaanElevation(
    /** Level 0: Canvas Base (#FDF9F3 / #141311) */
    val level0: ElevationLevel = ElevationLevel(
        elevationDp = 0.dp,
        containerColorLight = SurfaceLight,
        containerColorDark = SurfaceDark,
        borderColorLight = Color.Transparent,
        borderColorDark = Color.Transparent,
    ),
    /** Level 1: Lowest / Card Base (#FFFFFF / #1B1A17) with Hairline Border */
    val level1: ElevationLevel = ElevationLevel(
        elevationDp = 1.dp,
        containerColorLight = SurfaceContainerLowestLight,
        containerColorDark = SurfaceContainerLowestDark,
        borderColorLight = DelicateBorderLight,
        borderColorDark = DelicateBorderDark,
    ),
    /** Level 2: Low / Inset Container (#F7F3EE / #22201D) */
    val level2: ElevationLevel = ElevationLevel(
        elevationDp = 2.dp,
        containerColorLight = SurfaceContainerLowLight,
        containerColorDark = SurfaceContainerLowDark,
        borderColorLight = DelicateBorderLight,
        borderColorDark = DelicateBorderDark,
    ),
    /** Level 3: Standard Container / Controls (#F2EDE8 / #2A2824) */
    val level3: ElevationLevel = ElevationLevel(
        elevationDp = 4.dp,
        containerColorLight = SurfaceContainerLight,
        containerColorDark = SurfaceContainerDark,
        borderColorLight = DelicateBorderLight,
        borderColorDark = DelicateBorderDark,
    ),
    /** Level 4: High Surface / Popovers (#ECE7E2 / #33302B) */
    val level4: ElevationLevel = ElevationLevel(
        elevationDp = 8.dp,
        containerColorLight = SurfaceContainerHighLight,
        containerColorDark = SurfaceContainerHighDark,
        borderColorLight = OutlineVariantLight,
        borderColorDark = OutlineVariantDark,
    ),
    /** Level 5: Highest Surface / Floating Modals (#E6E2DD / #3E3B35) */
    val level5: ElevationLevel = ElevationLevel(
        elevationDp = 12.dp,
        containerColorLight = SurfaceContainerHighestLight,
        containerColorDark = SurfaceContainerHighestDark,
        borderColorLight = OutlineLight,
        borderColorDark = OutlineDark,
    ),
)
