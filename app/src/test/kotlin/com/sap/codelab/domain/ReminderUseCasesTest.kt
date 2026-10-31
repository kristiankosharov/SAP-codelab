package com.sap.codelab.domain

import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.location.LocationTrackingRepository
import com.sap.codelab.data.memo.MemoRepository
import com.sap.codelab.data.notification.ReminderNotifier
import com.sap.codelab.domain.usecase.MemoReminder
import com.sap.codelab.domain.usecase.NotifyNearbyMemosUseCaseImpl
import com.sap.codelab.domain.usecase.NotifyTriggeredMemosUseCaseImpl
import com.sap.codelab.testing.HERE
import com.sap.codelab.testing.memoData
import com.sap.codelab.testing.northOf
import com.sap.codelab.testing.stubDistances
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReminderUseCasesTest {

    private val memoRepository = mockk<MemoRepository>()
    private val locationRepository = mockk<LocationTrackingRepository>()
    private val notifier = mockk<ReminderNotifier>()
    private val reminder = MemoReminder(memoRepository, locationRepository, notifier)
    private val notifyNearby = NotifyNearbyMemosUseCaseImpl(memoRepository, locationRepository, reminder)
    private val notifyTriggered = NotifyTriggeredMemosUseCaseImpl(memoRepository, reminder)

    init {
        every { notifier.showReminder(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { memoRepository.markReminded(any()) } returns AppResult.Success(Unit)
        coEvery { locationRepository.removeGeofences(any()) } returns AppResult.Success(Unit)
        locationRepository.stubDistances()
        coEvery { locationRepository.getCurrentLocation() } returns AppResult.Success(GeoPointData(HERE.latitude, HERE.longitude))
    }

    @Test
    fun `nearby check reminds of memos within 200 m only`() = runTest {
        coEvery { memoRepository.countActiveMemos() } returns AppResult.Success(2)
        coEvery { memoRepository.getActiveMemosNear(any(), any()) } returns AppResult.Success(
            listOf(memoData(1, northOf(HERE, 199.0)), memoData(2, northOf(HERE, 201.0))),
        )

        assertEquals(AppResult.Success(1), notifyNearby())

        verify(exactly = 1) { notifier.showReminder(1, "Memo 1", "Text 1") }
        verify(exactly = 0) { notifier.showReminder(2, any(), any()) }
        coVerify { memoRepository.markReminded(listOf(1L)) }
        coVerify { locationRepository.removeGeofences(listOf(1L)) }
    }

    @Test
    fun `nearby check without a location fix returns the failure`() = runTest {
        val failure = AppResult.Failure(AppError.Location.Unavailable(null))
        coEvery { memoRepository.countActiveMemos() } returns AppResult.Success(1)
        coEvery { locationRepository.getCurrentLocation() } returns failure

        assertEquals(failure, notifyNearby())
        verify(exactly = 0) { notifier.showReminder(any(), any(), any()) }
    }

    @Test
    fun `nearby check without active memos does not ask for a location`() = runTest {
        coEvery { memoRepository.countActiveMemos() } returns AppResult.Success(0)

        assertEquals(AppResult.Success(0), notifyNearby())

        coVerify(exactly = 0) { locationRepository.getCurrentLocation() }
        coVerify(exactly = 0) { memoRepository.getActiveMemosNear(any(), any()) }
    }

    @Test
    fun `triggered geofences skip done and already reminded memos`() = runTest {
        coEvery { memoRepository.getMemos(listOf(1, 2, 3)) } returns AppResult.Success(
            listOf(memoData(1), memoData(2, isDone = true), memoData(3, isReminded = true)),
        )

        assertEquals(AppResult.Success(1), notifyTriggered(listOf(1, 2, 3)))

        verify(exactly = 1) { notifier.showReminder(any(), any(), any()) }
        coVerify { memoRepository.markReminded(listOf(1L)) }
    }

    @Test
    fun `a reminder that could not be shown keeps the memo active`() = runTest {
        coEvery { memoRepository.getMemos(listOf(1)) } returns AppResult.Success(listOf(memoData(1)))
        every { notifier.showReminder(any(), any(), any()) } returns AppResult.Failure(AppError.NotificationsBlocked(null))

        assertEquals(AppResult.Success(0), notifyTriggered(listOf(1)))

        coVerify(exactly = 0) { memoRepository.markReminded(any()) }
        coVerify(exactly = 0) { locationRepository.removeGeofences(any()) }
    }

    @Test
    fun `the notification shows the first 140 characters of the text`() = runTest {
        val text = "x".repeat(200)
        coEvery { memoRepository.getMemos(listOf(1)) } returns AppResult.Success(listOf(memoData(1, description = text)))

        notifyTriggered(listOf(1))

        verify { notifier.showReminder(1, "Memo 1", "x".repeat(140)) }
    }
}
