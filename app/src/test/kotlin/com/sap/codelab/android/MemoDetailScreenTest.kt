package com.sap.codelab.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.ui.feature.detail.MemoDetailAction
import com.sap.codelab.ui.feature.detail.MemoDetailItem
import com.sap.codelab.ui.feature.detail.MemoDetailScreen
import com.sap.codelab.ui.feature.detail.MemoDetailUiState
import com.sap.codelab.ui.components.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<MemoDetailAction>()
    private var backs = 0
    private val memo = MemoDetailItem("Buy milk", "At the corner shop", GeoPoint(1.0, 2.0), isDone = false, isReminded = false)

    private fun setContent(state: MemoDetailUiState) = compose.setContent {
        MemoDetailScreen(state = state, onAction = { actions += it }, onBack = { backs++ })
    }

    @Test
    fun shows_the_memo_and_goes_back() {
        setContent(MemoDetailUiState(isLoading = false, memo = memo, error = null))
        compose.onNodeWithText("Buy milk").assertIsDisplayed()
        compose.onNodeWithText("Reminds you within 200 m of this place.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun reminded_status() {
        setContent(MemoDetailUiState(isLoading = false, memo = memo.copy(isReminded = true), error = null))
        compose.onNodeWithText("You were reminded at this place.").assertIsDisplayed()
    }

    @Test
    fun done_status() {
        setContent(MemoDetailUiState(isLoading = false, memo = memo.copy(isDone = true), error = null))
        compose.onNodeWithText("Done.").assertIsDisplayed()
    }

    @Test
    fun loading() {
        setContent(MemoDetailUiState(isLoading = true, memo = null, error = null))
        compose.onNodeWithText("Buy milk").assertDoesNotExist()
    }

    @Test
    fun error_offers_retry() {
        setContent(MemoDetailUiState(isLoading = false, memo = null, error = UiText.Dynamic("Gone")))
        compose.onNodeWithText("Gone").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(listOf(MemoDetailAction.Retry), actions)
    }
}
