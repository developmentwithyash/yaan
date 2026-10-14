package com.yaan.feature.voice

enum class VoiceSessionState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
}

data class VoiceState(
    val sessionState: VoiceSessionState = VoiceSessionState.IDLE,
    val transcriptText: String = "",
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
)

sealed interface VoiceIntent {
    data object ToggleListening : VoiceIntent
    data class SetSpeed(val speed: Float) : VoiceIntent
    data object ToggleMute : VoiceIntent
}
