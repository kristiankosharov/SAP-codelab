package com.sap.codelab.data

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.GeofenceStatusCodes
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.GeofenceRegionData
import com.sap.codelab.data.location.LocationTrackingRepositoryImpl
import com.sap.codelab.data.location.source.GeofenceDataSource
import com.sap.codelab.data.location.source.LocationDataSource
import com.sap.codelab.data.permission.PermissionManager
import com.sap.codelab.data.permission.PermissionsData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocationTrackingRepositoryImplTest {

    private val locationDataSource = mockk<LocationDataSource>()
    private val geofenceDataSource = mockk<GeofenceDataSource>(relaxed = true)
    private val permissionManager = mockk<PermissionManager>()
    private val repository = LocationTrackingRepositoryImpl(locationDataSource, geofenceDataSource, permissionManager)
    private val region = GeofenceRegionData(1, 1.0, 2.0, 200f)

    private fun permissions(
        fine: Boolean = true,
        coarse: Boolean = true,
    ) {
        every { permissionManager.currentPermissions() } returns PermissionsData(fine, coarse, backgroundLocation = true, notifications = true)
    }

    private fun failureOf(result: AppResult<*>) = (result as AppResult.Failure).error

    @Test
    fun `current location is returned`() = runTest {
        permissions()
        coEvery { locationDataSource.currentLocation() } returns GeoPointData(1.0, 2.0)
        assertEquals(AppResult.Success(GeoPointData(1.0, 2.0)), repository.getCurrentLocation())
    }

    @Test
    fun `coarse location alone is enough for the current location`() = runTest {
        permissions(fine = false, coarse = true)
        coEvery { locationDataSource.currentLocation() } returns GeoPointData(1.0, 2.0)
        assertInstanceOf(AppResult.Success::class.java, repository.getCurrentLocation())
    }

    @Test
    fun `no fix is Unavailable`() = runTest {
        permissions()
        coEvery { locationDataSource.currentLocation() } returns null
        assertInstanceOf(AppError.Location.Unavailable::class.java, failureOf(repository.getCurrentLocation()))
    }

    @Test
    fun `without location permission nothing is asked`() = runTest {
        permissions(fine = false, coarse = false)
        assertInstanceOf(AppError.Location.PermissionDenied::class.java, failureOf(repository.getCurrentLocation()))
        assertInstanceOf(AppError.Location.PermissionDenied::class.java, failureOf(repository.replaceGeofences(listOf(region))))
        coVerify(exactly = 0) { locationDataSource.currentLocation() }
        coVerify(exactly = 0) { geofenceDataSource.replaceAll(any()) }
    }

    @Test
    fun `provider failures are mapped`() = runTest {
        permissions()
        coEvery { locationDataSource.currentLocation() } throws SecurityException("revoked")
        assertInstanceOf(AppError.Location.PermissionDenied::class.java, failureOf(repository.getCurrentLocation()))

        coEvery { locationDataSource.currentLocation() } throws ApiException(Status(8))
        assertInstanceOf(AppError.Location.Unavailable::class.java, failureOf(repository.getCurrentLocation()))

        coEvery { locationDataSource.currentLocation() } throws IllegalStateException("bug")
        assertInstanceOf(AppError.Location.Unknown::class.java, failureOf(repository.getCurrentLocation()))
    }

    @Test
    fun `geofence errors keep their status code`() = runTest {
        permissions()
        coEvery { geofenceDataSource.replaceAll(any()) } throws ApiException(Status(GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE))
        val error = failureOf(repository.replaceGeofences(listOf(region)))
        assertEquals(GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE, (error as AppError.Location.Geofence).statusCode)
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        permissions()
        coEvery { locationDataSource.currentLocation() } throws CancellationException("stop")
        assertThrows<CancellationException> { repository.getCurrentLocation() }
    }

    @Test
    fun `geofences are replaced and removed`() = runTest {
        permissions()
        assertEquals(AppResult.Success(Unit), repository.replaceGeofences(listOf(region)))
        assertEquals(AppResult.Success(Unit), repository.removeGeofences(listOf(1)))
        coVerify { geofenceDataSource.replaceAll(listOf(region)) }
        coVerify { geofenceDataSource.remove(listOf(1)) }
    }

    @Test
    fun `removing nothing does not call the provider`() = runTest {
        assertEquals(AppResult.Success(Unit), repository.removeGeofences(emptyList()))
        coVerify(exactly = 0) { geofenceDataSource.remove(any()) }
    }
}
