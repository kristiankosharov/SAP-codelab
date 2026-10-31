package com.sap.codelab.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.usecase.NotifyNearbyMemosUseCase
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

private const val TAG = "ProximityCheck"

/**
 * The safety net under geofencing: geofences can be late or lost (location switched off, aggressive
 * OEM battery savers), so this checks the distance to every active memo itself and re-registers the
 * geofences.
 */
@HiltWorker
internal class ProximityCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notifyNearbyMemos: NotifyNearbyMemosUseCase,
    private val syncGeofences: SyncGeofencesUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("Checking nearby memos (attempt %d)", runAttemptCount + 1)
        val notified = notifyNearbyMemos()
        when (notified) {
            is AppResult.Success -> Timber.tag(TAG).d("Reminded of %d nearby memos", notified.data)
            is AppResult.Failure -> Timber.tag(TAG).d("Check skipped: %s", notified.error::class.simpleName)
        }
        syncGeofences()
        return notified.toWorkResult()
    }
}
