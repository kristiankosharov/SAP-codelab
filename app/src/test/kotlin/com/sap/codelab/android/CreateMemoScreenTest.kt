package com.sap.codelab.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.ui.feature.create.CreateMemoAction
import com.sap.codelab.ui.feature.create.CreateMemoContent
import com.sap.codelab.ui.feature.create.CreateMemoNavigation
import com.sap.codelab.ui.feature.create.CreateMemoScreen
import com.sap.codelab.ui.feature.create.CreateMemoUiState
import com.sap.codelab.ui.components.permission.PermissionStep
import com.sap.codelab.ui.components.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class CreateMemoScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val actions = mutableListOf<CreateMemoAction>()
    private var backs = 0
    private val requested = mutableListOf<PermissionStep>()

    private val empty = CreateMemoUiState(
        title = "",
        description = "",
        location = null,
        initialCenter = null,
        showMyLocation = false,
        titleError = null,
        descriptionError = null,
        locationError = null,
        isSaving = false,
        permissionDialog = null,
        effect = null,
        navigation = null,
    )

    private fun setContent(state: CreateMemoUiState) = compose.setContent {
        CreateMemoContent(state = state, onAction = { actions += it }, onBack = { backs++ }, onRequestPermission = { requested += it })
    }

    @Test
    fun typing_saving_and_back() {
        setContent(empty)
        compose.onNode(hasSetTextAction() and hasText("Title")).performTextInput("Milk")
        compose.onNode(hasSetTextAction() and hasText("Text")).performTextInput("Shop")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        // The state is fixed here, so the fields also report going back to ""; only the typed values matter.
        assertTrue(
            actions.containsAll(listOf(CreateMemoAction.TitleChanged("Milk"), CreateMemoAction.DescriptionChanged("Shop"), CreateMemoAction.Save)),
        )
        assertEquals(1, backs)
        compose.onNodeWithText("Tap the map to choose where to be reminded.").assertIsDisplayed()
    }

    @Test
    fun errors_are_shown() {
        setContent(
            empty.copy(
                titleError = UiText.Dynamic("Title missing"),
                descriptionError = UiText.Dynamic("Text missing"),
                locationError = UiText.Dynamic("Pick a place"),
            ),
        )
        compose.onNodeWithText("Title missing").assertIsDisplayed()
        compose.onNodeWithText("Text missing").assertIsDisplayed()
        compose.onNodeWithText("Pick a place").assertIsDisplayed()
    }

    @Test
    fun a_picked_location_and_saving_state() {
        setContent(empty.copy(location = GeoPoint(1.0, 2.0), initialCenter = GeoPoint(1.0, 2.0), showMyLocation = true, isSaving = true))
        compose.onNodeWithText("You'll be reminded within 200 m of the marker. Tap again to move it.").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun result_navigation_goes_back_once() {
        setContent(empty.copy(navigation = CreateMemoNavigation.Back))
        compose.waitForIdle()
        assertEquals(1, backs)
        assertEquals(listOf(CreateMemoAction.NavigationConsumed::class), actions.map { it::class })
    }

    @Test
    fun the_permission_dialog_requests_or_is_dismissed() {
        setContent(empty.copy(permissionDialog = PermissionStep.PreciseLocation))
        compose.onNodeWithText("Location reminders").assertIsDisplayed()
        compose.onNodeWithText("Allow location").performClick()
        compose.onNodeWithText("Not now").performClick()
        assertEquals(listOf(PermissionStep.PreciseLocation), requested)
        assertEquals(listOf(CreateMemoAction.DismissPermissionDialog), actions)
    }

    @Test
    fun the_screen_wires_the_requester_and_resume() {
        compose.setContent {
            CreateMemoScreen(state = empty.copy(permissionDialog = PermissionStep.PreciseLocation), onAction = { actions += it }, onBack = {})
        }
        compose.waitForIdle()
        assertTrue(CreateMemoAction.PermissionsChanged in actions)
        compose.onNodeWithText("Allow location").performClick()
        assertTrue(shadowOf(compose.activity).lastRequestedPermission != null)
    }
}
