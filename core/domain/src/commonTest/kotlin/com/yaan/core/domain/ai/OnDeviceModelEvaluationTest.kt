package com.yaan.core.domain.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnDeviceModelEvaluationTest {

    @Test
    fun testDeviceRamTieringAndTrimming() {
        // 2 GB RAM (Budget Android device)
        val tier1 = DeviceRamTier.resolveTier(2L * 1024 * 1024 * 1024)
        assertEquals(DeviceRamTier.TIER_1_LOW, tier1)
        assertFalse(tier1.allowsOnDeviceSlm, "Tier 1 must strictly forbid resident SLM")

        val monitor1 = MemoryLifecycleMonitor(tier1)
        assertFalse(monitor1.loadModel(), "Cannot load SLM on Tier 1")

        // 6 GB RAM (Mid-range Android device)
        val tier2 = DeviceRamTier.resolveTier(6L * 1024 * 1024 * 1024)
        assertEquals(DeviceRamTier.TIER_2_MID, tier2)
        assertTrue(tier2.allowsOnDeviceSlm)

        val monitor2 = MemoryLifecycleMonitor(tier2)
        assertTrue(monitor2.loadModel())
        assertTrue(monitor2.isModelLoadedInRam)

        // Simulate Android onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL = 15)
        monitor2.onTrimMemory(15)
        assertFalse(monitor2.isModelLoadedInRam, "Model must be immediately unloaded upon critical memory pressure")
        assertEquals(1, monitor2.trimEventsHandled)
    }

    @Test
    fun testModelDownloadManagerLifecycle() {
        val descriptor = ModelDownloadManager.SMOLLM2_360M_DESCRIPTOR
        val manager = ModelDownloadManager(descriptor, requiresWifiOnly = true)

        assertEquals("220 MB (SmolLM2-360M INT4 Intent Router)", descriptor.userSizeDisclosure)

        // 1. Wi-Fi constraint: Fail if on cellular
        val cellularAttempt = manager.startDownload(
            availableDiskBytes = 10L * 1024 * 1024 * 1024, // 10 GB free
            isOnWifi = false
        )
        assertFalse(cellularAttempt)
        assertEquals(ModelDownloadStatus.FAILED, manager.status)
        assertTrue(manager.failureReason!!.contains("Wi-Fi connection required"))

        // 2. Storage constraint: Fail if insufficient disk space (< 1.5x 220MB = 330MB)
        val lowDiskAttempt = manager.startDownload(
            availableDiskBytes = 100L * 1024 * 1024, // 100 MB free (need 330 MB)
            isOnWifi = true
        )
        assertFalse(lowDiskAttempt)
        assertTrue(manager.failureReason!!.contains("Insufficient storage space"))

        // 3. Successful download simulation on Wi-Fi with sufficient disk
        val successStart = manager.startDownload(
            availableDiskBytes = 5L * 1024 * 1024 * 1024, // 5 GB free
            isOnWifi = true
        )
        assertTrue(successStart)
        assertEquals(ModelDownloadStatus.DOWNLOADING, manager.status)

        manager.updateProgress(50)
        assertEquals(50, manager.progressPercent)

        manager.updateProgress(100)
        assertEquals(ModelDownloadStatus.VERIFYING_CHECKSUM, manager.status)

        // 4. Corrupt checksum check
        val corruptVerified = manager.verifyChecksum("bad_hash_123")
        assertFalse(corruptVerified)
        assertEquals(ModelDownloadStatus.FAILED, manager.status)

        // 5. Valid checksum check
        manager.startDownload(5L * 1024 * 1024 * 1024, true)
        manager.updateProgress(100)
        val validVerified = manager.verifyChecksum(descriptor.sha256Checksum)
        assertTrue(validVerified)
        assertEquals(ModelDownloadStatus.INSTALLED, manager.status)

        // 6. Delete model
        manager.deleteModel()
        assertEquals(ModelDownloadStatus.NOT_DOWNLOADED, manager.status)
    }

    @Test
    fun testIntentRouterConfidenceCalibration() {
        val devSet = IntentRouterCalibration.createDevSet()
        assertEquals(100, devSet.size)

        val curve = IntentRouterCalibration.computeCalibrationCurve(devSet)
        println("=== INTENT ROUTER CONFIDENCE CALIBRATION CURVE ===")
        println("Threshold | Accuracy | Precision | Recall | F1 Score | Cascade Rate")
        for (p in curve) {
            println("${"%.2f".format(p.threshold).padEnd(9)} | ${"%.4f".format(p.localAccuracy).padEnd(8)} | ${"%.4f".format(p.localPrecision).padEnd(9)} | ${"%.4f".format(p.localRecall).padEnd(6)} | ${"%.4f".format(p.localF1).padEnd(8)} | ${"%.4f".format(p.cloudCascadeRate)}")
        }

        val optimal = IntentRouterCalibration.findOptimalThreshold(curve)
        println("Optimal Data-Driven Routing Threshold θ*: ${optimal.threshold} (F1: ${optimal.localF1})")

        // Invariant: Threshold must be chosen from data, not blind 0.75
        assertNotNull(optimal)
        assertTrue(optimal.localF1 > 0.85, "Calibrated threshold must achieve F1 > 0.85 on dev set")
    }
}
