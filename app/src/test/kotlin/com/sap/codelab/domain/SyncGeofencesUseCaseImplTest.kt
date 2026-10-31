package com.sap.codelab.domain

import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.GeofenceRegionData
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.domain.model.MAX_GEOFENCES
import com.sap.codelab.domain.model.REMINDER_RADIUS_METERS
import com.sap.codelab.domain.usecase.SyncGeofencesUseCaseImpl
import com.sap.codelab.testing.HERE
import com.sap.codelab.testing.memoData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncGeofencesUseCaseImplTest {

    private val memoRepository = mockk<MemoRepository>()
    private val locationRepository = mockk<LocationTrackingRepository>()
    private val regions = slot<List<GeofenceRegionData>>()
    private val useCase = SyncGeofencesUseCaseImpl(memoRepository, locationRepository)

    init {
        coEvery { locationRepository.replaceGeofences(capture(regions)) } returns AppResult.Success(Unit)
    }

    @Test
    fun `registers a 200 m geofence per active memo`() = runTest {
        coEvery { memoRepository.countActiveMemos() } returns AppResult.Success(2)
        coEvery { memoRepository.getActiveMemos() } returns AppResult.Success(listOf(memoData(1), memoData(2)))

        assertEquals(AppResult.Success(Unit), useCase())

        assertEquals(listOf(1L, 2L), regions.captured.map { it.id })
        assertEquals(listOf(REMINDER_RADIUS_METERS, REMINDER_RADIUS_METERS), regions.captured.map { it.radiusMeters })
        coVerify(exactly = 0) { locationRepository.getCurrentLocation() } // not needed while all fit
    }

    @Test
    fun `with more memos than geofences, registers the nearest ones`() = runTest {
        val here = GeoPointData(HERE.latitude, HERE.longitude)
        val nearest = (1L..MAX_GEOFENCES).map { memoData(it) }
        coEvery { memoRepository.countActiveMemos() } returns AppResult.Success(MAX_GEOFENCES + 5)
        coEvery { locationRepository.getCurrentLocation() } returns AppResult.Success(here)
        coEvery { memoRepository.getNearestActiveMemos(here, MAX_GEOFENCES) } returns AppResult.Success(nearest)

        useCase()

        assertEquals((1L..MAX_GEOFENCES).toList(), regions.captured.map { it.id })
        coVerify(exactly = 0) { memoRepository.getActiveMemos() }
    }

    @Test
    fun `with more memos than geofences and no location, registers the first ones`() = runTest {
        val all = (1L..MAX_GEOFENCES + 5).map { memoData(it) }
        coEvery { memoRepository.countActiveMemos() } returns AppResult.Success(all.size)
        coEvery { locationRepository.getCurrentLocation() } returns AppResult.Failure(AppError.Location.Unavailable(null))
        coEvery { memoRepository.getActiveMemos() } returns AppResult.Success(all)

        useCase()

        assertEquals((1L..MAX_GEOFENCES).toList(), regions.captured.map { it.id })
    }

    @Test
    fun `a database failure is returned and nothing is registered`() = runTest {
        val failure = AppResult.Failure(AppError.Local.Database(null))
        coEvery { memoRepository.countActiveMemos() } returns failure

        assertEquals(failure, useCase())
        coVerify(exactly = 0) { locationRepository.replaceGeofences(any()) }
    }
}
