package com.sap.codelab.data.location.source

import com.sap.codelab.data.location.GeoPointData

/** Raw access to the location provider. Callers check permissions first; failures are thrown and mapped by `safeLocationCall`. */
internal interface LocationDataSource {
    /** A fresh fix, else the last known one, else null. */
    suspend fun currentLocation(): GeoPointData?
}
