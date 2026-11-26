package com.yaan.feature.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Hardware/driver lifecycle events emitted by the platform audio capture pipeline.
 */
sealed interface AudioCaptureEvent {
    data class DeviceDisconnected(
        val deviceName: String? = null,
        val reason: String = "Audio input device disconnected"
    ) : AudioCaptureEvent

    data class CaptureFailed(val reason: String) : AudioCaptureEvent
}

/**
 * Platform-agnostic audio capture driver abstraction for voice recording lifecycle.
 */
interface AudioCaptureDriver {
    val events: Flow<AudioCaptureEvent>
    fun startCapture()
    fun stopCapture()
    fun cancelCapture()
}

/**
 * Default no-op audio capture driver used when platform microphone capture is not bound.
 */
class NoOpAudioCaptureDriver : AudioCaptureDriver {
    override val events: Flow<AudioCaptureEvent> = emptyFlow()
    override fun startCapture() = Unit
    override fun stopCapture() = Unit
    override fun cancelCapture() = Unit
}
