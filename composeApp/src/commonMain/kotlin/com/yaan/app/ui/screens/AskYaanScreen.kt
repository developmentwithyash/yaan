package com.yaan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.components.YaanCard
import com.yaan.app.ui.components.YaanChip
import com.yaan.app.ui.components.YaanPrimaryButton
import com.yaan.app.ui.components.YaanSecondaryButton
import com.yaan.app.ui.theme.YaanTheme

import androidx.compose.runtime.rememberCoroutineScope
import com.yaan.app.ui.state.AskYaanUiAction
import com.yaan.app.ui.state.AskYaanUiState
import com.yaan.app.ui.state.ProposedActionUi
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.repository.MemoryRepository
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.TaskRepository
import kotlinx.coroutines.launch

/**
 * Stateful Container Composable for AskYaanScreen.
 * Coordinates input text, chat/voice modes, thought process disclosures, and action proposals.
 */
@Composable
fun AskYaanScreen(
    onLaunchVoice: () -> Unit = {},
    noteRepository: NoteRepository? = null,
    taskRepository: TaskRepository? = null,
    memoryRepository: MemoryRepository? = null,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(AskYaanUiState()) }
    val coroutineScope = rememberCoroutineScope()

    AskYaanScreenContent(
        state = uiState,
        onAction = { action ->
            when (action) {
                is AskYaanUiAction.UpdateInput -> {
                    uiState = uiState.copy(inputText = action.text)
                }
                is AskYaanUiAction.SwitchMode -> {
                    uiState = uiState.copy(activeMode = action.mode)
                    if (action.mode == "Voice") {
                        onLaunchVoice()
                    }
                }
                AskYaanUiAction.ToggleThoughtProcess -> {
                    uiState = uiState.copy(thoughtExpanded = !uiState.thoughtExpanded)
                }
                AskYaanUiAction.ConfirmActionProposal -> {
                    coroutineScope.launch {
                        val prop = uiState.proposedAction
                        if (prop != null && !prop.isConfirmed && taskRepository != null) {
                            taskRepository.upsertTask(
                                Task(
                                    id = "task_committed_${prop.id}",
                                    title = prop.title.removePrefix("Proposed: ").removePrefix("Action: "),
                                    priority = TaskPriority.HIGH
                                )
                            )
                        }
                        uiState = uiState.copy(
                            proposedAction = uiState.proposedAction?.copy(isConfirmed = true),
                            responseMarkdown = "Action confirmed and committed to local vault. State updated."
                        )
                    }
                }
                AskYaanUiAction.SubmitQuery -> {
                    val q = uiState.inputText.trim()
                    if (q.isNotEmpty()) {
                        val lower = q.lowercase()
                        val isActionIntent = lower.startsWith("reschedule") ||
                                lower.startsWith("shift") ||
                                lower.startsWith("create task") ||
                                lower.startsWith("add task") ||
                                lower.startsWith("new task") ||
                                lower.startsWith("remind me") ||
                                lower.startsWith("postpone")

                        if (isActionIntent) {
                            val proposal = ProposedActionUi(
                                id = "action_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                                title = "Proposed: $q",
                                subtitle = "Grounded on user command · Requires affirmative tap to commit",
                                actionType = "EXECUTIVE_ACTION",
                                isConfirmed = false
                            )
                            uiState = uiState.copy(
                                inputText = "",
                                proposedAction = proposal,
                                responseMarkdown = "I've staged this action proposal for your review. No data has been modified. Please confirm below to commit."
                            )
                        } else {
                            coroutineScope.launch {
                                if (noteRepository != null) {
                                    val notesRes = noteRepository.searchNotes(q)
                                    val matchedNotes = notesRes.getOrDefault(emptyList())

                                    if (matchedNotes.isNotEmpty()) {
                                        val top = matchedNotes.first()
                                        uiState = uiState.copy(
                                            inputText = "",
                                            responseMarkdown = "Based on your encrypted vault notes:\n\n${top.contentCiphertext}\n\n[Citation: ${top.title} (ID: ${top.id})]"
                                        )
                                    } else {
                                        // Honest abstention
                                        uiState = uiState.copy(
                                            inputText = "",
                                            responseMarkdown = "I could not find information about \"$q\" in your vault. I do not guess or hallucinate facts that are not verified in your personal notes."
                                        )
                                    }
                                } else {
                                    uiState = uiState.copy(
                                        inputText = "",
                                        responseMarkdown = "Responding to: \"$q\" using on-device epistemic search..."
                                    )
                                }
                            }
                        }
                    }
                }
                AskYaanUiAction.LaunchVoice -> {
                    onLaunchVoice()
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Pure Stateless UI Composable for AskYaanScreen.
 * Contains ZERO business logic and zero mutation side-effects.
 */
@Composable
fun AskYaanScreenContent(
    state: AskYaanUiState,
    onAction: (AskYaanUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionScheduled = state.proposedAction?.isConfirmed == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Mode Switcher (Chat vs Voice) ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YaanTheme.extendedColors.surfaceContainerLow)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (state.activeMode == "Chat") MaterialTheme.colorScheme.surface
                                else YaanTheme.extendedColors.surfaceContainerLow
                            )
                            .clickable { onAction(AskYaanUiAction.SwitchMode("Chat")) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chat Stream",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (state.activeMode == "Chat") FontWeight.Bold else FontWeight.Medium,
                                color = if (state.activeMode == "Chat") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (state.activeMode == "Voice") MaterialTheme.colorScheme.surface
                                else YaanTheme.extendedColors.surfaceContainerLow
                            )
                            .clickable {
                                onAction(AskYaanUiAction.SwitchMode("Voice"))
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = YaanTheme.extendedColors.bronzeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Live Voice",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (state.activeMode == "Voice") FontWeight.Bold else FontWeight.Medium,
                                    color = if (state.activeMode == "Voice") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // ── Zero-Cloud Memory & Reasoner Badges ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YaanTheme.extendedColors.champagneTint)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = YaanTheme.extendedColors.bronzeAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "On-Device Vault · Grounded Citations",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(YaanTheme.extendedColors.sandSurface)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(YaanTheme.extendedColors.bronzeAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ask YAAN Vault Assistant",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = " · Context: 48 Notes ...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }

            // ── User Query Prompt Bubble ──────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                        .background(YaanTheme.extendedColors.sandSurface)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Draft an executive summary of my Q4 project roadmap for David, incorporating our discussion from yesterday's voice memo, and prepare a reminder to send it before 5 PM.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                ) {
                    Text(
                        text = "10:42 AM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = YaanTheme.extendedColors.bronzeAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // ── Thinking Process Disclosure Accordion ─────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(YaanTheme.extendedColors.surfaceContainerLow)
                        .clickable { onAction(AskYaanUiAction.ToggleThoughtProcess) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = YaanTheme.extendedColors.bronzeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thinking Process · Scanned 3 vault sources · 48ms",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Icon(
                        imageVector = if (state.thoughtExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = state.thoughtExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(YaanTheme.extendedColors.sandSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "• Scanned local vector store: Found 'Voice Memo: Onboarding Flow' recorded yesterday 8:15 AM.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "• Verified deliverables: Extracted SOC2 readiness milestone, Core Rust engine migration, and Tier-1 Beta rollout.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "• Calendar inspection: Cross-checked David Vance's schedule; identified no clashes prior to 5:00 PM cutoff.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // ── AI Editorial Dispatch Card ────────────────────────────────────
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header Tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YaanTheme.extendedColors.champagneTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = YaanTheme.extendedColors.bronzeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXECUTIVE DISPATCH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.05.sp
                            )
                        )
                    }

                    Text(
                        text = "Here is the executive brief ready for David Vance, calibrated to your latest operational priorities:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    )

                    // Highlights Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(YaanTheme.extendedColors.surfaceContainerLow)
                            .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Q4 Strategic Milestones",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            YaanChip(
                                text = "Confidence: High",
                                containerColor = YaanTheme.extendedColors.champagneTint,
                                contentColor = YaanTheme.extendedColors.bronzeAccent
                            )
                        }

                        MilestoneRow(
                            title = "Core Architecture Modernization",
                            desc = "Final cutover to Rust micro-kernels scheduled for Nov 14. Performance gains expected to trim 40% memory latency."
                        )
                        MilestoneRow(
                            title = "Tier-1 Beta Rollout",
                            desc = "Phased cohort onboarding begins Dec 02 with 25 executive accounts as approved in yesterday's memo."
                        )
                        MilestoneRow(
                            title = "SOC2 Type II Attestation",
                            desc = "Evidence collection concludes Dec 18; audit window cleared with no architectural blockers."
                        )
                    }

                    // Grounded Citations
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Grounded Vault Sources",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CitationCard(
                                title = "Voice Memo: Onboarding",
                                meta = "Yesterday · 8:15 AM · 4m 12s",
                                icon = Icons.Default.Mic
                            )
                            CitationCard(
                                title = "Note: Founders Sync",
                                meta = "Oct 24 · 12 Key Points",
                                icon = Icons.Default.EditNote
                            )
                        }
                    }

                    // Proposed Executive Action Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(YaanTheme.extendedColors.sandSurface)
                            .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(YaanTheme.extendedColors.bronzeAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Proposed Executive Action",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Text(
                                text = if (actionScheduled) "Scheduled" else "Ready for Confirmation",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (actionScheduled) YaanTheme.extendedColors.statusSuccess else YaanTheme.extendedColors.bronzeAccent
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Target Recipient",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = "David Vance (VP Ops)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Scheduled Trigger",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = "Today at 4:30 PM",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            YaanPrimaryButton(
                                text = if (actionScheduled) "Scheduled (4:30 PM)" else "Schedule & Send",
                                leadingIcon = if (actionScheduled) Icons.Default.CheckCircle else Icons.Default.AlarmAdd,
                                onClick = { onAction(AskYaanUiAction.ConfirmActionProposal) },
                                modifier = Modifier.weight(1f)
                            )
                            YaanSecondaryButton(
                                text = "Modify",
                                leadingIcon = Icons.Default.Tune,
                                onClick = {}
                            )
                        }
                    }

                    // Feedback Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.BookmarkAdd, "Save", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Share, "Share", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            text = "Local inference · 48ms",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // ── Contextual Accelerator Suggestion Pills ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                YaanChip(
                    text = "Show pricing decision",
                    leadingIcon = Icons.Default.AttachMoney,
                    onClick = { onAction(AskYaanUiAction.UpdateInput("Show pricing decision")) }
                )
                YaanChip(
                    text = "Summarize weekly workload",
                    leadingIcon = Icons.Default.Analytics,
                    onClick = { onAction(AskYaanUiAction.UpdateInput("Summarize weekly workload")) }
                )
                YaanChip(
                    text = "Check David's free slots",
                    leadingIcon = Icons.Default.EventAvailable,
                    onClick = { onAction(AskYaanUiAction.UpdateInput("Check David's free slots")) }
                )
            }
        }

        // ── Signature Floating Pill Composer Dock ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(YaanTheme.extendedColors.surfaceContainerLowest)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    if (state.inputText.isEmpty()) {
                        Text(
                            text = "Ask YAAN anything or give an action...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    BasicTextField(
                        value = state.inputText,
                        onValueChange = { onAction(AskYaanUiAction.UpdateInput(it)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Waveform / Voice Launcher Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(YaanTheme.extendedColors.champagneTint)
                        .clickable { onAction(AskYaanUiAction.LaunchVoice) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Live Voice",
                        tint = YaanTheme.extendedColors.bronzeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Voice",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = YaanTheme.extendedColors.bronzeAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onAction(AskYaanUiAction.SubmitQuery) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = null,
            tint = YaanTheme.extendedColors.bronzeAccent,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append("$title: ")
                }
                append(desc)
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun CitationCard(
    title: String,
    meta: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(YaanTheme.extendedColors.sandSurface)
            .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = YaanTheme.extendedColors.bronzeAccent,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
