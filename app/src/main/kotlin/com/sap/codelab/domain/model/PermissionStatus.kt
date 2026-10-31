package com.sap.codelab.domain.model

data class PermissionStatus(
    /** Precise location: needed for geofences and the map's location layer. */
    val hasPreciseLocation: Boolean,
    val hasAnyLocation: Boolean,
    /** "Allow all the time": geofences trigger while the app is in the background or not running. */
    val hasBackgroundLocation: Boolean,
    val hasNotifications: Boolean,
) {
    val canRemindInBackground: Boolean get() = hasPreciseLocation && hasBackgroundLocation && hasNotifications
}
