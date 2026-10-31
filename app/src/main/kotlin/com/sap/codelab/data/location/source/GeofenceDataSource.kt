package com.sap.codelab.data.location.source

import com.sap.codelab.data.location.GeofenceRegionData

/** Raw access to the geofencing provider. Callers check permissions first; failures are thrown and mapped by `safeLocationCall`. */
internal interface GeofenceDataSource {
    suspend fun replaceAll(regions: List<GeofenceRegionData>)

    suspend fun remove(ids: List<Long>)
}
