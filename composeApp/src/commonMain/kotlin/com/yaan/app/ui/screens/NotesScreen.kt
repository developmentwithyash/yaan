package com.yaan.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ui.components.YaanCard
import com.yaan.app.ui.state.NoteItemUi
import com.yaan.app.ui.state.NotesUiAction
import com.yaan.app.ui.state.NotesUiState
import com.yaan.app.ui.state.defaultNotesList
import com.yaan.app.ui.theme.YaanTheme
import com.yaan.core.domain.model.AudioMemoMetadata
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.VaultTag
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.launch

/**
 * Stateful Container Composable for NotesScreen.
 * Coordinates notes search, category filtering, audio player state, and local SQLite synchronization.
 */
@Composable
fun NotesScreen(
    onNavigateToAskYaan: () -> Unit = {},
    noteRepository: NoteRepository? = null,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(NotesUiState()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(noteRepository) {
        if (noteRepository != null) {
            // Seed starter notes into SQLite if the vault is currently empty
            val initial = noteRepository.getNotes().getOrDefault(emptyList())
            if (initial.isEmpty()) {
                defaultNotesList.forEach { def ->
                    noteRepository.upsertNote(
                        Note(
                            id = def.id,
                            title = def.title,
                            contentCiphertext = def.preview,
                            tags = listOf(VaultTag(name = def.tag, isEncrypted = true)),
                            isPinned = def.isPinned,
                            audioMemo = def.audioDuration?.let {
                                AudioMemoMetadata(audioUri = "vault://audio/${def.id}", durationMs = 165_000L)
                            }
                        )
                    )
                }
            }

            // Continuously observe encrypted notes from SQLite
            noteRepository.observeNotes().collect { repoNotes ->
                if (repoNotes.isNotEmpty()) {
                    uiState = uiState.copy(
                        notes = repoNotes.map { n ->
                            NoteItemUi(
                                id = n.id,
                                title = n.title,
                                preview = n.contentCiphertext,
                                tag = n.tags.firstOrNull()?.name ?: "Personal",
                                timestamp = "Saved",
                                isPinned = n.isPinned,
                                isEncrypted = true,
                                audioDuration = n.audioMemo?.let { "${it.durationMs / 1000}s" }
                            )
                        }
                    )
                } else {
                    uiState = uiState.copy(notes = defaultNotesList)
                }
            }
        }
    }

    NotesScreenContent(
        state = uiState,
        onAction = { action ->
            when (action) {
                is NotesUiAction.UpdateSearchQuery -> {
                    uiState = uiState.copy(searchQuery = action.query)
                    if (noteRepository != null) {
                        coroutineScope.launch {
                            val res = if (action.query.isEmpty()) {
                                noteRepository.getNotes()
                            } else {
                                noteRepository.searchNotes(action.query)
                            }
                            res.onSuccess { filtered ->
                                uiState = uiState.copy(
                                    notes = filtered.map { n ->
                                        NoteItemUi(
                                            id = n.id,
                                            title = n.title,
                                            preview = n.contentCiphertext,
                                            tag = n.tags.firstOrNull()?.name ?: "Personal",
                                            timestamp = "Saved",
                                            isPinned = n.isPinned,
                                            isEncrypted = true,
                                            audioDuration = n.audioMemo?.let { "${it.durationMs / 1000}s" }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                is NotesUiAction.SelectFilter -> {
                    uiState = uiState.copy(selectedFilter = action.filter)
                }
                NotesUiAction.ToggleAudioPlayback -> {
                    uiState = uiState.copy(isPlayingAudio = !uiState.isPlayingAudio)
                }
                is NotesUiAction.DeleteNote -> {
                    coroutineScope.launch {
                        if (noteRepository != null) {
                            noteRepository.deleteNote(action.id)
                        }
                        uiState = uiState.copy(notes = uiState.notes.filterNot { it.id == action.id })
                    }
                }
                NotesUiAction.NavigateToAskYaan -> {
                    onNavigateToAskYaan()
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Pure Stateless UI Composable for NotesScreen.
 * Dynamically filters and renders all pinned and unpinned vault notes.
 */
@Composable
fun NotesScreenContent(
    state: NotesUiState,
    onAction: (NotesUiAction) -> Unit,
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

    // Dynamic filtering based on search query and category pill selection
    val filteredNotes = remember(state.notes, state.selectedFilter, state.searchQuery) {
        state.notes.filter { note ->
            val matchesSearch = if (state.searchQuery.isBlank()) true else {
                note.title.contains(state.searchQuery, ignoreCase = true) ||
                note.preview.contains(state.searchQuery, ignoreCase = true) ||
                note.tag.contains(state.searchQuery, ignoreCase = true)
            }
            val matchesFilter = when (state.selectedFilter) {
                "All Notes" -> true
                "Pinned" -> note.isPinned
                "Voice Memos" -> note.audioDuration != null || note.tag.contains("Voice", ignoreCase = true)
                else -> note.tag.equals(state.selectedFilter, ignoreCase = true)
            }
            matchesSearch && matchesFilter
        }
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val regularNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    // Dynamic list of filter categories extracted from notes
    val dynamicCategories = remember(state.notes) {
        val predefined = listOf("Strategy", "Ideas", "Decisions", "AWS Infra", "Product Specs")
        val fromNotes = state.notes.map { it.tag }.filter { it.isNotBlank() && it !in listOf("All Notes", "Voice Memos", "Pinned") }
        (predefined + fromNotes).distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Search Bar & Filter Chips ─────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Search Input Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(YaanTheme.extendedColors.surfaceContainerLowest)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.searchQuery.isEmpty()) {
                            Text(
                                text = "Search all notes, voice memos, and decisions...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { onAction(NotesUiAction.UpdateSearchQuery(it)) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Horizontal Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Notes (dynamic count badge)
                val isAllNotes = state.selectedFilter == "All Notes"
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isAllNotes) MaterialTheme.colorScheme.primary else YaanTheme.extendedColors.surfaceContainer)
                        .then(
                            if (!isAllNotes) Modifier.border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.7f), CircleShape)
                            else Modifier
                        )
                        .clickable { onAction(NotesUiAction.SelectFilter("All Notes")) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "All Notes",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAllNotes) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isAllNotes) Color.White.copy(alpha = 0.25f) else YaanTheme.extendedColors.surfaceContainerLow)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${state.notes.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllNotes) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Voice Memos
                val isVoice = state.selectedFilter == "Voice Memos"
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isVoice) MaterialTheme.colorScheme.primary else YaanTheme.extendedColors.surfaceContainer)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.7f), CircleShape)
                        .clickable { onAction(NotesUiAction.SelectFilter("Voice Memos")) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isVoice) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Voice Memos",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isVoice) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Pinned
                val isPinned = state.selectedFilter == "Pinned"
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isPinned) MaterialTheme.colorScheme.primary else YaanTheme.extendedColors.surfaceContainer)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.7f), CircleShape)
                        .clickable { onAction(NotesUiAction.SelectFilter("Pinned")) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) Color.White else YaanTheme.extendedColors.bronzeAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pinned",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isPinned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Dynamic Categories
                dynamicCategories.forEach { category ->
                    val isSelected = state.selectedFilter == category
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else YaanTheme.extendedColors.surfaceContainer)
                            .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.7f), CircleShape)
                            .clickable { onAction(NotesUiAction.SelectFilter(category)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else YaanTheme.extendedColors.bronzeAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Natural Language Semantic Query Prompt Banner ─────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YaanTheme.extendedColors.champagneTint)
                .border(1.dp, YaanTheme.extendedColors.secondaryFixedDim.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(YaanTheme.extendedColors.secondaryContainer.copy(alpha = 0.9f))
                            .border(1.dp, YaanTheme.extendedColors.secondaryFixedDim.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SEMANTIC VAULT QUERY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.05.sp,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Zero-Cloud Indexed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "\"What did we decide about the pricing model?\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Divider and Action Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.5f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = YaanTheme.extendedColors.bronzeAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cross-references all notes & audio transcripts",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onAction(NotesUiAction.NavigateToAskYaan) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ask YAAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Section 1: Pinned Foundations ─────────────────────────────────
        if (pinnedNotes.isNotEmpty() || state.selectedFilter == "Pinned") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pinned Foundations",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        )
                    }
                    Text(
                        text = "${pinnedNotes.size} ${if (pinnedNotes.size == 1) "Anchor" else "Anchors"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }

                pinnedNotes.forEach { note ->
                    NoteCardItem(
                        note = note,
                        pulseAlpha = pulseAlpha,
                        isPlayingAudio = state.isPlayingAudio,
                        onAction = onAction
                    )
                }
            }
        }

        // ── 4. Section 2: Recent Streams & Vault Records ──────────────────────
        if (regularNotes.isNotEmpty() && state.selectedFilter != "Pinned") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent Streams & Notes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        )
                    }
                    Text(
                        text = "${regularNotes.size} ${if (regularNotes.size == 1) "Record" else "Records"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    )
                }

                regularNotes.forEach { note ->
                    NoteCardItem(
                        note = note,
                        pulseAlpha = pulseAlpha,
                        isPlayingAudio = state.isPlayingAudio,
                        onAction = onAction
                    )
                }
            }
        }

        // ── 5. Empty Vault State ─────────────────────────────────────────────
        if (filteredNotes.isEmpty()) {
            YaanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = if (state.searchQuery.isNotBlank()) "No notes match \"${state.searchQuery}\"" else "No notes in ${state.selectedFilter}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Capture a thought using Quick Capture on Home or ask YAAN AI to record notes directly into your encrypted vault.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Individual Encrypted Note / Memo Card Item.
 * Renders badges, tags, titles, ciphertext preview, audio waveform (if memo), and delete action.
 */
@Composable
private fun NoteCardItem(
    note: NoteItemUi,
    pulseAlpha: Float,
    isPlayingAudio: Boolean,
    onAction: (NotesUiAction) -> Unit
) {
    val isAudio = note.audioDuration != null || note.tag.contains("Voice", ignoreCase = true)

    YaanCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAudio) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .alpha(pulseAlpha)
                                .background(Color(0xFF059669))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(YaanTheme.extendedColors.secondaryContainer.copy(alpha = 0.8f))
                            .border(1.dp, YaanTheme.extendedColors.secondaryFixedDim.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAudio) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = "#${note.tag.lowercase().replace(" ", "-")}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (note.isPinned) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(YaanTheme.extendedColors.champagneTint)
                                .border(1.dp, YaanTheme.extendedColors.bronzeAccent.copy(alpha = 0.4f), CircleShape)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = YaanTheme.extendedColors.bronzeAccent,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onAction(NotesUiAction.DeleteNote(note.id)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            )

            Text(
                text = note.preview,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp,
                    fontSize = 13.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.audioDuration != null) {
                AudioWaveformPlayer(
                    duration = note.audioDuration,
                    isPlaying = isPlayingAudio,
                    onTogglePlay = { onAction(NotesUiAction.ToggleAudioPlayback) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.4f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (note.isPinned) YaanTheme.extendedColors.bronzeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = note.timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Encrypted Vault",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Audio waveform scrubber component for voice memos with playback status.
 */
@Composable
private fun AudioWaveformPlayer(
    duration: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(YaanTheme.extendedColors.surfaceContainerLow)
            .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waveform Bars
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barHeights = listOf(8, 16, 24, 18, 12, 28, 20, 14, 22, 10, 18, 26, 16, 12, 20, 14, 8)
                barHeights.forEachIndexed { idx, h ->
                    val isPassed = idx < 10
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(h.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPassed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = if (isPlaying) "01:24 / $duration" else duration,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(YaanTheme.extendedColors.surfaceContainer)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "1.0x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
