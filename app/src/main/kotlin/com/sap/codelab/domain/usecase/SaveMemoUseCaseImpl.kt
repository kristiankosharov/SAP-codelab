package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.memo.MemoData
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.model.GeoPoint
import javax.inject.Inject

internal class SaveMemoUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
    private val syncGeofences: SyncGeofencesUseCase,
) : SaveMemoUseCase {

    override suspend fun invoke(
        title: String,
        description: String,
        location: GeoPoint,
    ): AppResult<Long> {
        val memo = MemoData(
            id = 0,
            title = title.trim(),
            description = description.trim(),
            latitude = location.latitude,
            longitude = location.longitude,
            isDone = false,
            isReminded = false,
        )
        val result = memoRepository.addMemo(memo)
        // The memo is saved either way; without permission the next sync (permission grant, boot,
        // periodic check) registers its geofence.
        if (result is AppResult.Success) syncGeofences()
        return result
    }
}
