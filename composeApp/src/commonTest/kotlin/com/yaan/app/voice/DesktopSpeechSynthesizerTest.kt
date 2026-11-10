package com.yaan.app.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSpeechSynthesizerTest {

    private class MockProcessHandle : DesktopProcessHandle {
        var destroyed: Boolean = false
        var alive: Boolean = true

        override val isAlive: Boolean
            get() = alive && !destroyed

        override fun destroyForcibly() {
            destroyed = true
            alive = false
        }

        override fun waitFor(): Int = 0
    }

    private class MockProcessExecutor : DesktopProcessExecutor {
        var lastCommand: List<String>? = null
        val handle = MockProcessHandle()

        override fun startProcess(command: List<String>): DesktopProcessHandle {
            lastCommand = command
            return handle
        }
    }

    @Test
    fun testMacOsCommandResolution() {
        val resolver = DesktopSpeechCommandResolver()
        val cmd = resolver.resolveCommand("Mac OS X", "Hello from YAAN", 1.0f, "en-US")

        assertEquals("/usr/bin/say", cmd[0])
        assertTrue(cmd.contains("-r"))
        assertTrue(cmd.contains("Hello from YAAN"))
    }

    @Test
    fun testLinuxCommandResolution() {
        val resolver = DesktopSpeechCommandResolver()
        val cmd = resolver.resolveCommand("Linux", "Tasks for today", 1.2f, "en-US")

        assertTrue(cmd[0] == "spd-say" || cmd[0] == "espeak")
        assertTrue(cmd.contains("Tasks for today"))
    }

    @Test
    fun testWindowsCommandResolution() {
        val resolver = DesktopSpeechCommandResolver()
        val cmd = resolver.resolveCommand("Windows 11", "System online", 1.0f, "en-US")

        assertEquals("powershell.exe", cmd[0])
        assertTrue(cmd.any { it.contains("SpeechSynthesizer") })
    }

    @Test
    fun testSynthesizerDispatchesProcessAndReportsSpeaking() {
        val executor = MockProcessExecutor()
        val synth = DesktopSpeechSynthesizer(
            osName = "Mac OS X",
            executor = executor
        )

        assertFalse(synth.isSpeaking)
        synth.speak("Testing speech", 1.0f, "en-US")

        assertTrue(synth.isSpeaking)
        assertEquals("/usr/bin/say", executor.lastCommand?.get(0))
    }

    @Test
    fun testStopImmediatelyTerminatesActiveProcess() {
        val executor = MockProcessExecutor()
        val synth = DesktopSpeechSynthesizer(
            osName = "Mac OS X",
            executor = executor
        )

        synth.speak("Long running speech", 1.0f, "en-US")
        assertTrue(synth.isSpeaking)

        synth.stop()
        assertFalse(synth.isSpeaking)
        assertTrue(executor.handle.destroyed, "Process handle must be forcibly destroyed upon stop()")
    }
}
