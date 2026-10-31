package com.sap.codelab.ui.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.sap.codelab.core.onFailure
import com.sap.codelab.core.onSuccess
import com.sap.codelab.domain.usecase.GetPermissionStatusUseCase
import com.sap.codelab.domain.usecase.MarkMemoDoneUseCase
import com.sap.codelab.domain.usecase.ObserveMemosUseCase
import com.sap.codelab.domain.usecase.SyncGeofencesUseCase
import com.sap.codelab.ui.components.mvi.BaseViewModel
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.permission.nextMissingStep
import com.sap.codelab.ui.components.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
internal class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMemos: ObserveMemosUseCase,
    private val markMemoDone: MarkMemoDoneUseCase,
    private val getPermissionStatus: GetPermissionStatusUseCase,
    private val syncGeofences: SyncGeofencesUseCase,
) : BaseViewModel<HomeUiState, HomeAction, Nothing>() {

    private var showAll by savedStateHandle.saveable { mutableStateOf(false) }
    private var isLoading by mutableStateOf(true)
    private var memos by mutableStateOf<ImmutableList<MemoItem>>(persistentListOf())
    private var permissionStatus by mutableStateOf(getPermissionStatus())
    private var observeJob: Job? = null

    init {
        observe()
    }

    @Composable
    override fun uiState() = HomeUiState(
        isLoading = isLoading,
        memos = memos,
        showAll = showAll,
        missingPermission = permissionStatus.nextMissingStep(),
        effect = currentEffect,
    )

    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.ToggleShowAll -> {
                showAll = !showAll
                observe()
            }
            is HomeAction.MarkDone -> markDone(action.id)
            HomeAction.PermissionsChanged -> refreshPermissions()
            is HomeAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    /** (Re)starts observing the memos for the current filter; the previous collection is cancelled. */
    private fun observe() {
        observeJob?.cancel()
        isLoading = true
        observeJob = observeMemos(includeDone = showAll)
            .onEach { result ->
                isLoading = false
                result
                    .onSuccess { data -> memos = data.map { it.toItem() }.toImmutableList() }
                    .onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
            }
            .launchIn(viewModelScope)
    }

    private fun markDone(id: Long) {
        viewModelScope.launch {
            markMemoDone(id).onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
        }
    }

    /** A newly granted location permission means the geofences can be registered now. */
    private fun refreshPermissions() {
        val status = getPermissionStatus()
        if (status == permissionStatus) return
        val gainedLocation = status.hasPreciseLocation && !permissionStatus.hasPreciseLocation
        val gainedBackground = status.hasBackgroundLocation && !permissionStatus.hasBackgroundLocation
        permissionStatus = status
        if (gainedLocation || gainedBackground) viewModelScope.launch { syncGeofences() }
    }
}
