package com.sap.codelab.ui.feature.create

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.sap.codelab.R
import com.sap.codelab.core.onFailure
import com.sap.codelab.core.onSuccess
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.usecase.GetCurrentLocationUseCase
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCase
import com.sap.codelab.domain.usecase.SaveMemoUseCase
import com.sap.codelab.ui.components.mvi.BaseViewModel
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.permission.nextMissingStep
import com.sap.codelab.ui.components.text.UiText
import com.sap.codelab.ui.components.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
internal class CreateMemoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPermissionStatus: GetPermissionStatusUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val saveMemo: SaveMemoUseCase,
) : BaseViewModel<CreateMemoUiState, CreateMemoAction, CreateMemoNavigation>() {

    // What the user typed and picked survives process death.
    private var title by savedStateHandle.saveable { mutableStateOf("") }
    private var description by savedStateHandle.saveable { mutableStateOf("") }
    private var latitude by savedStateHandle.saveable { mutableStateOf<Double?>(null) }
    private var longitude by savedStateHandle.saveable { mutableStateOf<Double?>(null) }

    /** Errors show only after the first save attempt, then follow the input. */
    private var showErrors by mutableStateOf(false)
    private var initialCenter by mutableStateOf<GeoPoint?>(null)
    private var isSaving by mutableStateOf(false)
    private var saveJob: Job? = null
    private var permissionStatus by mutableStateOf(getPermissionStatus())

    /** "Not now" hides the dialog until the next permission check (screen resumed, request answered). */
    private var isPermissionDialogDismissed by mutableStateOf(false)

    private val location: GeoPoint?
        get() {
            val lat = latitude ?: return null
            val lng = longitude ?: return null
            return GeoPoint(lat, lng)
        }

    init {
        centerOnDevice()
    }

    @Composable
    override fun uiState() = CreateMemoUiState(
        title = title,
        description = description,
        location = location,
        initialCenter = initialCenter,
        showMyLocation = permissionStatus.hasAnyLocation,
        titleError = errorIf(title.isBlank(), R.string.create_memo_title_error),
        descriptionError = errorIf(description.isBlank(), R.string.create_memo_description_error),
        locationError = errorIf(location == null, R.string.create_memo_location_error),
        isSaving = isSaving,
        permissionDialog = if (isPermissionDialogDismissed) null else permissionStatus.nextMissingStep(),
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: CreateMemoAction) {
        when (action) {
            is CreateMemoAction.TitleChanged -> title = action.title
            is CreateMemoAction.DescriptionChanged -> description = action.description
            is CreateMemoAction.LocationPicked -> {
                latitude = action.location.latitude
                longitude = action.location.longitude
            }
            CreateMemoAction.Save -> save()
            CreateMemoAction.PermissionsChanged -> {
                isPermissionDialogDismissed = false
                refreshPermissions()
            }
            CreateMemoAction.DismissPermissionDialog -> isPermissionDialogDismissed = true
            is CreateMemoAction.EffectConsumed -> consumeEffect(action.effect)
            is CreateMemoAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    /** Moves the map to the device once, when location is allowed; no fix leaves it zoomed out. */
    private fun centerOnDevice() {
        if (initialCenter != null || !permissionStatus.hasAnyLocation) return
        viewModelScope.launch { getCurrentLocation().onSuccess { initialCenter = it } }
    }

    private fun refreshPermissions() {
        val status = getPermissionStatus()
        if (status == permissionStatus) return
        permissionStatus = status
        centerOnDevice()
    }

    private fun save() {
        if (saveJob?.isActive == true) return
        showErrors = true
        val location = location
        if (title.isBlank() || description.isBlank() || location == null) return
        saveJob = viewModelScope.launch {
            isSaving = true
            try {
                saveMemo(title, description, location)
                    .onSuccess { navigate(CreateMemoNavigation.Back) }
                    .onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
            } finally {
                isSaving = false
            }
        }
    }

    private fun errorIf(
        invalid: Boolean,
        @StringRes message: Int,
    ): UiText? =
        if (showErrors && invalid) UiText.Resource(message) else null
}
