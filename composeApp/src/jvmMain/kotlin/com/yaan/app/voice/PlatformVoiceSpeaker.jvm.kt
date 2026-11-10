package com.yaan.app.voice

class JvmProcessHandle(private val process: Process) : DesktopProcessHandle {
    override val isAlive: Boolean
        get() = process.isAlive

    override fun destroyForcibly() {
        process.destroyForcibly()
    }

    override fun waitFor(): Int {
        return process.waitFor()
    }
}

class JvmProcessExecutor : DesktopProcessExecutor {
    override fun startProcess(command: List<String>): DesktopProcessHandle {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        return JvmProcessHandle(process)
    }
}

actual class PlatformVoiceSpeaker actual constructor() {
    private val synthesizer = DesktopSpeechSynthesizer(
        osName = System.getProperty("os.name") ?: "Mac OS X",
        executor = JvmProcessExecutor()
    )

    actual val isSpeaking: Boolean
        get() = synthesizer.isSpeaking

    actual fun speak(text: String, rate: Float, language: String) {
        synthesizer.speak(text, rate, language)
    }

    actual fun stop() {
        synthesizer.stop()
    }
}
