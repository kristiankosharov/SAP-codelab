package com.sap.codelab.data.location

import android.location.Location
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.source.GeofenceDataSource
import com.sap.codelab.data.location.source.LocationDataSource
import com.sap.codelab.data.permission.PermissionManager
import javax.inject.Inject
import timber.log.Timber

private const val TAG = "LocationTracking"

internal class LocationTrackingRepositoryImpl @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val geofenceDataSource: GeofenceDataSource,
    private val permissionManager: PermissionManager,
) : LocationTrackingRepository {

    override suspend fun getCurrentLocation(): AppResult<GeoPointData> {
        val permissions = permissionManager.currentPermissions()
        if (!permissions.fineLocation && !permissions.coarseLocation) return permissionDenied("current location")
        return when (val result = safeLocationCall { locationDataSource.currentLocation() }) {
            is AppResult.Success -> result.data?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Location.Unavailable(null))
            is AppResult.Failure -> result
        }
    }

    override suspend fun replaceGeofences(regions: List<GeofenceRegionData>): AppResult<Unit> {
        if (!permissionManager.currentPermissions().fineLocation) return permissionDenied("geofences")
        Timber.tag(TAG).d("Replacing geofences with %d regions", regions.size)
        return safeLocationCall { geofenceDataSource.replaceAll(regions) }
    }

    override suspend fun removeGeofences(ids: List<Long>): AppResult<Unit> {
        if (ids.isEmpty()) return AppResult.Success(Unit)
        Timber.tag(TAG).d("Removing geofences for memos %s", ids)
        return safeLocationCall { geofenceDataSource.remove(ids) }
    }

    override fun distanceMeters(
        from: GeoPointData,
        to: GeoPointData,
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return results[0].toDouble()
    }

    private fun permissionDenied(what: String): AppResult.Failure {
        Timber.tag(TAG).d("Skipping %s: location permission not granted", what)
        return AppResult.Failure(AppError.Location.PermissionDenied(null))
    }
}
