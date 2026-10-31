package com.sap.codelab.android

import android.app.Application
import android.app.PendingIntent
import android.location.Location
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Tasks
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.GeofenceRegionData
import com.sap.codelab.data.location.source.GeofenceDataSourceImpl
import com.sap.codelab.data.location.source.LocationDataSourceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationDataSourcesTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val fused = mockk<FusedLocationProviderClient>()
    private val geofencing = mockk<GeofencingClient>()

    @Before
    fun mockPlayServices() {
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any<android.content.Context>()) } returns fused
        every { LocationServices.getGeofencingClient(any<android.content.Context>()) } returns geofencing
    }

    @After
    fun tearDown() = unmockkAll()

    private fun location(
        lat: Double,
        lng: Double,
    ) = Location("fused").apply {
        latitude = lat
        longitude = lng
        accuracy = 5f
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }

    private fun currentLocationReturns(location: Location?) {
        every { fused.getCurrentLocation(any<CurrentLocationRequest>(), any<CancellationToken>()) } returns Tasks.forResult(location)
    }

    @Test
    fun uses_the_current_fix() = runTest {
        currentLocationReturns(location(1.0, 2.0))
        assertEquals(GeoPointData(1.0, 2.0), LocationDataSourceImpl(app).currentLocation())
    }

    @Test
    fun falls_back_to_the_last_known_fix() = runTest {
        currentLocationReturns(null)
        every { fused.lastLocation } returns Tasks.forResult(location(3.0, 4.0))
        assertEquals(GeoPointData(3.0, 4.0), LocationDataSourceImpl(app).currentLocation())
    }

    @Test
    fun no_fix_at_all_is_null() = runTest {
        currentLocationReturns(null)
        every { fused.lastLocation } returns Tasks.forResult(null)
        assertNull(LocationDataSourceImpl(app).currentLocation())
    }

    @Test
    fun replaces_all_geofences() = runTest {
        val request = slot<GeofencingRequest>()
        every { geofencing.removeGeofences(any<PendingIntent>()) } returns Tasks.forResult(null)
        every { geofencing.addGeofences(capture(request), any()) } returns Tasks.forResult(null)

        GeofenceDataSourceImpl(app).replaceAll(listOf(GeofenceRegionData(5, 1.0, 2.0, 200f)))

        val geofence = request.captured.geofences.single()
        assertEquals("5", geofence.requestId)
        assertEquals(Geofence.GEOFENCE_TRANSITION_ENTER, geofence.transitionTypes)
        assertEquals(GeofencingRequest.INITIAL_TRIGGER_ENTER, request.captured.initialTrigger)
        assertEquals(200f, geofence.radius)
    }

    @Test
    fun replacing_with_nothing_only_removes() = runTest {
        every { geofencing.removeGeofences(any<PendingIntent>()) } returns Tasks.forResult(null)
        GeofenceDataSourceImpl(app).replaceAll(emptyList())
        verify(exactly = 0) { geofencing.addGeofences(any(), any()) }
    }

    @Test
    fun removes_geofences_by_memo_id() = runTest {
        every { geofencing.removeGeofences(any<List<String>>()) } returns Tasks.forResult(null)
        GeofenceDataSourceImpl(app).remove(listOf(1, 2))
        verify { geofencing.removeGeofences(listOf("1", "2")) }
    }
}
