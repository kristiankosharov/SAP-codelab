package com.sap.codelab.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.LocationTrackingRepositoryImpl
import com.sap.codelab.data.location.source.GeofenceDataSource
import com.sap.codelab.data.location.source.LocationDataSource
import com.sap.codelab.data.permission.PermissionManager
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on Robolectric because the distance comes from the framework's `Location.distanceBetween`. */
@RunWith(AndroidJUnit4::class)
class LocationDistanceTest {

    private val repository = LocationTrackingRepositoryImpl(
        mockk<LocationDataSource>(),
        mockk<GeofenceDataSource>(),
        mockk<PermissionManager>(),
    )
    private val sofia = GeoPointData(42.6977, 23.3219)

    @Test
    fun `distance to the same point is zero`() {
        assertEquals(0.0, repository.distanceMeters(sofia, sofia), 0.0)
    }

    @Test
    fun `200 m of latitude is measured as about 200 m`() {
        val north = GeoPointData(sofia.latitude + 200.0 / 111_195.08, sofia.longitude)

        assertEquals(200.0, repository.distanceMeters(sofia, north), 1.0)
    }

    @Test
    fun `distance is symmetric`() {
        val plovdiv = GeoPointData(42.1354, 24.7453)

        assertEquals(repository.distanceMeters(sofia, plovdiv), repository.distanceMeters(plovdiv, sofia), 1.0)
        assertEquals(133_000.0, repository.distanceMeters(sofia, plovdiv), 3_000.0)
    }
}
