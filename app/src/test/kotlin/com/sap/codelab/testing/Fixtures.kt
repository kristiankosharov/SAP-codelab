package com.sap.codelab.testing

import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoData
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.Memo
import io.mockk.every
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Sofia city centre; the reference point for distance tests. */
val HERE = GeoPoint(42.6977, 23.3219)

/** A point [meters] north of [from] (one degree of latitude ≈ 111 195 m on the haversine sphere). */
fun northOf(
    from: GeoPoint,
    meters: Double,
) = GeoPoint(from.latitude + meters / 111_195.08, from.longitude)

fun memoData(
    id: Long,
    at: GeoPoint = HERE,
    isDone: Boolean = false,
    isReminded: Boolean = false,
    title: String = "Memo $id",
    description: String = "Text $id",
) = MemoData(id, title, description, at.latitude, at.longitude, isDone, isReminded)

fun memo(
    id: Long,
    at: GeoPoint = HERE,
    isDone: Boolean = false,
    isReminded: Boolean = false,
) = Memo(id, "Memo $id", "Text $id", at, isDone, isReminded)

private const val EARTH_RADIUS_METERS = 6_371_008.8

/** Haversine distance in meters; stands in for the Android `Location.distanceBetween` that plain JVM tests cannot run. */
fun haversineMeters(
    from: GeoPointData,
    to: GeoPointData,
): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val h = sin((lat2 - lat1) / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(Math.toRadians(to.longitude - from.longitude) / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}

fun LocationTrackingRepository.stubDistances() {
    every { distanceMeters(any(), any()) } answers { haversineMeters(firstArg(), secondArg()) }
}
