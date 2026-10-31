package com.sap.codelab.data.permission

/** Which runtime permissions location reminders depend on are granted right now. */
data class PermissionsData(
    val fineLocation: Boolean,
    val coarseLocation: Boolean,
    /** Always true below API 29, where foreground location also covers the background. */
    val backgroundLocation: Boolean,
    /** Always true below API 33; false when the user turned the app's notifications off. */
    val notifications: Boolean,
)

/**
 * Reads permission state. Requesting permissions needs an Activity, so that lives in the UI
 * (`ui/permission`); this only checks, so background work can use it too.
 */
interface PermissionManager {
    fun currentPermissions(): PermissionsData
}
