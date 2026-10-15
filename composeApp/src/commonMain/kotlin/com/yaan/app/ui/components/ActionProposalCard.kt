package com.yaan.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.state.ProposedActionUi
import com.yaan.app.ui.theme.YaanTheme

/**
 * Interactive Action Proposal Card (Critical User Journey 7, Finding F-004).
 *
 * Enforces the Two-Phase Action Commitment Invariant:
 * 1. Stage 1 (Staged): Requires affirmative user confirmation before modifying local vault state.
 * 2. Stage 2 (Committed): Provides clear confirmation and reversible Undo rollback.
 */
@Composable
fun ActionProposalCard(
    proposal: ProposedActionUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onUndo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (proposal.isDismissed) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismissed",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Action proposal dismissed. No vault state was modified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (proposal.isUndone) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undone",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Action reverted. Vault restored to prior state.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        return
    }

    YaanCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = if (proposal.isConfirmed)
            YaanTheme.extendedColors.surfaceContainerLowest
        else
            YaanTheme.extendedColors.champagneTint.copy(alpha = 0.45f),
        borderColor = if (proposal.isConfirmed)
            YaanTheme.extendedColors.statusSuccess.copy(alpha = 0.4f)
        else
            YaanTheme.extendedColors.bronzeAccent.copy(alpha = 0.4f),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header Status Row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (proposal.isConfirmed) YaanTheme.extendedColors.statusSuccess
                                else YaanTheme.extendedColors.bronzeAccent
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (proposal.isConfirmed) "ACTION COMMITTED" else "ACTION PROPOSAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = if (proposal.isConfirmed) YaanTheme.extendedColors.statusSuccess
                        else YaanTheme.extendedColors.bronzeAccent
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (proposal.isConfirmed)
                        YaanTheme.extendedColors.statusSuccess.copy(alpha = 0.12f)
                    else
                        YaanTheme.extendedColors.bronzeAccent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (proposal.isConfirmed) "Vault Updated" else "Requires Confirmation",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (proposal.isConfirmed) YaanTheme.extendedColors.statusSuccess
                        else YaanTheme.extendedColors.bronzeAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // ── Proposal Content ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = proposal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = proposal.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // ── Two-Phase Affirmative Action Buttons ──────────────────────────
            if (!proposal.isConfirmed) {
                // Invariant Disclaimer
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = YaanTheme.extendedColors.bronzeAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Affirmative approval required before local vault is modified.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    YaanPrimaryButton(
                        text = "Confirm & Execute",
                        leadingIcon = Icons.Default.AlarmAdd,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.3f)
                    )
                    YaanSecondaryButton(
                        text = "Dismiss",
                        leadingIcon = Icons.Default.Close,
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            } else {
                // Confirmed State with Reversible Undo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Committed",
                            tint = YaanTheme.extendedColors.statusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deterministic State Persisted",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = YaanTheme.extendedColors.statusSuccess
                        )
                    }

                    if (onUndo != null) {
                        YaanGhostButton(
                            text = "Undo Action",
                            trailingIcon = Icons.AutoMirrored.Filled.Undo,
                            onClick = onUndo
                        )
                    }
                }
            }
        }
    }
}
