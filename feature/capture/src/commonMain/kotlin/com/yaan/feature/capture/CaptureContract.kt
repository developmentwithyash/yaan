package com.yaan.feature.capture

data class CaptureState(
    val inputText: String = "",
    val detectedUrls: List<UrlPreviewMetadata> = emptyList(),
    val isSubmitting: Boolean = false,
    val feedbackMessage: String? = null,
)

sealed interface CaptureIntent {
    data class UpdateInput(val text: String) : CaptureIntent
    data object Submit : CaptureIntent
    data object ClearFeedback : CaptureIntent
}
