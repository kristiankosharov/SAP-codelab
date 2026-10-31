package com.sap.codelab.background

import androidx.work.ListenableWorker
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.usecase.NotifyTriggeredMemosUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeofenceTriggerHandlerImplTest {

    @Test
    fun `entered geofences remind of their memos`() = runTest {
        val notify = mockk<NotifyTriggeredMemosUseCase>()
        coEvery { notify(any()) } returns AppResult.Success(2) andThen AppResult.Failure(AppError.Unknown(null))
        val handler = GeofenceTriggerHandlerImpl(notify)

        handler.onGeofencesEntered(listOf(1, 2))
        handler.onGeofencesEntered(listOf(3))

        coVerify { notify(listOf(1L, 2L)) }
        coVerify { notify(listOf(3L)) }
    }

    @Test
    fun `only database failures are retried`() {
        assertEquals(ListenableWorker.Result.retry(), AppResult.Failure(AppError.Local.Database(null)).toWorkResult())
        assertEquals(ListenableWorker.Result.success(), AppResult.Failure(AppError.Location.PermissionDenied(null)).toWorkResult())
        assertEquals(ListenableWorker.Result.success(), AppResult.Success(Unit).toWorkResult())
    }
}
