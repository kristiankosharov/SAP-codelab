package com.sap.codelab.data.location

data class GeoPointData(
    val latitude: Double,
    val longitude: Double,
)

/** A circular region that notifies when the device enters it. [id] is the memo id. */
data class GeofenceRegionData(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
)
