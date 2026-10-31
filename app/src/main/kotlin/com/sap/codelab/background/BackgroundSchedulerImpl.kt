package com.sap.codelab.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import timber.log.Timber

private const val TAG = "BackgroundScheduler"
private const val PROXIMITY_CHECK_WORK = "proximity_check"
private const val GEOFENCE_SYNC_WORK = "geofence_sync"
private const val PROXIMITY_CHECK_INTERVAL_MINUTES = 15L

internal class BackgroundSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackgroundScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    override fun scheduleLocationTracking() {
        scheduleProximityChecks()
        requestGeofenceSync()
    }

    private fun scheduleProximityChecks() {
        val request = PeriodicWorkRequestBuilder<ProximityCheckWorker>(PROXIMITY_CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(PROXIMITY_CHECK_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        Timber.tag(TAG).d("Proximity checks scheduled every %d min (WorkManager enforces at least 15 min)", PROXIMITY_CHECK_INTERVAL_MINUTES)
    }

    private fun requestGeofenceSync() {
        val request = OneTimeWorkRequestBuilder<GeofenceSyncWorker>().build()
        workManager.enqueueUniqueWork(GEOFENCE_SYNC_WORK, ExistingWorkPolicy.KEEP, request)
        Timber.tag(TAG).d("Geofence sync requested")
    }
}
