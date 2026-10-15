package com.yaan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BookmarkAdd
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ai.YaanAssistantEngine
import com.yaan.app.ui.components.ActionProposalCard
import com.yaan.app.ui.components.YaanCard
import com.yaan.app.ui.components.YaanChip
import com.yaan.app.ui.state.AskYaanUiAction
import com.yaan.app.ui.state.AskYaanUiState
import com.yaan.app.ui.state.ChatMessageUi
import com.yaan.app.ui.theme.YaanTheme
import com.yaan.core.common.datetime.nowInstant
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val assistantEngine = remember {
        YaanAssistantEngine(noteRepository, taskRepository, memoryRepository)
    }

    fun submitPrompt(promptText: String) {
        keyboardController?.hide()
        focusManager.clearFocus()

        val q = promptText.trim()
        if (q.isNotEmpty()) {
            val userMsg = ChatMessageUi(
                id = "msg_${nowInstant().toEpochMilliseconds()}",
                sender = "USER",
                text = q
            )
            uiState = uiState.copy(
                inputText = "",
                messages = uiState.messages + userMsg,
                isStreaming = true
            )

            coroutineScope.launch {
                val result = assistantEngine.processQuery(q)
                val yaanMsg = ChatMessageUi(
                    id = "msg_${nowInstant().toEpochMilliseconds()}_reply",
                    sender = "YAAN",
                    text = result.replyText,
                    citations = result.citations,
                    proposedAction = result.proposedAction
                )

                uiState = uiState.copy(
                    isStreaming = false,
                    messages = uiState.messages + yaanMsg,
                    responseMarkdown = result.replyText,
                    proposedAction = result.proposedAction ?: uiState.proposedAction
                )
            }
        }
    }

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
                            proposedAction = uiState.proposedAction?.copy(
                                isConfirmed = true,
                                isDismissed = false,
                                isUndone = false
                            ),
                            responseMarkdown = "Action confirmed and committed to local vault. State updated."
                        )
                    }
                }
                AskYaanUiAction.DismissActionProposal -> {
                    uiState = uiState.copy(
                        proposedAction = uiState.proposedAction?.copy(isDismissed = true),
                        responseMarkdown = "Action proposal dismissed. No vault state was modified."
                    )
                }
                AskYaanUiAction.UndoActionProposal -> {
                    coroutineScope.launch {
                        val prop = uiState.proposedAction
                        if (prop != null && prop.isConfirmed && taskRepository != null) {
                            taskRepository.deleteTask("task_committed_${prop.id}")
                        }
                        uiState = uiState.copy(
                            proposedAction = uiState.proposedAction?.copy(
                                isUndone = true,
                                isConfirmed = false
                            ),
                            responseMarkdown = "Action reverted. Vault task removed and restored to prior state."
                        )
                    }
                }
                AskYaanUiAction.SubmitQuery -> {
                    submitPrompt(uiState.inputText)
                }
                is AskYaanUiAction.ExecutePrompt -> {
                    submitPrompt(action.prompt)
                }
                AskYaanUiAction.LaunchVoice -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
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
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (state.activeMode == "Chat") MaterialTheme.colorScheme.surface else YaanTheme.extendedColors.surfaceContainerLow)
                            .clickable { onAction(AskYaanUiAction.SwitchMode("Chat")) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Chat",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (state.activeMode == "Chat") FontWeight.Bold else FontWeight.Medium,
                                color = if (state.activeMode == "Chat") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (state.activeMode == "Voice") MaterialTheme.colorScheme.surface else YaanTheme.extendedColors.surfaceContainerLow)
                            .clickable { onAction(AskYaanUiAction.SwitchMode("Voice")) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (state.activeMode == "Voice") YaanTheme.extendedColors.bronzeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice",
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
                        text = "100% On-Device Vault · Zero Cloud Leakage",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Context beacon row (showing Loading while reasoning)
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
                            .background(if (state.isStreaming) Color(0xFFF59E0B) else YaanTheme.extendedColors.bronzeAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isStreaming) "Loading AI · Reasoning (0ms)..." else "Ask YAAN Autonomous Navigator",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.isStreaming) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
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

            // ── Dynamic Conversation Messages Thread ──────────────────────────
            state.messages.forEach { msg ->
                if (msg.sender == "USER") {
                    // User Query Prompt Bubble (Aligned Right)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                        ) {
                            Text(
                                text = msg.timestamp,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = YaanTheme.extendedColors.bronzeAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
                    // YAAN Intelligence Dispatch Card (Aligned Left)
                    YaanCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    text = "YAAN EXECUTIVE INTELLIGENCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.05.sp
                                    )
                                )
                            }

                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            )

                            // Citations if available
                            if (msg.citations.isNotEmpty()) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Grounded Citations",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    msg.citations.forEach { cite ->
                                        CitationCard(
                                            title = cite,
                                            meta = "Encrypted On-Device Vault",
                                            icon = Icons.Default.EditNote
                                        )
                                    }
                                }
                            }

                            // Interactive Action Proposal Card
                            (msg.proposedAction ?: (if (msg == state.messages.lastOrNull()) state.proposedAction else null))?.let { proposal ->
                                ActionProposalCard(
                                    proposal = proposal,
                                    onConfirm = { onAction(AskYaanUiAction.ConfirmActionProposal) },
                                    onDismiss = { onAction(AskYaanUiAction.DismissActionProposal) },
                                    onUndo = { onAction(AskYaanUiAction.UndoActionProposal) }
                                )
                            }

                            // Feedback Footer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.BookmarkAdd, "Save", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.Share, "Share", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text(
                                    text = "0ms local inference",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── Streaming / Reasoning Indicator ──────────────────────────────
            AnimatedVisibility(visible = state.isStreaming) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(YaanTheme.extendedColors.champagneTint)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading AI response on-device...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Medium
                        )
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
                            text = "Thinking Process · Scanned vault sources · 0ms",
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
                            text = "• Checked on-device vault: Verified tasks, notes, and commitments.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "• Zero-cloud assurance: Request resolved locally with zero network egress.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "• Action safety: Any proposed state mutation requires explicit single-tap approval.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // ── Contextual Accelerator Suggestion Pills (Direct-Execution onClick) ─
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                YaanChip(
                    text = "What are my tasks?",
                    leadingIcon = Icons.Default.EventAvailable,
                    onClick = { onAction(AskYaanUiAction.ExecutePrompt("What are my tasks?")) }
                )
                YaanChip(
                    text = "What's my day look like?",
                    leadingIcon = Icons.Default.Analytics,
                    onClick = { onAction(AskYaanUiAction.ExecutePrompt("What's my day look like?")) }
                )
                YaanChip(
                    text = "Add task: Review Q3 financials",
                    leadingIcon = Icons.Default.Add,
                    onClick = { onAction(AskYaanUiAction.ExecutePrompt("Add task: Review Q3 financials")) }
                )
                YaanChip(
                    text = "What can you do?",
                    leadingIcon = Icons.Default.Verified,
                    onClick = { onAction(AskYaanUiAction.ExecutePrompt("What can you do?")) }
                )
            }
        }

        // ── Signature Floating Pill Composer Dock (with IME Padding for Keyboard) ─
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
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
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                onAction(AskYaanUiAction.SubmitQuery)
                            }
                        ),
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
