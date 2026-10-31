package com.sap.codelab.ui.feature.create

import androidx.compose.runtime.Immutable
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.ui.components.mvi.UiAction
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.mvi.UiNavigation
import com.sap.codelab.ui.components.mvi.UiState
import com.sap.codelab.ui.components.permission.PermissionStep
import com.sap.codelab.ui.components.text.UiText

@Immutable
internal data class CreateMemoUiState(
    val title: String,
    val description: String,
    val location: GeoPoint?,
    /** Where the map starts when nothing is picked yet: the device location, once known. */
    val initialCenter: GeoPoint?,
    val showMyLocation: Boolean,
    val titleError: UiText?,
    val descriptionError: UiText?,
    val locationError: UiText?,
    val isSaving: Boolean,
    /** The next missing permission, asked in a dialog until the user grants it or says "not now". */
    val permissionDialog: PermissionStep?,
    val effect: UiEffect<CreateMemoAction>?,
    val navigation: CreateMemoNavigation?,
) : UiState

internal sealed interface CreateMemoAction : UiAction {
    data class TitleChanged(val title: String) : CreateMemoAction

    data class DescriptionChanged(val description: String) : CreateMemoAction

    data class LocationPicked(val location: GeoPoint) : CreateMemoAction

    data object Save : CreateMemoAction

    /** After a permission request, and whenever the screen resumes (the user may have changed settings). */
    data object PermissionsChanged : CreateMemoAction

    data object DismissPermissionDialog : CreateMemoAction

    class EffectConsumed(val effect: UiEffect<CreateMemoAction>) : CreateMemoAction

    class NavigationConsumed(val navigation: CreateMemoNavigation) : CreateMemoAction
}

internal sealed interface CreateMemoNavigation : UiNavigation {
    /** The memo was saved. */
    data object Back : CreateMemoNavigation
}
