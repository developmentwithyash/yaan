package com.yaan.app.voice

/**
 * Cross-platform interface for synthesizing speech audio.
 */
expect class PlatformVoiceSpeaker() {
    fun speak(text: String, rate: Float = 1.0f)
    fun stop()
    val isSpeaking: Boolean
}
