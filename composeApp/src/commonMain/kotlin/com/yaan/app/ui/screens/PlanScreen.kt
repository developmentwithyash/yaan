package com.yaan.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.components.YaanCard
import com.yaan.app.ui.theme.YaanTheme
import com.yaan.app.ui.state.PlanUiAction
import com.yaan.app.ui.state.PlanUiState

/**
 * Stateful Container Composable for PlanScreen.
 * Coordinates calendar selections, fast capture text, task completion states, and active routines.
 */
@Composable
fun PlanScreen(
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(PlanUiState()) }

    PlanScreenContent(
        state = uiState,
        onAction = { action ->
            when (action) {
                is PlanUiAction.SelectTab -> {
                    uiState = uiState.copy(selectedTab = action.tab)
                }
                is PlanUiAction.SelectDay -> {
                    uiState = uiState.copy(selectedDay = action.day)
                }
                is PlanUiAction.UpdateFastAction -> {
                    uiState = uiState.copy(fastActionText = action.text)
                }
                is PlanUiAction.ToggleTask -> {
                    uiState = when (action.taskKey) {
                        "dueNext" -> uiState.copy(isDueNextDone = !uiState.isDueNextDone)
                        "task1" -> uiState.copy(task1Done = !uiState.task1Done)
                        "task2" -> uiState.copy(task2Done = !uiState.task2Done)
                        "task3" -> uiState.copy(task3Done = !uiState.task3Done)
                        "stream1" -> uiState.copy(stream1Done = !uiState.stream1Done)
                        "stream2" -> uiState.copy(stream2Done = !uiState.stream2Done)
                        "stream3" -> uiState.copy(stream3Done = !uiState.stream3Done)
                        else -> uiState
                    }
                }
                PlanUiAction.StartRoutine -> {
                    uiState = uiState.copy(isRoutineRunning = !uiState.isRoutineRunning)
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Pure Stateless UI Composable for PlanScreen.
 * Contains ZERO business logic, state mutation, or side-effects.
 */
@Composable
fun PlanScreenContent(
    state: PlanUiState,
    onAction: (PlanUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Segmented View Controller Tabs ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YaanTheme.extendedColors.surfaceContainer)
                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                state.availableTabs.forEach { tab ->
                    val isSelected = state.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(YaanTheme.extendedColors.surfaceContainerLowest)
                                        .shadow(1.dp, RoundedCornerShape(8.dp))
                                        .border(
                                            0.5.dp,
                                            YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.6f),
                                            RoundedCornerShape(8.dp)
                                        )
                                } else Modifier
                            )
                            .clickable { onAction(PlanUiAction.SelectTab(tab)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // ── 2. Date Scrubber Row ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YaanTheme.extendedColors.surfaceContainerLowest)
                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val days = listOf(
                    Triple("M", 21, 1),
                    Triple("T", 22, 1),
                    Triple("W", 23, 2),
                    Triple("Thu", 24, 3),
                    Triple("F", 25, 1),
                    Triple("S", 26, 0),
                    Triple("S", 27, 1)
                )

                days.forEach { (weekday, dayNum, dotCount) ->
                    val isSelected = dayNum == state.selectedDay
                    if (isSelected) {
                        // Active Day Card (Golden Bronze Pill)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onAction(PlanUiAction.SelectDay(dayNum)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = weekday,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = YaanTheme.extendedColors.champagneTint,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(dotCount) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(YaanTheme.extendedColors.champagneTint)
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard Day
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAction(PlanUiAction.SelectDay(dayNum)) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = weekday,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (dotCount > 0) {
                                    repeat(dotCount) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(YaanTheme.extendedColors.secondaryFixedDim)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. Reminder Health & Diagnostics Banner ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YaanTheme.extendedColors.surfaceContainerLow)
                .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .alpha(pulseAlpha)
                            .background(Color(0xFF059669))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Notification Active · 5 primed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "Diagnostics",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Diagnostics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // ── 4. Due Next Section ──────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DUE NEXT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.05.sp,
                        fontSize = 11.sp
                    )
                )

                // Urgent Due Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFDAD6))
                        .border(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFBA1A1A))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "DUE IN 25 MIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBA1A1A),
                                fontSize = 10.sp,
                                letterSpacing = 0.02.sp
                            )
                        )
                    }
                }
            }

            // Active Commitment Card
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "10:30 AM — 11:15 AM",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Product Architecture Review",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    textDecoration = if (state.isDueNextDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Focus: Mobile SQLite local storage schema and event dispatch loop",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(YaanTheme.extendedColors.surfaceContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 4 Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Done Button (Gold)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.isDueNextDone) YaanTheme.extendedColors.surfaceContainerHigh
                                    else MaterialTheme.colorScheme.primary
                                )
                                .clickable { onAction(PlanUiAction.ToggleTask("dueNext")) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (state.isDueNextDone) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (state.isDueNextDone) "Completed" else "Done",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (state.isDueNextDone) MaterialTheme.colorScheme.primary else Color.White,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // +10m Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YaanTheme.extendedColors.sandSurface)
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(8.dp))
                                .clickable { }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+10m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Shift Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YaanTheme.extendedColors.sandSurface)
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(8.dp))
                                .clickable { }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Shift",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Briefing Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YaanTheme.extendedColors.sandSurface)
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(8.dp))
                                .clickable { }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Briefing",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 5. Later Today Section ───────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LATER TODAY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.05.sp,
                        fontSize = 11.sp
                    )
                )
                val scheduledCount = listOf(!state.task1Done, !state.task2Done, !state.task3Done).count { it }
                Text(
                    text = "$scheduledCount scheduled",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            // Commitment 1: Client Onboarding Call
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(PlanUiAction.ToggleTask("task1")) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Rounded square checkbox
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (state.task1Done) MaterialTheme.colorScheme.primary
                                    else YaanTheme.extendedColors.surfaceContainer
                                )
                                .border(
                                    1.dp,
                                    if (state.task1Done) MaterialTheme.colorScheme.primary
                                    else YaanTheme.extendedColors.delicateBorder,
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.task1Done) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "2:00 PM – 2:45 PM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(YaanTheme.extendedColors.secondaryContainer)
                                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Weekly",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Client Onboarding Call with Acme Health",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    textDecoration = if (state.task1Done) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                            Text(
                                text = "Lead stakeholder demo with Dr. Vance",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = { onAction(PlanUiAction.ToggleTask("task1")) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Meeting",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Commitment 2: Review PR #418
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(PlanUiAction.ToggleTask("task2")) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (state.task2Done) MaterialTheme.colorScheme.primary
                                    else YaanTheme.extendedColors.surfaceContainer
                                )
                                .border(
                                    1.dp,
                                    if (state.task2Done) MaterialTheme.colorScheme.primary
                                    else YaanTheme.extendedColors.delicateBorder,
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.task2Done) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "4:00 PM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(YaanTheme.extendedColors.sandSurface)
                                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Security Checklist v2",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Review PR #418 – Auth Token Rotation",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    textDecoration = if (state.task2Done) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                            Text(
                                text = "Verify ephemeral key lifecycle and auto-revocation logic",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = { onAction(PlanUiAction.ToggleTask("task2")) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Commitment 3: Geofence Pharmacy
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(PlanUiAction.ToggleTask("task3")) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (state.task3Done) MaterialTheme.colorScheme.primary
                                    else YaanTheme.extendedColors.surfaceContainer
                                )
                                .border(
                                    1.dp,
                                    if (state.task3Done) MaterialTheme.colorScheme.primary
                                    else YaanTheme.extendedColors.delicateBorder,
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.task3Done) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "6:15 PM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(YaanTheme.extendedColors.champagneTint)
                                        .border(1.dp, YaanTheme.extendedColors.amberGold.copy(alpha = 0.3f), CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = YaanTheme.extendedColors.bronzeAccent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Geofence Ready",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = YaanTheme.extendedColors.bronzeAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Pick up prescription from Pharmacy",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    textDecoration = if (state.task3Done) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                            Text(
                                text = "Grand Ave Health Mart · Trigger at 300m perimeter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = { onAction(PlanUiAction.ToggleTask("task3")) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = "Location",
                            tint = YaanTheme.extendedColors.bronzeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── 6. Section 3: Lightweight Projects & Routines ────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE STREAMS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.05.sp,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "Manage All",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.clickable { }
                )
            }

            // Project Accordion Card
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val streamDoneCount = (if (state.stream1Done) 1 else 0) + (if (state.stream2Done) 1 else 0) + (if (state.stream3Done) 1 else 0) + 2

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YaanTheme.extendedColors.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Project: Launch YAAN Beta",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(YaanTheme.extendedColors.champagneTint)
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$streamDoneCount/5 done",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(YaanTheme.extendedColors.surfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(streamDoneCount / 5f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Nested Checklist Items
                    Column(
                        modifier = Modifier.padding(start = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // Task 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(PlanUiAction.ToggleTask("stream1")) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (state.stream1Done) MaterialTheme.colorScheme.primary
                                        else YaanTheme.extendedColors.sandSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (state.stream1Done) MaterialTheme.colorScheme.primary
                                        else YaanTheme.extendedColors.delicateBorder,
                                        RoundedCornerShape(3.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.stream1Done) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finalize mobile UI system & tokens",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (state.stream1Done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (state.stream1Done) TextDecoration.LineThrough else TextDecoration.None,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Task 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(PlanUiAction.ToggleTask("stream2")) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (state.stream2Done) MaterialTheme.colorScheme.primary
                                        else YaanTheme.extendedColors.sandSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (state.stream2Done) MaterialTheme.colorScheme.primary
                                        else YaanTheme.extendedColors.delicateBorder,
                                        RoundedCornerShape(3.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.stream2Done) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test notification background wake reliability on Android",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (state.stream2Done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (state.stream2Done) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = if (state.stream2Done) FontWeight.Normal else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Task 3
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(PlanUiAction.ToggleTask("stream3")) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (state.stream3Done) MaterialTheme.colorScheme.primary
                                        else YaanTheme.extendedColors.sandSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (state.stream3Done) MaterialTheme.colorScheme.primary
                                        else YaanTheme.extendedColors.delicateBorder,
                                        RoundedCornerShape(3.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.stream3Done) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deploy staging build & invite batch 1 alpha users",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (state.stream3Done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (state.stream3Done) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = if (state.stream3Done) FontWeight.Normal else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Routine Card
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.isRoutineRunning) YaanTheme.extendedColors.champagneTint
                                    else YaanTheme.extendedColors.sandSurface
                                )
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (state.isRoutineRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Evening Shutdown Routine",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = if (state.isRoutineRunning) "● Active: Logging daylight hours & syncing vault..." else "6:45 PM daily · Archive inbox & log daylight hours",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (state.isRoutineRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { onAction(PlanUiAction.StartRoutine) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isRoutineRunning) MaterialTheme.colorScheme.primary
                                else YaanTheme.extendedColors.sandSurface
                            )
                            .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (state.isRoutineRunning) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = "Run Routine",
                            tint = if (state.isRoutineRunning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ── 7. Fast Action Prompt Input ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YaanTheme.extendedColors.surfaceContainerLowest)
                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (state.fastActionText.isEmpty()) {
                        Text(
                            text = "Try \"Submit tax draft tomorrow 9am\"...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }
                    BasicTextField(
                        value = state.fastActionText,
                        onValueChange = { onAction(PlanUiAction.UpdateFastAction(it)) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
