package com.sap.codelab.ui

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.model.PermissionStatus
import com.sap.codelab.domain.usecase.GetCurrentLocationUseCase
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCase
import com.sap.codelab.domain.usecase.SaveMemoUseCase
import com.sap.codelab.testing.HERE
import com.sap.codelab.testing.MainDispatcherExtension
import com.sap.codelab.ui.feature.create.CreateMemoAction
import com.sap.codelab.ui.feature.create.CreateMemoNavigation
import com.sap.codelab.ui.feature.create.CreateMemoViewModel
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.permission.PermissionStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CreateMemoViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val saveMemo = mockk<SaveMemoUseCase>()
    private val grantedLocation = PermissionStatus(
        hasPreciseLocation = true,
        hasAnyLocation = true,
        hasBackgroundLocation = true,
        hasNotifications = true,
    )

    private val noPermissions = PermissionStatus(
        hasPreciseLocation = false,
        hasAnyLocation = false,
        hasBackgroundLocation = false,
        hasNotifications = true,
    )
    private var permissions = grantedLocation

    private fun viewModel() = CreateMemoViewModel(
        SavedStateHandle(),
        GetPermissionStatusUseCase { permissions },
        GetCurrentLocationUseCase { AppResult.Success(HERE) },
        saveMemo,
    )

    private fun CreateMemoViewModel.fillIn() {
        onAction(CreateMemoAction.TitleChanged("Buy milk"))
        onAction(CreateMemoAction.DescriptionChanged("At the corner shop"))
        onAction(CreateMemoAction.LocationPicked(HERE))
    }

    @Test
    fun `starts centred on the device location`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            val state = awaitItem()
            assertEquals(HERE, state.initialCenter)
            assertNull(state.titleError) // no errors before the first save
        }
    }

    @Test
    fun `asks for the first missing permission in a dialog`() = runTest(main.dispatcher) {
        permissions = noPermissions
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            val state = awaitItem()
            assertEquals(PermissionStep.PreciseLocation, state.permissionDialog)
            assertFalse(state.showMyLocation)
            assertNull(state.initialCenter)
        }
    }

    @Test
    fun `not now hides the dialog`() = runTest(main.dispatcher) {
        permissions = noPermissions
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            viewModel.onAction(CreateMemoAction.DismissPermissionDialog)
            Snapshot.sendApplyNotifications()
            assertNull(expectMostRecentItem().permissionDialog)
        }
    }

    @Test
    fun `the dialog comes back on the next permission check while something is missing`() = runTest(main.dispatcher) {
        permissions = noPermissions
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            viewModel.onAction(CreateMemoAction.DismissPermissionDialog)
            Snapshot.sendApplyNotifications()
            assertNull(expectMostRecentItem().permissionDialog)

            viewModel.onAction(CreateMemoAction.PermissionsChanged)
            Snapshot.sendApplyNotifications()
            assertEquals(PermissionStep.PreciseLocation, expectMostRecentItem().permissionDialog)
        }
    }

    @Test
    fun `granting location moves to the next step and centres the map`() = runTest(main.dispatcher) {
        permissions = noPermissions
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            permissions = noPermissions.copy(hasPreciseLocation = true, hasAnyLocation = true)
            viewModel.onAction(CreateMemoAction.PermissionsChanged)
            Snapshot.sendApplyNotifications()
            val state = expectMostRecentItem()
            assertEquals(PermissionStep.BackgroundLocation, state.permissionDialog)
            assertTrue(state.showMyLocation)
            assertEquals(HERE, state.initialCenter)
        }
    }

    @Test
    fun `no dialog when everything is granted`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            assertNull(awaitItem().permissionDialog)
        }
    }

    @Test
    fun `saving an incomplete memo shows the errors and saves nothing`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            viewModel.onAction(CreateMemoAction.Save)
            Snapshot.sendApplyNotifications()
            val state = expectMostRecentItem()
            assertNotNull(state.titleError)
            assertNotNull(state.descriptionError)
            assertNotNull(state.locationError)
            assertNull(state.navigation)
        }
        coVerify(exactly = 0) { saveMemo(any(), any(), any()) }
    }

    @Test
    fun `saving a complete memo navigates back`() = runTest(main.dispatcher) {
        coEvery { saveMemo("Buy milk", "At the corner shop", HERE) } returns AppResult.Success(1)
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onAction(CreateMemoAction.Save)
            Snapshot.sendApplyNotifications()
            val state = expectMostRecentItem()
            assertEquals(CreateMemoNavigation.Back, state.navigation)

            viewModel.onAction(CreateMemoAction.NavigationConsumed(CreateMemoNavigation.Back))
            Snapshot.sendApplyNotifications()
            assertNull(expectMostRecentItem().navigation)
        }
    }

    @Test
    fun `a failed save shows a snackbar and stays`() = runTest(main.dispatcher) {
        coEvery { saveMemo(any(), any(), any()) } returns AppResult.Failure(AppError.Local.Database(null))
        val viewModel = viewModel()
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onAction(CreateMemoAction.Save)
            Snapshot.sendApplyNotifications()
            val state = expectMostRecentItem()
            assertInstanceOf(UiEffect.ShowSnackbar::class.java, state.effect)
            assertNull(state.navigation)
        }
    }
}
