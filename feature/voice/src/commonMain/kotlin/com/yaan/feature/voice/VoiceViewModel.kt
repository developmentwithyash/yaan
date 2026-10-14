package com.yaan.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceState())
    val state = _state.asStateFlow()

    fun onIntent(intent: VoiceIntent) {
        when (intent) {
            is VoiceIntent.ToggleListening -> {
                _state.update {
                    val nextState = if (it.sessionState == VoiceSessionState.IDLE) {
                        VoiceSessionState.LISTENING
                    } else {
                        VoiceSessionState.IDLE
                    }
                    it.copy(sessionState = nextState)
                }
            }
            is VoiceIntent.SetSpeed -> {
                _state.update { it.copy(playbackSpeed = intent.speed) }
            }
            is VoiceIntent.ToggleMute -> {
                _state.update { it.copy(isMuted = !it.isMuted) }
            }
        }
    }
}
