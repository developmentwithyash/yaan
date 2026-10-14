package com.yaan.core.domain.ai

/**
 * RAM hardware capability tiers for mobile devices.
 */
enum class DeviceRamTier(
    val minRamBytes: Long,
    val maxRamBytes: Long,
    val label: String,
    val allowsOnDeviceSlm: Boolean,
    val maxWorkingSetMb: Int
) {
    TIER_1_LOW(
        minRamBytes = 0L,
        maxRamBytes = 4L * 1024 * 1024 * 1024, // < 4 GB
        label = "Low Memory (<= 3 GB)",
        allowsOnDeviceSlm = false, // Strictly Lexical FTS5 + Rule Parser
        maxWorkingSetMb = 30
    ),
    TIER_2_MID(
        minRamBytes = 4L * 1024 * 1024 * 1024,
        maxRamBytes = 6L * 1024 * 1024 * 1024, // 4 GB - 6 GB
        label = "Mid Memory (4 GB - 6 GB)",
        allowsOnDeviceSlm = true, // Can load quantized SLM on-demand (e.g. SmolLM2-360M INT4, ~220MB)
        maxWorkingSetMb = 350
    ),
    TIER_3_HIGH(
        minRamBytes = 6L * 1024 * 1024 * 1024,
        maxRamBytes = Long.MAX_VALUE, // > 6 GB
        label = "High Memory (>= 8 GB)",
        allowsOnDeviceSlm = true, // Resident SLM allowed
        maxWorkingSetMb = 600
    );

    companion object {
        fun resolveTier(totalMemoryBytes: Long): DeviceRamTier {
            return when {
                totalMemoryBytes < 4L * 1024 * 1024 * 1024 -> TIER_1_LOW
                totalMemoryBytes <= 6L * 1024 * 1024 * 1024 -> TIER_2_MID
                else -> TIER_3_HIGH
            }
        }
    }
}

/**
 * Memory trimmer and lifecycle monitor handling Android onTrimMemory callbacks safely.
 */
class MemoryLifecycleMonitor(
    val tier: DeviceRamTier
) {
    var isModelLoadedInRam: Boolean = false
        private set

    var trimEventsHandled: Int = 0
        private set

    fun loadModel(): Boolean {
        if (!tier.allowsOnDeviceSlm) {
            return false // Forbidden on Tier 1
        }
        isModelLoadedInRam = true
        return true
    }

    fun unloadModel() {
        isModelLoadedInRam = false
    }

    /**
     * Handles Android ComponentCallbacks2 onTrimMemory signal.
     * Level values:
     * TRIM_MEMORY_RUNNING_CRITICAL = 15
     * TRIM_MEMORY_BACKGROUND = 40
     * TRIM_MEMORY_COMPLETE = 80
     */
    fun onTrimMemory(level: Int) {
        trimEventsHandled++
        // If memory pressure is critical (>= 15) or app sent to background (>= 40), unload model
        if (level >= 15 && isModelLoadedInRam) {
            unloadModel()
        }
    }
}
