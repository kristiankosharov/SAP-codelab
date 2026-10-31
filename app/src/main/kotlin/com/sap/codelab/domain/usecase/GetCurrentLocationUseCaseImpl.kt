package com.sap.codelab.domain.usecase

import com.sap.codelab.core.AppResult
import com.sap.codelab.core.map
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.model.GeoPoint
import javax.inject.Inject

internal class GetCurrentLocationUseCaseImpl @Inject constructor(
    private val locationRepository: LocationTrackingRepository,
) : GetCurrentLocationUseCase {

    override suspend fun invoke(): AppResult<GeoPoint> = locationRepository.getCurrentLocation().map { it.toDomain() }
}
