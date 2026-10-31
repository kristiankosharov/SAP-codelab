package com.sap.codelab.ui

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.model.PermissionStatus
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCase
import com.sap.codelab.domain.usecase.MarkMemoDoneUseCase
import com.sap.codelab.domain.usecase.ObserveMemosUseCase
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import com.sap.codelab.testing.MainDispatcherExtension
import com.sap.codelab.testing.memo
import com.sap.codelab.ui.feature.home.HomeAction
import com.sap.codelab.ui.feature.home.HomeViewModel
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.permission.PermissionStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val observeMemos = mockk<ObserveMemosUseCase>()
    private val markMemoDone = mockk<MarkMemoDoneUseCase>()
    private val syncGeofences = mockk<SyncGeofencesUseCase>()
    private var permissions = PermissionStatus(
        hasPreciseLocation = false,
        hasAnyLocation = false,
        hasBackgroundLocation = false,
        hasNotifications = true,
    )

    init {
        every { observeMemos(false) } returns flowOf(AppResult.Success(listOf(memo(1))))
        every { observeMemos(true) } returns flowOf(AppResult.Success(listOf(memo(1), memo(2, isDone = true))))
        coEvery { markMemoDone(any()) } returns AppResult.Success(Unit)
        coEvery { syncGeofences() } returns AppResult.Success(Unit)
    }

    private fun viewModel() = HomeViewModel(
        SavedStateHandle(),
        observeMemos,
        markMemoDone,
        GetPermissionStatusUseCase { permissions },
        syncGeofences,
    )

    @Test
    fun `shows the open memos and the first missing permission`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.showAll)
            assertEquals(listOf(1L), state.memos.map { it.id })
            assertEquals(PermissionStep.PreciseLocation, state.missingPermission)
        }
    }

    @Test
    fun `toggling the filter shows all memos`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            viewModel.onAction(HomeAction.ToggleShowAll)
            Snapshot.sendApplyNotifications()
            val state = expectMostRecentItem()
            assertTrue(state.showAll)
            assertEquals(listOf(1L, 2L), state.memos.map { it.id })
        }
    }

    @Test
    fun `marking a memo done calls the use case and shows failures`() = runTest(main.dispatcher) {
        coEvery { markMemoDone(1) } returns AppResult.Failure(AppError.Local.Database(null))
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            assertNull(awaitItem().effect)
            viewModel.onAction(HomeAction.MarkDone(1))
            Snapshot.sendApplyNotifications()
            assertInstanceOf(UiEffect.ShowSnackbar::class.java, expectMostRecentItem().effect)
        }
        coVerify { markMemoDone(1) }
    }

    @Test
    fun `granting location re-registers the geofences and moves to the next step`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            permissions = permissions.copy(hasPreciseLocation = true, hasAnyLocation = true)
            viewModel.onAction(HomeAction.PermissionsChanged)
            Snapshot.sendApplyNotifications()
            assertEquals(PermissionStep.BackgroundLocation, expectMostRecentItem().missingPermission)
        }
        coVerify(exactly = 1) { syncGeofences() }
    }

    @Test
    fun `an unchanged permission status does nothing`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(HomeAction.PermissionsChanged)
        coVerify(exactly = 0) { syncGeofences() }
    }
}
