package com.sap.codelab.ui

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.sap.codelab.core.AppError
import com.sap.codelab.core.AppResult
import com.sap.codelab.domain.usecase.GetMemoUseCase
import com.sap.codelab.testing.MainDispatcherExtension
import com.sap.codelab.testing.memo
import com.sap.codelab.ui.feature.detail.MemoDetailViewModel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MemoDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    @Test
    fun `loads the memo`() = runTest(main.dispatcher) {
        val viewModel = MemoDetailViewModel(memoId = 3, getMemo = GetMemoUseCase { AppResult.Success(memo(it)) })
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Memo 3", state.memo?.title)
            assertNull(state.error)
        }
    }

    @Test
    fun `a missing memo shows an error`() = runTest(main.dispatcher) {
        val viewModel = MemoDetailViewModel(memoId = 3, getMemo = GetMemoUseCase { AppResult.Failure(AppError.Local.NotFound) })
        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            val state = awaitItem()
            assertNull(state.memo)
            assertNotNull(state.error)
        }
    }
}
