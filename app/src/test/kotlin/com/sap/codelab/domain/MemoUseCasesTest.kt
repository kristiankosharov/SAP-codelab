package com.sap.codelab.domain

import app.cash.turbine.test
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.data.permission.PermissionManager
import com.sap.codelab.data.permission.PermissionsData
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.PermissionStatus
import com.sap.codelab.domain.usecase.GetCurrentLocationUseCaseImpl
import com.sap.codelab.domain.usecase.GetMemoUseCaseImpl
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCaseImpl
import com.sap.codelab.domain.usecase.MarkMemoDoneUseCaseImpl
import com.sap.codelab.domain.usecase.ObserveMemosUseCaseImpl
import com.sap.codelab.domain.usecase.SaveMemoUseCaseImpl
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import com.sap.codelab.testing.HERE
import com.sap.codelab.testing.memo
import com.sap.codelab.testing.memoData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoUseCasesTest {

    private val memoRepository = mockk<MemoRepository>()
    private val locationRepository = mockk<LocationTrackingRepository>()
    private val syncGeofences = mockk<SyncGeofencesUseCase>()
    private val dbFailure = AppResult.Failure(AppError.Local.Database(null))

    @Test
    fun `saving trims the text, stores a new memo and syncs geofences`() = runTest {
        coEvery { memoRepository.addMemo(any()) } returns AppResult.Success(5)
        coEvery { syncGeofences() } returns AppResult.Success(Unit)

        val result = SaveMemoUseCaseImpl(memoRepository, syncGeofences)("  Title ", " Text  ", HERE)

        assertEquals(AppResult.Success(5L), result)
        coVerify {
            memoRepository.addMemo(match { it.title == "Title" && it.description == "Text" && !it.isDone && !it.isReminded && it.latitude == HERE.latitude })
        }
        coVerify(exactly = 1) { syncGeofences() }
    }

    @Test
    fun `a failed save does not sync`() = runTest {
        coEvery { memoRepository.addMemo(any()) } returns dbFailure
        assertEquals(dbFailure, SaveMemoUseCaseImpl(memoRepository, syncGeofences)("a", "b", HERE))
        coVerify(exactly = 0) { syncGeofences() }
    }

    @Test
    fun `marking done removes the geofence, but not when it failed`() = runTest {
        coEvery { locationRepository.removeGeofences(any()) } returns AppResult.Success(Unit)
        val useCase = MarkMemoDoneUseCaseImpl(memoRepository, locationRepository)

        coEvery { memoRepository.markDone(1) } returns AppResult.Success(Unit)
        assertEquals(AppResult.Success(Unit), useCase(1))
        coVerify(exactly = 1) { locationRepository.removeGeofences(listOf(1L)) }

        coEvery { memoRepository.markDone(2) } returns dbFailure
        assertEquals(dbFailure, useCase(2))
        coVerify(exactly = 0) { locationRepository.removeGeofences(listOf(2L)) }
    }

    @Test
    fun `memo use cases map data to domain models`() = runTest {
        every { memoRepository.observeMemos(true) } returns flowOf(AppResult.Success(listOf(memoData(1))))
        coEvery { memoRepository.getMemo(1) } returns AppResult.Success(memoData(1))

        ObserveMemosUseCaseImpl(memoRepository)(true).test {
            assertEquals(AppResult.Success(listOf(memo(1))), awaitItem())
            awaitComplete()
        }
        assertEquals(AppResult.Success(memo(1)), GetMemoUseCaseImpl(memoRepository)(1))
    }

    @Test
    fun `location use cases map data to domain models`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns AppResult.Success(GeoPointData(1.0, 2.0))
        val permissionManager = mockk<PermissionManager>()
        every { permissionManager.currentPermissions() } returns PermissionsData(
            fineLocation = false,
            coarseLocation = true,
            backgroundLocation = false,
            notifications = true,
        )

        assertEquals(AppResult.Success(GeoPoint(1.0, 2.0)), GetCurrentLocationUseCaseImpl(locationRepository)())
        val status = GetPermissionStatusUseCaseImpl(permissionManager)()
        assertEquals(PermissionStatus(hasPreciseLocation = false, hasAnyLocation = true, hasBackgroundLocation = false, hasNotifications = true), status)
        assertFalse(status.canRemindInBackground)
        assertTrue(status.copy(hasPreciseLocation = true, hasBackgroundLocation = true).canRemindInBackground)
    }
}
