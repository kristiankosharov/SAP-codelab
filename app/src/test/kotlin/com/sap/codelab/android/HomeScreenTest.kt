package com.sap.codelab.android

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.ui.feature.home.HomeAction
import com.sap.codelab.ui.feature.home.HomeContent
import com.sap.codelab.ui.feature.home.HomeScreen
import com.sap.codelab.ui.feature.home.HomeUiState
import com.sap.codelab.ui.feature.home.MemoItem
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.permission.PermissionStep
import com.sap.codelab.ui.components.text.UiText
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val actions = mutableListOf<HomeAction>()
    private val opened = mutableListOf<Long>()
    private var created = 0
    private val requested = mutableListOf<PermissionStep>()

    private fun state(
        memos: List<MemoItem> = listOf(MemoItem(1, "Buy milk", "At the corner shop", isDone = false)),
        isLoading: Boolean = false,
        showAll: Boolean = false,
        missingPermission: PermissionStep? = null,
        effect: UiEffect<HomeAction>? = null,
    ) = HomeUiState(isLoading, persistentListOf(*memos.toTypedArray()), showAll, missingPermission, effect)

    private fun setContent(state: HomeUiState) = compose.setContent {
        HomeContent(
            state = state,
            onAction = { actions += it },
            onCreateMemo = { created++ },
            onOpenMemo = { opened += it },
            onRequestPermission = { requested += it },
        )
    }

    @Test
    fun list_interactions() {
        setContent(state())
        compose.onNodeWithText("Buy milk").assertIsDisplayed().performClick()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Show all").performClick()
        compose.onNodeWithContentDescription("New memo").performClick()

        assertEquals(listOf(1L), opened)
        assertEquals(listOf(HomeAction.MarkDone(1), HomeAction.ToggleShowAll), actions)
        assertEquals(1, created)
    }

    @Test
    fun showing_all_offers_show_open() {
        setContent(state(showAll = true, memos = listOf(MemoItem(2, "Done one", "x", isDone = true))))
        compose.onNodeWithText("Show open").assertIsDisplayed()
    }

    @Test
    fun empty_and_loading() {
        setContent(state(memos = emptyList()))
        compose.onNodeWithText("No memos yet. Tap + to create one.").assertIsDisplayed()
    }

    @Test
    fun loading_hides_the_list() {
        setContent(state(isLoading = true))
        compose.onNodeWithText("Buy milk").assertDoesNotExist()
    }

    @Test
    fun the_banner_asks_for_the_missing_step() {
        setContent(state(missingPermission = PermissionStep.Notifications))
        compose.onNodeWithText("Allow notifications").performClick()
        assertEquals(listOf(PermissionStep.Notifications), requested)
    }

    @Test
    fun snackbar_action_is_dispatched_and_consumed() {
        val effect = UiEffect.ShowSnackbar(UiText.Dynamic("Could not save"), UiText.Dynamic("Retry"), HomeAction.ToggleShowAll)
        setContent(state(effect = effect))
        compose.waitUntil { compose.onAllNodes(hasText("Retry")).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil { actions.any { it is HomeAction.EffectConsumed } }
        assertTrue(HomeAction.ToggleShowAll in actions)
    }

    @Test
    fun toast_effect_is_shown_and_consumed() {
        setContent(state(effect = UiEffect.ShowToast(UiText.Dynamic("Saved"), isLong = true)))
        compose.waitUntil { actions.any { it is HomeAction.EffectConsumed } }
        assertEquals("Saved", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun screen_requests_permissions_and_rechecks_on_resume() {
        compose.setContent {
            HomeScreen(state = state(missingPermission = PermissionStep.PreciseLocation), onAction = { actions += it }, onCreateMemo = {}, onOpenMemo = {})
        }
        compose.waitForIdle()
        assertTrue(HomeAction.PermissionsChanged in actions) // ON_RESUME

        compose.onNodeWithText("Allow location").performClick()
        val request = shadowOf(compose.activity).lastRequestedPermission
        assertTrue(Manifest.permission.ACCESS_FINE_LOCATION in request.requestedPermissions)

        // Denied without a rationale: Android won't ask again, so the app settings open.
        actions.clear()
        @Suppress("DEPRECATION") // how Robolectric delivers the result to the ActivityResult registry
        compose.activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { -1 },
        )
        compose.waitForIdle()
        assertEquals(listOf(HomeAction.PermissionsChanged), actions)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, shadowOf(compose.activity).nextStartedActivity.action)
    }

    @Test
    fun background_step_requests_background_location() {
        compose.setContent {
            HomeScreen(state = state(missingPermission = PermissionStep.BackgroundLocation), onAction = {}, onCreateMemo = {}, onOpenMemo = {})
        }
        compose.onNodeWithText("Open location settings").performClick()
        val request = shadowOf(compose.activity).lastRequestedPermission
        assertEquals(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), request.requestedPermissions.toList())
    }

    @Test
    fun notifications_turned_off_in_settings_open_the_settings() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        compose.setContent {
            HomeScreen(state = state(missingPermission = PermissionStep.Notifications), onAction = {}, onCreateMemo = {}, onOpenMemo = {})
        }
        compose.onNodeWithText("Allow notifications").performClick()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, shadowOf(compose.activity).nextStartedActivity.action)
    }
}
