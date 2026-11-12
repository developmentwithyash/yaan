package com.yaan.app.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPermissionCoordinatorTest {

    @Test
    fun `on Android below API 33 notifications do not require runtime permission`() {
        val coordinator = NotificationPermissionCoordinator(osApiLevel = 31)

        assertFalse(coordinator.isRuntimePermissionRequired())
        assertEquals(
            NotificationPermissionStatus.NOT_REQUIRED,
            coordinator.evaluateStatus(hasPermission = false, shouldShowRationale = false)
        )
        assertFalse(coordinator.shouldShowRationale(hasPermission = false, canShowRationale = true))
    }

    @Test
    fun `on Android API 33 and above runtime permission is strictly required`() {
        val coordinator = NotificationPermissionCoordinator(osApiLevel = 33)

        assertTrue(coordinator.isRuntimePermissionRequired())

        // Initial state before granting
        assertEquals(
            NotificationPermissionStatus.DENIED,
            coordinator.evaluateStatus(hasPermission = false, shouldShowRationale = false)
        )

        // User denies with rationale needed
        assertEquals(
            NotificationPermissionStatus.SHOULD_SHOW_RATIONALE,
            coordinator.evaluateStatus(hasPermission = false, shouldShowRationale = true)
        )

        // Permission granted
        assertEquals(
            NotificationPermissionStatus.GRANTED,
            coordinator.evaluateStatus(hasPermission = true, shouldShowRationale = false)
        )
    }

    @Test
    fun `permission result callback updates status accurately`() {
        val coordinator = NotificationPermissionCoordinator(osApiLevel = 34)

        assertEquals(NotificationPermissionStatus.GRANTED, coordinator.onPermissionResult(granted = true))
        assertEquals(NotificationPermissionStatus.DENIED, coordinator.onPermissionResult(granted = false))
    }

    @Test
    fun `rationale explanation copy contains alarm and reminder justification`() {
        val rationale = NotificationPermissionCoordinator.EXPLANATORY_RATIONALE_MESSAGE

        assertTrue(rationale.contains("reminders", ignoreCase = true))
        assertTrue(rationale.contains("notifications", ignoreCase = true))
    }
}
