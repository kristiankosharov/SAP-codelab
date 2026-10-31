package com.sap.codelab.ui.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.codelab.core.onFailure
import com.sap.codelab.core.onSuccess
import com.sap.codelab.domain.usecase.GetMemoUseCase
import com.sap.codelab.ui.components.mvi.BaseViewModel
import com.sap.codelab.ui.components.text.UiText
import com.sap.codelab.ui.components.text.toUiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = MemoDetailViewModel.Factory::class)
internal class MemoDetailViewModel @AssistedInject constructor(
    @Assisted private val memoId: Long,
    private val getMemo: GetMemoUseCase,
) : BaseViewModel<MemoDetailUiState, MemoDetailAction, Nothing>() {

    @AssistedFactory
    interface Factory {
        fun create(memoId: Long): MemoDetailViewModel
    }

    private var isLoading by mutableStateOf(true)
    private var memo by mutableStateOf<MemoDetailItem?>(null)
    private var error by mutableStateOf<UiText?>(null)
    private var loadJob: Job? = null

    init {
        load()
    }

    @Composable
    override fun uiState() = MemoDetailUiState(isLoading = isLoading, memo = memo, error = error)

    override fun onAction(action: MemoDetailAction) {
        when (action) {
            MemoDetailAction.Retry -> load()
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading = true
            error = null
            try {
                getMemo(memoId)
                    .onSuccess { memo = it.toItem() }
                    .onFailure { error = it.toUiText() }
            } finally {
                isLoading = false
            }
        }
    }
}
