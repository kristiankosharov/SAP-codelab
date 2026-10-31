package com.sap.codelab.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

private const val TAG = "GeofenceSync"

/** Re-registers every geofence (after boot, an app update, or on request). */
@HiltWorker
internal class GeofenceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncGeofences: SyncGeofencesUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("Syncing geofences (attempt %d)", runAttemptCount + 1)
        val result = syncGeofences()
        if (result is AppResult.Failure) Timber.tag(TAG).d("Sync skipped: %s", result.error::class.simpleName)
        return result.toWorkResult()
    }
}
