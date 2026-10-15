@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.yaan.app.voice

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance

actual class PlatformVoiceSpeaker actual constructor() {
    private val synthesizer = AVSpeechSynthesizer()

    init {
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                category = AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionDuckOthers,
                error = null
            )
        } catch (_: Throwable) {}
    }

    private fun resolveVoice(languageCode: String): AVSpeechSynthesisVoice? {
        val allVoices = AVSpeechSynthesisVoice.speechVoices() as? List<*>
        val matchedVoices = allVoices?.filterIsInstance<AVSpeechSynthesisVoice>()
            ?.filter { it.language.startsWith(languageCode) } ?: emptyList()

        // 1. Prefer Enhanced or Premium quality voice
        val highQuality = matchedVoices.firstOrNull { it.quality > 1L }
        if (highQuality != null) return highQuality

        // 2. Match requested language code
        val byLang = AVSpeechSynthesisVoice.voiceWithLanguage(languageCode)
        if (byLang != null) return byLang

        // 3. Calm, refined British accent fallback
        val british = AVSpeechSynthesisVoice.voiceWithLanguage("en-GB")
        if (british != null) return british

        return AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
    }

    actual fun speak(text: String, rate: Float, language: String) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val cleanText = text.replace(Regex("[*#_`>]"), " ").trim()
        if (cleanText.isEmpty()) return

        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                category = AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionDuckOthers,
                error = null
            )
        } catch (_: Throwable) {}

        val utterance = AVSpeechUtterance(string = cleanText)
        utterance.rate = 0.50f * rate.coerceIn(0.5f, 2.0f)
        utterance.pitchMultiplier = 1.02f
        utterance.preUtteranceDelay = 0.05
        utterance.postUtteranceDelay = 0.05
        utterance.voice = resolveVoice(language)
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual val isSpeaking: Boolean
        get() = synthesizer.isSpeaking()
}
