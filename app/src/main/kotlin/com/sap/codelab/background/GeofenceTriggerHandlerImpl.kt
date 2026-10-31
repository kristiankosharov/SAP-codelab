package com.sap.codelab.background

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.geofence.GeofenceTriggerHandler
import com.sap.codelab.domain.usecase.NotifyTriggeredMemosUseCase
import javax.inject.Inject
import timber.log.Timber

private const val TAG = "GeofenceTrigger"

internal class GeofenceTriggerHandlerImpl @Inject constructor(
    private val notifyTriggeredMemos: NotifyTriggeredMemosUseCase,
) : GeofenceTriggerHandler {

    override suspend fun onGeofencesEntered(memoIds: List<Long>) {
        val result = notifyTriggeredMemos(memoIds)
        if (result is AppResult.Success) Timber.tag(TAG).d("Reminded of %d memos", result.data)
    }
}
