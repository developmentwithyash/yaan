package com.yaan.app.ci

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BinarySizeReportTest {

    private fun resolveProjectRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null) {
            if (File(dir, "settings.gradle.kts").exists() && File(dir, ".github/workflows/ci.yml").exists()) {
                return dir
            }
            dir = dir.parentFile
        }
        error("Could not locate project root from ${System.getProperty("user.dir")}")
    }

    @Test
    fun testCiWorkflowContainsBinarySizeReportStep() {
        val root = resolveProjectRoot()
        val ciFile = File(root, ".github/workflows/ci.yml")
        val content = ciFile.readText()

        assertTrue(content.contains("binary_size_report.sh"), "ci.yml must invoke scripts/binary_size_report.sh")
        assertTrue(content.contains("GITHUB_STEP_SUMMARY"), "ci.yml or script must support GITHUB_STEP_SUMMARY")
        assertTrue(content.contains("assembleDebug"), "ci.yml must build composeApp-debug.apk for size measurement")
        assertTrue(content.contains("packageUberJarForCurrentOS"), "ci.yml must build desktop uber jar for size measurement")
    }

    @Test
    fun testBinarySizeReportPassesWithinBudgetAndWritesSummary() {
        val root = resolveProjectRoot()
        val script = File(root, "scripts/binary_size_report.sh")
        assertTrue(script.exists(), "scripts/binary_size_report.sh must exist")

        val tempDir = createTempDirectory("yaan-size-pass").toFile()
        try {
            val mockApk = File(tempDir, "composeApp-debug.apk").apply { writeBytes(ByteArray(1024 * 512)) }
            val mockJar = File(tempDir, "composeApp-desktop.jar").apply { writeBytes(ByteArray(1024 * 768)) }
            val summaryFile = File(tempDir, "step_summary.md")

            val process = ProcessBuilder("bash", script.absolutePath).apply {
                directory(root)
                environment()["APK_PATH"] = mockApk.absolutePath
                environment()["JAR_PATH"] = mockJar.absolutePath
                environment()["APK_BUDGET_BYTES"] = (2 * 1024 * 1024).toString()
                environment()["JAR_BUDGET_BYTES"] = (2 * 1024 * 1024).toString()
                environment()["GITHUB_STEP_SUMMARY"] = summaryFile.absolutePath
                redirectErrorStream(true)
            }.start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            assertEquals(0, exitCode, "Expected exit code 0 within budget, got $exitCode:\n$output")
            assertTrue(summaryFile.exists(), "GITHUB_STEP_SUMMARY file should be created")
            val summary = summaryFile.readText()
            assertTrue(summary.contains("composeApp-debug.apk"), "Summary must report APK artifact")
            assertTrue(summary.contains("composeApp-desktop.jar"), "Summary must report Desktop JAR artifact")
            assertTrue(summary.contains("PASS"), "Summary must indicate PASS status")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testBinarySizeReportFailsWhenBudgetExceeded() {
        val root = resolveProjectRoot()
        val script = File(root, "scripts/binary_size_report.sh")
        assertTrue(script.exists(), "scripts/binary_size_report.sh must exist")

        val tempDir = createTempDirectory("yaan-size-fail").toFile()
        try {
            val oversizedApk = File(tempDir, "composeApp-debug.apk").apply { writeBytes(ByteArray(2048)) }
            val okJar = File(tempDir, "composeApp-desktop.jar").apply { writeBytes(ByteArray(512)) }
            val summaryFile = File(tempDir, "step_summary.md")

            val process = ProcessBuilder("bash", script.absolutePath).apply {
                directory(root)
                environment()["APK_PATH"] = oversizedApk.absolutePath
                environment()["JAR_PATH"] = okJar.absolutePath
                environment()["APK_BUDGET_BYTES"] = "1024"
                environment()["JAR_BUDGET_BYTES"] = "2048"
                environment()["GITHUB_STEP_SUMMARY"] = summaryFile.absolutePath
                redirectErrorStream(true)
            }.start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            assertNotEquals(0, exitCode, "Expected non-zero exit code when APK exceeds budget:\n$output")
            val summary = summaryFile.readText()
            assertTrue(summary.contains("EXCEEDED"), "Summary must flag EXCEEDED status")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
