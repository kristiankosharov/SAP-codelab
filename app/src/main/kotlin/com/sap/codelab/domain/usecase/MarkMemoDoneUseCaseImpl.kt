package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoRepository
import javax.inject.Inject

internal class MarkMemoDoneUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
    private val locationRepository: LocationTrackingRepository,
) : MarkMemoDoneUseCase {

    override suspend fun invoke(id: Long): AppResult<Unit> {
        val result = memoRepository.markDone(id)
        // A geofence that stays behind is harmless: done memos are skipped when it triggers.
        if (result is AppResult.Success) locationRepository.removeGeofences(listOf(id))
        return result
    }
}
