package com.yaan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaan.app.ai.YaanAssistantEngine
import com.yaan.app.ui.components.YaanCard
import com.yaan.app.ui.components.YaanChip
import com.yaan.app.ui.components.YaanPrimaryButton
import com.yaan.app.ui.state.ProposedActionUi
import com.yaan.app.ui.theme.YaanTheme
import com.yaan.app.voice.PlatformVoiceSpeaker
import com.yaan.core.common.datetime.nowInstant
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.repository.MemoryRepository
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.TaskRepository
import com.yaan.feature.voice.VoiceSessionState
import kotlinx.coroutines.launch

@Composable
fun LiveVoiceModal(
    onDismiss: () -> Unit,
    noteRepository: NoteRepository? = null,
    taskRepository: TaskRepository? = null,
    memoryRepository: MemoryRepository? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val assistantEngine = remember {
        YaanAssistantEngine(noteRepository, taskRepository, memoryRepository)
    }
    val voiceSpeaker = remember { PlatformVoiceSpeaker() }

    DisposableEffect(Unit) {
        onDispose {
            voiceSpeaker.stop()
        }
    }

    var sessionState by remember { mutableStateOf(VoiceSessionState.IDLE) }
    var userUtterance by remember { mutableStateOf("Hold to talk or tap a voice prompt below...") }
    var yaanResponse by remember { mutableStateOf("YAAN Live Voice Assistant is active. Speak any command, request a briefing, or search your encrypted vault.") }
    var proposedAction by remember {
        mutableStateOf<ProposedActionUi?>(
            ProposedActionUi(
                id = "voice_action_01",
                title = "Schedule 45m Focus Block",
                subtitle = "Today · 2:45 PM – 3:30 PM · Locked to local calendar",
                actionType = "CALENDAR_RESCHEDULE",
                isConfirmed = false
            )
        )
    }
    var isConfirmed by remember { mutableStateOf(false) }
    var speedIndex by remember { mutableStateOf(0) }
    val speeds = listOf(1.0f, 1.2f, 1.5f)
    val speedLabels = listOf("1.0x", "1.2x", "1.5x")
    var isMuted by remember { mutableStateOf(false) }

    fun executeVoiceCommand(command: String) {
        userUtterance = "“$command”"
        sessionState = VoiceSessionState.THINKING
        voiceSpeaker.stop()

        coroutineScope.launch {
            val result = assistantEngine.processQuery(command)
            yaanResponse = "“${result.replyText}”"
            proposedAction = result.proposedAction ?: proposedAction
            isConfirmed = false
            sessionState = VoiceSessionState.SPEAKING

            if (!isMuted) {
                voiceSpeaker.speak(result.replyText, speeds[speedIndex])
            }
        }
    }

    // Dynamic Orb Animations
    val infiniteTransition = rememberInfiniteTransition()
    val rotationSpeed = when (sessionState) {
        VoiceSessionState.THINKING -> 3000
        VoiceSessionState.SPEAKING -> 6000
        VoiceSessionState.LISTENING -> 8000
        else -> 14000
    }
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(rotationSpeed, easing = LinearEasing), RepeatMode.Restart)
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(rotationSpeed / 2 + 2000, easing = LinearEasing), RepeatMode.Restart)
    )
    val pulseMax = if (sessionState == VoiceSessionState.LISTENING || sessionState == VoiceSessionState.SPEAKING) 1.08f else 1.03f
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = pulseMax,
        animationSpec = infiniteRepeatable(tween(if (sessionState == VoiceSessionState.LISTENING) 800 else 1600), RepeatMode.Reverse)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Top Header & Shield Subtext ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YaanTheme.extendedColors.sandSurface)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (sessionState) {
                                    VoiceSessionState.LISTENING -> Color(0xFFEF4444)
                                    VoiceSessionState.THINKING -> Color(0xFF3B82F6)
                                    VoiceSessionState.SPEAKING -> Color(0xFF10B981)
                                    else -> YaanTheme.extendedColors.bronzeAccent
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "YAAN LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = when (sessionState) {
                            VoiceSessionState.LISTENING -> " · Listening"
                            VoiceSessionState.THINKING -> " · Thinking (0ms)"
                            VoiceSessionState.SPEAKING -> " · Speaking"
                            else -> " · Ready"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            if (isMuted) voiceSpeaker.stop()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(YaanTheme.extendedColors.surfaceContainerLow)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            voiceSpeaker.stop()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(YaanTheme.extendedColors.surfaceContainerLow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Shield subtext
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = YaanTheme.extendedColors.bronzeAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Zero-cloud private audio stream · 100% On-device executive core",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // ── 2. Kinetic 3D Golden Orb ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Orbital Track Outer
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .rotate(rotationAngle)
                        .border(
                            1.dp,
                            YaanTheme.extendedColors.bronzeAccent.copy(alpha = 0.35f),
                            CircleShape
                        )
                )

                // Orbital Track Mid
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .rotate(counterRotation)
                        .border(
                            1.dp,
                            YaanTheme.extendedColors.amberGold.copy(alpha = 0.45f),
                            CircleShape
                        )
                )

                // Outer ambient glow
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    YaanTheme.extendedColors.champagneTint,
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Core 3D Luminous Golden Orb
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(orbPulse)
                        .shadow(elevation = 14.dp, shape = CircleShape, spotColor = YaanTheme.extendedColors.bronzeAccent)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFBE48A),
                                    Color(0xFFD4AF37),
                                    Color(0xFF8A6511)
                                )
                            )
                        )
                        .border(2.dp, Color(0xFFFFF2B2).copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }
            }

            // Speaking status pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(YaanTheme.extendedColors.champagneTint)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (sessionState) {
                                VoiceSessionState.LISTENING -> Color(0xFFEF4444)
                                VoiceSessionState.THINKING -> Color(0xFF3B82F6)
                                VoiceSessionState.SPEAKING -> Color(0xFF10B981)
                                else -> YaanTheme.extendedColors.bronzeAccent
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (sessionState) {
                        VoiceSessionState.LISTENING -> "Listening to audio..."
                        VoiceSessionState.THINKING -> "Reasoning on-device..."
                        VoiceSessionState.SPEAKING -> "Speaking aloud..."
                        else -> "Tap Hold to Talk below"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = YaanTheme.extendedColors.bronzeAccent
                    )
                )
            }

            // ── 3. User Utterance Transcript Bubble ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(YaanTheme.extendedColors.sandSurface)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = userUtterance,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                )
            }

            // ── 4. YAAN Intelligence Response Card ────────────────────────────
            YaanCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = YaanTheme.extendedColors.surfaceContainerLowest,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = YaanTheme.extendedColors.bronzeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YAAN Intelligence",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Text(
                            text = "0ms on-device",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Text(
                        text = yaanResponse,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    )

                    // Embedded Schedule / Action Confirmation Card
                    proposedAction?.let { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(YaanTheme.extendedColors.sandSurface)
                                .border(1.dp, YaanTheme.extendedColors.delicateBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = YaanTheme.extendedColors.bronzeAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1
                                )
                            }

                            YaanPrimaryButton(
                                text = if (isConfirmed) "Committed" else "Confirm",
                                onClick = {
                                    if (!isConfirmed && taskRepository != null) {
                                        coroutineScope.launch {
                                            taskRepository.upsertTask(
                                                Task(
                                                    id = "task_voice_${nowInstant().toEpochMilliseconds()}",
                                                    title = action.title,
                                                    priority = TaskPriority.HIGH
                                                )
                                            )
                                            isConfirmed = true
                                            if (!isMuted) {
                                                voiceSpeaker.speak("Action committed and locked to your vault.")
                                            }
                                        }
                                    } else {
                                        isConfirmed = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── 5. Quick Voice Accelerator Pills ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                YaanChip(
                    text = "Review my commitments",
                    leadingIcon = Icons.Default.CalendarToday,
                    onClick = { executeVoiceCommand("Review my commitments and prepare daily plan") }
                )
                YaanChip(
                    text = "What are my tasks?",
                    leadingIcon = Icons.Default.AutoAwesome,
                    onClick = { executeVoiceCommand("What are my tasks?") }
                )
                YaanChip(
                    text = "Add task: Review Q3 financials",
                    leadingIcon = Icons.Default.CalendarToday,
                    onClick = { executeVoiceCommand("Add task: Review Q3 financials") }
                )
                YaanChip(
                    text = "What's my day look like?",
                    leadingIcon = Icons.Default.AutoAwesome,
                    onClick = { executeVoiceCommand("What's my day look like?") }
                )
            }

            // ── 6. Mode Switcher Chips Row ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YaanTheme.extendedColors.sandSurface)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable {
                            voiceSpeaker.stop()
                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Text chat",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YaanTheme.extendedColors.sandSurface)
                        .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable {
                            speedIndex = (speedIndex + 1) % speeds.size
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = speedLabels[speedIndex],
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 7. Bottom Floating Voice Controller Dock ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(YaanTheme.extendedColors.surfaceContainerLowest)
                    .border(1.dp, YaanTheme.extendedColors.delicateBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (sessionState == VoiceSessionState.LISTENING) Color(0xFFEF4444)
                            else YaanTheme.extendedColors.surfaceContainerLow
                        )
                        .clickable {
                            if (sessionState == VoiceSessionState.IDLE) {
                                sessionState = VoiceSessionState.LISTENING
                                userUtterance = "Listening... Tap 'Hold to talk' or speak a prompt"
                            } else if (sessionState == VoiceSessionState.LISTENING) {
                                executeVoiceCommand("Review my commitments and prepare daily plan")
                            } else if (sessionState == VoiceSessionState.SPEAKING) {
                                voiceSpeaker.stop()
                                sessionState = VoiceSessionState.IDLE
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (sessionState == VoiceSessionState.LISTENING) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Wide Gold "Hold to talk" pill
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFBBF24),
                                    Color(0xFFD97706)
                                )
                            )
                        )
                        .clickable {
                            if (sessionState == VoiceSessionState.IDLE) {
                                sessionState = VoiceSessionState.LISTENING
                                userUtterance = "Listening... Tap to execute command"
                            } else if (sessionState == VoiceSessionState.LISTENING) {
                                executeVoiceCommand("Review my commitments and prepare daily plan")
                            } else if (sessionState == VoiceSessionState.SPEAKING) {
                                voiceSpeaker.stop()
                                sessionState = VoiceSessionState.IDLE
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF141311),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (sessionState) {
                            VoiceSessionState.LISTENING -> "Tap to Process"
                            VoiceSessionState.THINKING -> "Thinking..."
                            VoiceSessionState.SPEAKING -> "Tap to Stop"
                            else -> "Tap to Speak"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF141311),
                            fontSize = 13.sp
                        )
                    )
                }

                // Hang Up Button (Red)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                        .clickable {
                            voiceSpeaker.stop()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
