package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.core.map
import com.sap.codelab.data.location.GeofenceRegionData
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoData
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.model.MAX_GEOFENCES
import com.sap.codelab.domain.model.REMINDER_RADIUS_METERS
import javax.inject.Inject

internal class SyncGeofencesUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
    private val locationRepository: LocationTrackingRepository,
) : SyncGeofencesUseCase {

    override suspend fun invoke(): AppResult<Unit> {
        val memos = when (val result = watchedMemos()) {
            is AppResult.Success -> result.data.map { it.toDomain() }
            is AppResult.Failure -> return result
        }
        val regions = memos.map { memo ->
            GeofenceRegionData(memo.id, memo.location.latitude, memo.location.longitude, REMINDER_RADIUS_METERS)
        }
        return locationRepository.replaceGeofences(regions)
    }

    /** Every memo while they fit; otherwise the nearest ones (the periodic check re-syncs as the user moves). */
    private suspend fun watchedMemos(): AppResult<List<MemoData>> {
        val count = when (val result = memoRepository.countActiveMemos()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        if (count <= MAX_GEOFENCES) return memoRepository.getActiveMemos()
        val here = (locationRepository.getCurrentLocation() as? AppResult.Success)?.data
            ?: return memoRepository.getActiveMemos().map { it.take(MAX_GEOFENCES) }
        return memoRepository.getNearestActiveMemos(here, MAX_GEOFENCES)
    }
}
