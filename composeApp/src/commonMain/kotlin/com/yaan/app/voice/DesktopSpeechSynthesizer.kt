package com.yaan.app.voice

/**
 * Handle representing an actively running speech process.
 */
interface DesktopProcessHandle {
    val isAlive: Boolean
    fun destroyForcibly()
    fun waitFor(): Int
}

/**
 * Interface abstraction for dispatching operating system speech processes.
 */
interface DesktopProcessExecutor {
    fun startProcess(command: List<String>): DesktopProcessHandle
}

/**
 * Resolves platform-specific TTS CLI commands across macOS, Linux, and Windows.
 */
class DesktopSpeechCommandResolver {

    fun resolveCommand(
        osName: String,
        text: String,
        rate: Float = 1.0f,
        language: String = "en-US"
    ): List<String> {
        val sanitizedText = sanitizeText(text)
        val lowerOs = osName.lowercase()

        return when {
            lowerOs.contains("mac") -> {
                // macOS /usr/bin/say: normal speaking rate is ~175 wpm
                val wpm = (175 * rate).toInt().coerceIn(50, 400)
                listOf("/usr/bin/say", "-r", wpm.toString(), sanitizedText)
            }
            lowerOs.contains("linux") -> {
                // Linux speech-dispatcher / espeak
                val speechRate = ((rate - 1.0f) * 100).toInt().coerceIn(-100, 100)
                listOf("spd-say", "-r", speechRate.toString(), sanitizedText)
            }
            lowerOs.contains("win") -> {
                // Windows PowerShell System.Speech.Synthesis
                val psRate = ((rate - 1.0f) * 10).toInt().coerceIn(-10, 10)
                val escapedScript = "Add-Type -AssemblyName System.Speech; \$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; \$synth.Rate = $psRate; \$synth.Speak('$sanitizedText')"
                listOf("powershell.exe", "-NoProfile", "-Command", escapedScript)
            }
            else -> {
                // Fallback generic voice command
                listOf("say", sanitizedText)
            }
        }
    }

    private fun sanitizeText(input: String): String {
        return input.replace("'", "")
            .replace("\"", "")
            .replace(";", "")
            .replace("|", "")
            .replace("&", "")
            .replace("$", "")
            .replace("`", "")
            .trim()
    }
}

/**
 * Desktop Speech Synthesizer managing speech execution and lifecycle.
 * Satisfies Issue #25:
 * - Cross-platform command resolution (macOS say, Linux spd-say, Windows PowerShell).
 * - Immediate stop() cancellation of running processes.
 * - Accurate isSpeaking state reporting.
 */
class DesktopSpeechSynthesizer(
    private val osName: String = "Mac OS X",
    private val executor: DesktopProcessExecutor,
    private val resolver: DesktopSpeechCommandResolver = DesktopSpeechCommandResolver()
) {
    private var activeHandle: DesktopProcessHandle? = null

    val isSpeaking: Boolean
        get() = activeHandle?.isAlive == true

    fun speak(text: String, rate: Float = 1.0f, language: String = "en-US") {
        stop()
        val command = resolver.resolveCommand(osName, text, rate, language)
        try {
            activeHandle = executor.startProcess(command)
        } catch (_: Throwable) {
            activeHandle = null
        }
    }

    fun stop() {
        activeHandle?.let { handle ->
            if (handle.isAlive) {
                handle.destroyForcibly()
            }
        }
        activeHandle = null
    }
}
