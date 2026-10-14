package com.yaan.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.usecase.CaptureIntentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CaptureViewModel(
    private val captureIntentUseCase: CaptureIntentUseCase,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureState())
    val state = _state.asStateFlow()

    fun onIntent(intent: CaptureIntent) {
        when (intent) {
            is CaptureIntent.UpdateInput -> {
                _state.update { it.copy(inputText = intent.text) }
            }
            is CaptureIntent.Submit -> {
                submitCurrentInput()
            }
            is CaptureIntent.ClearFeedback -> {
                _state.update { it.copy(feedbackMessage = null) }
            }
        }
    }

    private fun submitCurrentInput() {
        val input = _state.value.inputText
        if (input.isBlank()) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.main) {
            val result = captureIntentUseCase(input)
            result.fold(
                onSuccess = { message ->
                    _state.update {
                        it.copy(
                            inputText = "",
                            isSubmitting = false,
                            feedbackMessage = message
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            feedbackMessage = "Error: ${error.message}"
                        )
                    }
                }
            )
        }
    }
}
