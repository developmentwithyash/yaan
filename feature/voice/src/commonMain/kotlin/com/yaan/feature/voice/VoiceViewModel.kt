package com.yaan.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.logging.YaanLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val dispatchers: CoroutineDispatchers,
    private val captureDriver: AudioCaptureDriver = NoOpAudioCaptureDriver(),
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceState())
    val state = _state.asStateFlow()

    init {
        observeCaptureEvents()
    }

    private fun observeCaptureEvents() {
        viewModelScope.launch(dispatchers.main) {
            captureDriver.events.collect { event ->
                when (event) {
                    is AudioCaptureEvent.DeviceDisconnected -> {
                        val detail = if (!event.deviceName.isNullOrBlank()) {
                            "${event.deviceName}: ${event.reason}"
                        } else {
                            event.reason
                        }
                        handleCaptureInterruption(detail)
                    }
                    is AudioCaptureEvent.CaptureFailed -> {
                        handleCaptureInterruption(event.reason)
                    }
                }
            }
        }
    }

    fun onIntent(intent: VoiceIntent) {
        when (intent) {
            is VoiceIntent.ToggleListening -> toggleListening()
            is VoiceIntent.CancelRecording -> cancelRecording()
            is VoiceIntent.DeviceDisconnected -> handleCaptureInterruption(intent.reason)
            is VoiceIntent.DismissRecordingError -> {
                _state.update { it.copy(recordingState = VoiceRecordingState.Idle) }
            }
            is VoiceIntent.SetSpeed -> {
                _state.update { it.copy(playbackSpeed = intent.speed) }
            }
            is VoiceIntent.ToggleMute -> {
                _state.update { it.copy(isMuted = !it.isMuted) }
            }
        }
    }

    private fun toggleListening() {
        if (_state.value.sessionState == VoiceSessionState.IDLE) {
            try {
                captureDriver.startCapture()
                _state.update {
                    it.copy(
                        sessionState = VoiceSessionState.LISTENING,
                        recordingState = VoiceRecordingState.Recording
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val reason = e.message?.takeIf { msg -> msg.isNotBlank() }
                    ?: "Failed to start audio capture"
                YaanLogger.w("VoiceViewModel", e) { "Audio capture start failed: $reason" }
                _state.update {
                    it.copy(
                        sessionState = VoiceSessionState.IDLE,
                        recordingState = VoiceRecordingState.Error(reason)
                    )
                }
            }
        } else {
            runCatching { captureDriver.stopCapture() }
                .onFailure { e ->
                    YaanLogger.w("VoiceViewModel", e) { "Audio capture stop failed: ${e.message}" }
                }
            _state.update {
                it.copy(
                    sessionState = VoiceSessionState.IDLE,
                    recordingState = VoiceRecordingState.Idle
                )
            }
        }
    }

    private fun cancelRecording() {
        runCatching { captureDriver.cancelCapture() }
            .onFailure { e ->
                YaanLogger.w("VoiceViewModel", e) { "Audio capture cancel failed: ${e.message}" }
            }
        _state.update {
            it.copy(
                sessionState = VoiceSessionState.IDLE,
                recordingState = VoiceRecordingState.Cancelled,
                transcriptText = ""
            )
        }
    }

    private fun handleCaptureInterruption(reason: String) {
        runCatching { captureDriver.cancelCapture() }
            .onFailure { e ->
                YaanLogger.w("VoiceViewModel", e) { "Cleanup after disconnect failed: ${e.message}" }
            }
        val safeReason = reason.ifBlank { "Audio input device disconnected" }
        _state.update {
            it.copy(
                sessionState = VoiceSessionState.IDLE,
                recordingState = VoiceRecordingState.Error(safeReason)
            )
        }
    }
}
