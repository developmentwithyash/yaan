package com.yaan.feature.voice

import com.yaan.core.common.dispatcher.TestCoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeAudioCaptureDriver(
    var throwOnStart: Throwable? = null,
    var throwOnCancel: Throwable? = null,
) : AudioCaptureDriver {
    private val _events = MutableSharedFlow<AudioCaptureEvent>(extraBufferCapacity = 16)
    override val events: Flow<AudioCaptureEvent> = _events

    var startCalls = 0
    var stopCalls = 0
    var cancelCalls = 0
    var isCapturing = false

    override fun startCapture() {
        startCalls++
        throwOnStart?.let { throw it }
        isCapturing = true
    }

    override fun stopCapture() {
        stopCalls++
        isCapturing = false
    }

    override fun cancelCapture() {
        cancelCalls++
        isCapturing = false
        throwOnCancel?.let { throw it }
    }

    suspend fun emitEvent(event: AudioCaptureEvent) {
        _events.emit(event)
    }
}

class VoiceViewModelTest {

    @Test
    fun testStartAndCancelRecordingTransitionsToCancelledState() = runTest {
        val driver = FakeAudioCaptureDriver()
        val viewModel = VoiceViewModel(
            dispatchers = TestCoroutineDispatchers(),
            captureDriver = driver
        )

        assertEquals(VoiceSessionState.IDLE, viewModel.state.value.sessionState)
        assertEquals(VoiceRecordingState.Idle, viewModel.state.value.recordingState)

        viewModel.onIntent(VoiceIntent.ToggleListening)
        assertEquals(VoiceSessionState.LISTENING, viewModel.state.value.sessionState)
        assertEquals(VoiceRecordingState.Recording, viewModel.state.value.recordingState)
        assertTrue(driver.isCapturing)

        viewModel.onIntent(VoiceIntent.CancelRecording)
        assertEquals(VoiceSessionState.IDLE, viewModel.state.value.sessionState)
        assertEquals(VoiceRecordingState.Cancelled, viewModel.state.value.recordingState)
        assertEquals(1, driver.cancelCalls)
        assertEquals("", viewModel.state.value.transcriptText)
    }

    @Test
    fun testDeviceDisconnectionEventTransitionsToErrorWithoutCrashing() = runTest {
        val driver = FakeAudioCaptureDriver()
        val viewModel = VoiceViewModel(
            dispatchers = TestCoroutineDispatchers(),
            captureDriver = driver
        )

        viewModel.onIntent(VoiceIntent.ToggleListening)
        assertEquals(VoiceRecordingState.Recording, viewModel.state.value.recordingState)

        driver.emitEvent(
            AudioCaptureEvent.DeviceDisconnected(
                deviceName = "USB-C Headset",
                reason = "Audio input device disconnected mid-recording"
            )
        )

        assertEquals(VoiceSessionState.IDLE, viewModel.state.value.sessionState)
        val errorState = assertIs<VoiceRecordingState.Error>(viewModel.state.value.recordingState)
        assertTrue(errorState.reason.contains("disconnected", ignoreCase = true))
        assertEquals(1, driver.cancelCalls)
    }

    @Test
    fun testStartCaptureExceptionTransitionsGracefullyToErrorState() = runTest {
        val driver = FakeAudioCaptureDriver(
            throwOnStart = IllegalStateException("Microphone hardware unavailable")
        )
        val viewModel = VoiceViewModel(
            dispatchers = TestCoroutineDispatchers(),
            captureDriver = driver
        )

        viewModel.onIntent(VoiceIntent.ToggleListening)

        assertEquals(VoiceSessionState.IDLE, viewModel.state.value.sessionState)
        val errorState = assertIs<VoiceRecordingState.Error>(viewModel.state.value.recordingState)
        assertEquals("Microphone hardware unavailable", errorState.reason)
    }

    @Test
    fun testCancelWhenDriverThrowsStillCompletesSafely() = runTest {
        val driver = FakeAudioCaptureDriver(
            throwOnCancel = RuntimeException("AudioTrack already released")
        )
        val viewModel = VoiceViewModel(
            dispatchers = TestCoroutineDispatchers(),
            captureDriver = driver
        )

        viewModel.onIntent(VoiceIntent.ToggleListening)
        viewModel.onIntent(VoiceIntent.CancelRecording)

        assertEquals(VoiceSessionState.IDLE, viewModel.state.value.sessionState)
        assertEquals(VoiceRecordingState.Cancelled, viewModel.state.value.recordingState)
    }

    @Test
    fun testDismissRecordingErrorResetsStateToIdle() = runTest {
        val driver = FakeAudioCaptureDriver()
        val viewModel = VoiceViewModel(
            dispatchers = TestCoroutineDispatchers(),
            captureDriver = driver
        )

        viewModel.onIntent(VoiceIntent.DeviceDisconnected("Bluetooth mic lost"))
        assertIs<VoiceRecordingState.Error>(viewModel.state.value.recordingState)

        viewModel.onIntent(VoiceIntent.DismissRecordingError)
        assertEquals(VoiceRecordingState.Idle, viewModel.state.value.recordingState)
    }
}
