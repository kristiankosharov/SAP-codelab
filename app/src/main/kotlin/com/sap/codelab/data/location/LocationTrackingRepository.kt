package com.sap.codelab.data.location

import com.sap.codelab.core.AppResult

/**
 * Device location and geofence registration. The provider (Play services today) is hidden behind
 * internal data sources, so it can be swapped without touching domain or ui.
 */
interface LocationTrackingRepository {
    /** A fresh fix (balanced power), falling back to the last known one. Needs a location permission. */
    suspend fun getCurrentLocation(): AppResult<GeoPointData>

    /** Replaces every registered geofence with [regions]. Needs fine location (and background to trigger while away). */
    suspend fun replaceGeofences(regions: List<GeofenceRegionData>): AppResult<Unit>

    suspend fun removeGeofences(ids: List<Long>): AppResult<Unit>

    /** Distance in meters between two points along the WGS84 ellipsoid. */
    fun distanceMeters(
        from: GeoPointData,
        to: GeoPointData,
    ): Double
}
