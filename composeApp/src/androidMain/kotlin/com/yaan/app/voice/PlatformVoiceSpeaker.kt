package com.yaan.app.voice

actual class PlatformVoiceSpeaker actual constructor() {
    private var speaking = false

    actual fun speak(text: String, rate: Float, language: String) {
        speaking = true
    }

    actual fun stop() {
        speaking = false
    }

    actual val isSpeaking: Boolean
        get() = speaking
}
