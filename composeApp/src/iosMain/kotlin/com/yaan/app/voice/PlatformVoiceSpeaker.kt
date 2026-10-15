package com.yaan.app.voice

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance

actual class PlatformVoiceSpeaker actual constructor() {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, rate: Float) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val cleanText = text.replace(Regex("[*#_`>]"), " ").trim()
        if (cleanText.isEmpty()) return
        val utterance = AVSpeechUtterance(string = cleanText)
        utterance.rate = 0.52f * rate.coerceIn(0.5f, 2.0f)
        utterance.pitchMultiplier = 1.05f
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual val isSpeaking: Boolean
        get() = synthesizer.isSpeaking()
}
