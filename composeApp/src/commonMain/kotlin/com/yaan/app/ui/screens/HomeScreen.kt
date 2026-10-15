package com.yaan.app.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.components.YaanCard
import com.yaan.app.ui.components.YaanChip
import com.yaan.app.ui.components.YaanGhostButton
import com.yaan.app.ui.components.YaanPrimaryButton
import androidx.compose.runtime.LaunchedEffect
import com.yaan.app.ui.components.YaanQuickCaptureDock
import com.yaan.app.ui.components.YaanSecondaryButton
import com.yaan.app.ui.state.HomeUiAction
import com.yaan.app.ui.state.HomeUiState
import com.yaan.app.ui.state.TaskItemUi
import com.yaan.app.ui.theme.YaanTheme
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.repository.BriefingRepository
import com.yaan.core.domain.repository.TaskRepository
import com.yaan.core.domain.usecase.CaptureIntentUseCase
import kotlinx.coroutines.launch

/**
 * Stateful Container Composable for HomeScreen.
 *
 * Coordinates business logic, reactive state holders, and use case dispatching.
 * Decoupled from pure presentation layout.
 */
@Composable
fun HomeScreen(
    onNavigateToAskYaan: () -> Unit = {},
    onLaunchVoice: () -> Unit = {},
    taskRepository: TaskRepository? = null,
    briefingRepository: BriefingRepository? = null,
    captureUseCase: CaptureIntentUseCase? = null,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(HomeUiState()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(taskRepository) {
        if (taskRepository != null) {
            taskRepository.observeTasks().collect { repoTasks ->
                if (repoTasks.isNotEmpty()) {
                    uiState = uiState.copy(
                        tasks = repoTasks.map { t ->
                            TaskItemUi(
                                id = t.id,
                                title = t.title,
                                dueTime = t.dueAt?.toString() ?: "Today",
                                tag = t.priority.name,
                                isHighPriority = t.priority == TaskPriority.HIGH || t.priority == TaskPriority.URGENT,
                                completed = t.status == TaskStatus.COMPLETED
                            )
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(briefingRepository) {
        if (briefingRepository != null) {
            briefingRepository.observeLatestBriefing().collect { b ->
                if (b != null) {
                    val conf = b.conflicts.firstOrNull()
                    uiState = uiState.copy(
                        briefingHeadline = "Daily Briefing",
                        briefingSummary = b.summaryMarkdown,
                        conflictTitle = conf?.description ?: uiState.conflictTitle,
                        conflictResolution = conf?.proposedResolution ?: uiState.conflictResolution
                    )
                }
            }
        }
    }

    HomeScreenContent(
        state = uiState,
        onAction = { action ->
            when (action) {
                is HomeUiAction.ToggleTask -> {
                    coroutineScope.launch {
                        if (taskRepository != null) {
                            taskRepository.toggleTaskCompleted(action.id)
                        } else {
                            uiState = uiState.copy(
                                tasks = uiState.tasks.map { task ->
                                    if (task.id == action.id) task.copy(completed = !task.completed) else task
                                }
                            )
                        }
                    }
                }

                HomeUiAction.DismissBriefing -> {
                    uiState = uiState.copy(showBriefing = false)
                }

                HomeUiAction.DismissConflict -> {
                    uiState = uiState.copy(showConflict = false)
                }

                HomeUiAction.AdjustConflict -> {
                    uiState = uiState.copy(conflictAdjusted = true)
                }

                is HomeUiAction.SubmitQuickCapture -> {
                    val input = action.input.trim()
                    if (input.isNotEmpty()) {
                        if (captureUseCase != null) {
                            coroutineScope.launch {
                                val result = captureUseCase(input)
                                result.fold(
                                    onSuccess = { msg ->
                                        uiState = uiState.copy(
                                            feedbackMessage = msg,
                                            tasks = uiState.tasks + TaskItemUi(
                                                id = (uiState.tasks.size + 1).toString(),
                                                title = input,
                                                dueTime = "Captured just now",
                                                tag = "Auto-Triaged"
                                            )
                                        )
                                    },
                                    onFailure = { err ->
                                        uiState = uiState.copy(feedbackMessage = "Error: ${err.message}")
                                    }
                                )
                            }
                        } else {
                            uiState = uiState.copy(
                                tasks = uiState.tasks + TaskItemUi(
                                    id = (uiState.tasks.size + 1).toString(),
                                    title = input,
                                    dueTime = "Captured just now",
                                    tag = "Auto-Triaged"
                                )
                            )
                        }
                    }
                }

                HomeUiAction.ClearFeedback -> {
                    uiState = uiState.copy(feedbackMessage = null)
                }

                HomeUiAction.NavigateToAskYaan -> {
                    onNavigateToAskYaan()
                }

                HomeUiAction.LaunchVoice -> {
                    onLaunchVoice()
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Pure Stateless UI Composable for HomeScreen.
 *
 * Implements Executive Kinetic Minimalism design system.
 * Contains ZERO domain repository calls, zero direct state mutations, and zero business logic.
 */
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Executive Editorial Header & Date Stamp ────────────────────────
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THURSDAY, OCT 24 · 09:48 AM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.05.sp
                    )
                )

                YaanChip(
                    text = "Vault Sync Active",
                    leadingDotColor = YaanTheme.extendedColors.bronzeAccent,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = state.briefingHeadline,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
            )
        }

        // ── 2. Daily Briefing Card (0ms Pre-computed Instant Render) ─────────
        if (state.showBriefing) {
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            YaanChip(
                                text = "Daily Briefing",
                                leadingIcon = Icons.Default.Insights,
                                containerColor = YaanTheme.extendedColors.champagneTint,
                                contentColor = YaanTheme.extendedColors.bronzeAccent
                            )

                            Text(
                                text = "Prepared 04:00 AM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        IconButton(
                            onClick = { onAction(HomeUiAction.DismissBriefing) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Briefing",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = state.briefingSummary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(YaanTheme.extendedColors.bronzeAccent)
                            )
                            Text(
                                text = "Cognitive Load: Light",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        YaanGhostButton(
                            text = "Expand Brief",
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { onAction(HomeUiAction.NavigateToAskYaan) }
                        )
                    }
                }
            }
        }

        // ── 3. Explainable Contextual Conflict Banner ────────────────────────
        if (state.showConflict) {
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.champagneTint,
                borderColor = YaanTheme.extendedColors.delicateBorder,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YaanChip(
                            text = if (state.conflictAdjusted) "Rescheduled to 1:15 PM" else "Conflict Detected",
                            leadingIcon = if (state.conflictAdjusted) Icons.Default.Check else Icons.Default.Warning,
                            containerColor = YaanTheme.extendedColors.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Calendar Grounding",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your 2:00 PM Q4 Roadmap Review overlaps with your Dentist commute (25 min). Would you like to shift the prep reminder to 1:15 PM?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Transit micro-anchor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YaanTheme.extendedColors.surfaceContainerLowest.copy(alpha = 0.9f))
                            .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Transit: Downtown Dental Care · Traffic Normal (+4m)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!state.conflictAdjusted) {
                            YaanPrimaryButton(
                                text = "Adjust (1:15 PM)",
                                leadingIcon = Icons.Default.Update,
                                onClick = { onAction(HomeUiAction.AdjustConflict) }
                            )
                            YaanSecondaryButton(
                                text = "Keep As Is",
                                onClick = { onAction(HomeUiAction.DismissConflict) }
                            )
                        } else {
                            Text(
                                text = "Prep alert shifted to 1:15 PM · Calendar synced",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = YaanTheme.extendedColors.bronzeAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Next High-Stakes Commitment Card ──────────────────────────────
        YaanCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UP NEXT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = YaanTheme.extendedColors.bronzeAccent,
                            letterSpacing = 0.05.sp
                        )
                    )

                    YaanChip(
                        text = "Calendar Grounded",
                        containerColor = YaanTheme.extendedColors.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.nextCommitmentTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.nextCommitmentTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location / Link Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(YaanTheme.extendedColors.surfaceContainerLow)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Meeting Link",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.nextCommitmentLocation,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // ── 5. Immediate Priorities Interactive Checklist ───────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S PRIORITIES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.05.sp
                    )
                )

                Text(
                    text = "${state.tasks.count { it.completed }} of ${state.tasks.size} done",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tasks.forEach { task ->
                    YaanCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(HomeUiAction.ToggleTask(task.id)) },
                        backgroundColor = if (task.completed) {
                            YaanTheme.extendedColors.surfaceContainerLowest.copy(alpha = 0.6f)
                        } else {
                            YaanTheme.extendedColors.surfaceContainerLowest
                        },
                        borderColor = if (task.completed) {
                            YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.5f)
                        } else {
                            YaanTheme.extendedColors.delicateBorder
                        },
                        elevation = if (task.completed) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox Circle
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (task.completed) {
                                            YaanTheme.extendedColors.bronzeAccent
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        },
                                        shape = CircleShape
                                    )
                                    .background(
                                        if (task.completed) YaanTheme.extendedColors.bronzeAccent else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.completed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Task Text & Meta
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (task.completed) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = task.dueTime,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    )

                                    if (task.isHighPriority) {
                                        YaanChip(
                                            text = task.tag,
                                            containerColor = YaanTheme.extendedColors.champagneTint,
                                            contentColor = YaanTheme.extendedColors.bronzeAccent
                                        )
                                    } else {
                                        Text(
                                            text = "· ${task.tag}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { /* Options */ },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 6. Prominent Quick Capture Dock ──────────────────────────────────
        YaanQuickCaptureDock(
            onCaptureSubmitted = { query ->
                onAction(HomeUiAction.SubmitQuickCapture(query))
            },
            onVoiceToggle = {
                onAction(HomeUiAction.LaunchVoice)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
