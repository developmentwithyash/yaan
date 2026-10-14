package com.yaan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.components.YaanMonogram

/**
 * YAAN Brand Presentation & Launch Screen
 *
 * Implements the Stitch master brand screen ("YAANStack Typographic Brand Identity Logo")
 * with the bespoke Architectural YS Monogram centered, supporting real-time switching between
 * Dark Obsidian and Warm Golden Editorial themes.
 */
@Composable
fun BrandSplashScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        )
    )

    val backgroundColor = if (isDarkTheme) Color(0xFF0C0D11) else Color(0xFFFAF9F5)
    val cardBackground = if (isDarkTheme) Color(0xFF14161F) else Color(0xFFFFFFFF)
    val delicateBorder = if (isDarkTheme) Color(0xFF282E40) else Color(0xFFE2E8F0)
    val goldAccent = if (isDarkTheme) Color(0xFFE8C366) else Color(0xFFB88A2E)
    val textColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0C0D11)
    val subtitleColor = if (isDarkTheme) Color(0xFF8E99AC) else Color(0xFF6B7280)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top Bar Controls: Theme Switcher Badge ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Editorial Classification Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(cardBackground)
                    .border(1.dp, delicateBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "BESPOKE TYPOGRAPHY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = goldAccent
                    )
                )
            }

            // Dark / Light Theme Quick Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBackground)
                    .border(1.dp, delicateBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = goldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDarkTheme) "Dark Obsidian" else "Warm Golden",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // ── Centered Brand Logo Showcase (From Stitch Screen Master) ────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Luminous Aura Glow behind Monogram
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Radial ambient glow
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    goldAccent.copy(alpha = 0.14f * auraPulse),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // The Centered Architectural YS Monogram
                YaanMonogram(
                    modifier = Modifier.size(160.dp),
                    isDark = isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Typographic Display Wordmark
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "YAAN",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        letterSpacing = 6.sp,
                        color = textColor
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "STACK",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        letterSpacing = 7.sp,
                        color = goldAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calibrated Kerning Divider Line
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                goldAccent.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Corporate Classification Subtitle
            Text(
                text = "INTELLIGENT SYSTEMS & INFRASTRUCTURE",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    color = subtitleColor
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Autonomous Action Navigator\nZero-Knowledge Local Vault · Sub-150ms Interaction",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = subtitleColor.copy(alpha = 0.85f)
                )
            )
        }

        // ── Bottom Action Button: Enter Workspace ───────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (isDarkTheme) {
                                listOf(Color(0xFFE8C366), Color(0xFFB88A2E))
                            } else {
                                listOf(Color(0xFF785600), Color(0xFF986D00))
                            }
                        )
                    )
                    .clickable(onClick = onEnterApp)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFF0C0D11) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Local Vault",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = if (isDarkTheme) Color(0xFF0C0D11) else Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFF0C0D11) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "100% On-Device Deterministic · AES-GCM-256 + Argon2id",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = subtitleColor.copy(alpha = 0.6f)
                )
            )
        }
    }
}
