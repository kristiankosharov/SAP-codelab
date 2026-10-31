package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.mapper.toData
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.model.REMINDER_RADIUS_METERS
import javax.inject.Inject

internal class NotifyNearbyMemosUseCaseImpl @Inject constructor(
    private val memoRepository: MemoRepository,
    private val locationRepository: LocationTrackingRepository,
    private val reminder: MemoReminder,
) : NotifyNearbyMemosUseCase {

    override suspend fun invoke(): AppResult<Int> {
        //Validation if there is any memos
        when (val result = memoRepository.countActiveMemos()) {
            is AppResult.Success -> if (result.data == 0) return AppResult.Success(0)
            is AppResult.Failure -> return result
        }
        val here = when (val result = locationRepository.getCurrentLocation()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val candidates = when (val result = memoRepository.getActiveMemosNear(here, REMINDER_RADIUS_METERS.toDouble())) {
            is AppResult.Success -> result.data.map { it.toDomain() }
            is AppResult.Failure -> return result
        }
        return reminder.remind(candidates.filter { locationRepository.distanceMeters(here, it.location.toData()) <= REMINDER_RADIUS_METERS })
    }
}
