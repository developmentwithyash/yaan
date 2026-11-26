package com.yaan.feature.voice

enum class VoiceSessionState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
}

sealed interface VoiceRecordingState {
    data object Idle : VoiceRecordingState
    data object Recording : VoiceRecordingState
    data object Cancelled : VoiceRecordingState
    data class Error(val reason: String) : VoiceRecordingState
}

data class VoiceState(
    val sessionState: VoiceSessionState = VoiceSessionState.IDLE,
    val recordingState: VoiceRecordingState = VoiceRecordingState.Idle,
    val transcriptText: String = "",
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
)

sealed interface VoiceIntent {
    data object ToggleListening : VoiceIntent
    data object CancelRecording : VoiceIntent
    data class DeviceDisconnected(val reason: String = "Audio input device disconnected") : VoiceIntent
    data object DismissRecordingError : VoiceIntent
    data class SetSpeed(val speed: Float) : VoiceIntent
    data object ToggleMute : VoiceIntent
}
