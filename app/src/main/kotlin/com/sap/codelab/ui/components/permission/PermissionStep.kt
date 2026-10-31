package com.sap.codelab.ui.components.permission

import com.sap.codelab.domain.model.PermissionStatus

/**
 * The permissions location reminders need, in the order they must be asked: background location can
 * only be requested once foreground location is granted.
 */
enum class PermissionStep {
    PreciseLocation,
    Notifications,
    BackgroundLocation,
}

/** The next permission to ask for, or null when reminders work in the background. */
fun PermissionStatus.nextMissingStep(): PermissionStep? = when {
    !hasPreciseLocation -> PermissionStep.PreciseLocation
    !hasNotifications -> PermissionStep.Notifications
    !hasBackgroundLocation -> PermissionStep.BackgroundLocation
    else -> null
}
