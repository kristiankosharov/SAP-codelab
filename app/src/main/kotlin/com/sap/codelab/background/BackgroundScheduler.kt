package com.sap.codelab.background

/** Schedules the background work that keeps reminders working while the app is not in use. */
interface BackgroundScheduler {
    /**
     * Starts location tracking in the background: the periodic fallback check (every 15 min, the WorkManager
     * minimum) and a one-time geofence re-registration soon (a reboot and an app update both clear them).
     * Idempotent: both are unique work, so calling it on every process start is cheap.
     */
    fun scheduleLocationTracking()
}
