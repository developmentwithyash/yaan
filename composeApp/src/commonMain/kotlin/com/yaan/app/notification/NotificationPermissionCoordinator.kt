package com.yaan.app.notification

/**
 * Standard status states for runtime notification permissions on Android 13+ (API 33+).
 */
enum class NotificationPermissionStatus {
    /**
     * Runtime permission is not required by this OS level (e.g. Android 12 and below, Desktop JVM, iOS).
     */
    NOT_REQUIRED,

    /**
     * User has granted POST_NOTIFICATIONS permission.
     */
    GRANTED,

    /**
     * Permission is denied. An in-app explanation or system settings dialog is appropriate.
     */
    DENIED,

    /**
     * Platform indicates the app should explain why notifications are necessary before re-requesting.
     */
    SHOULD_SHOW_RATIONALE
}

/**
 * Enterprise coordinator for evaluating, requesting, and guiding users through
 * Android 13+ (Tiramisu API 33+) POST_NOTIFICATIONS runtime permissions.
 * Prevents silent drops of reminders and alarm broadcasts.
 */
class NotificationPermissionCoordinator(
    val osApiLevel: Int = 33
) {

    /**
     * Evaluates whether the current OS API level mandates runtime notification permission.
     */
    fun isRuntimePermissionRequired(): Boolean = osApiLevel >= TIRAMISU_API_LEVEL

    /**
     * Evaluates the permission status based on manifest grant status and rationale capability.
     */
    fun evaluateStatus(
        hasPermission: Boolean,
        shouldShowRationale: Boolean = false
    ): NotificationPermissionStatus {
        if (!isRuntimePermissionRequired()) {
            return NotificationPermissionStatus.NOT_REQUIRED
        }

        return when {
            hasPermission -> NotificationPermissionStatus.GRANTED
            shouldShowRationale -> NotificationPermissionStatus.SHOULD_SHOW_RATIONALE
            else -> NotificationPermissionStatus.DENIED
        }
    }

    /**
     * Checks if the explanatory rationale modal/card should be displayed to the user.
     */
    fun shouldShowRationale(hasPermission: Boolean, canShowRationale: Boolean): Boolean {
        if (!isRuntimePermissionRequired()) return false
        return !hasPermission && canShowRationale
    }

    /**
     * Handles the result of an ActivityResultContracts.RequestPermission() call.
     */
    fun onPermissionResult(granted: Boolean): NotificationPermissionStatus {
        return if (granted) {
            NotificationPermissionStatus.GRANTED
        } else {
            NotificationPermissionStatus.DENIED
        }
    }

    companion object {
        const val TIRAMISU_API_LEVEL = 33

        const val EXPLANATORY_RATIONALE_MESSAGE =
            "YAAN needs notification permission to deliver your scheduled reminders, executive alarms, and daily morning briefings on time. Without notifications, time-sensitive reminders cannot alert you."
    }
}
