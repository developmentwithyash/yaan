package com.yaan.app.voice

actual class PlatformVoiceSpeaker actual constructor() {
    private var speaking: Boolean = false

    actual val isSpeaking: Boolean
        get() = speaking

    actual fun speak(text: String, rate: Float, language: String) {
        speaking = true
        println("[DesktopVoiceSpeaker: $language @ ${rate}x] $text")
        speaking = false
    }

    actual fun stop() {
        speaking = false
    }
}
