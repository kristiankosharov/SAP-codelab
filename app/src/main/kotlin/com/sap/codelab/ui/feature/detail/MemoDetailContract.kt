package com.sap.codelab.ui.feature.detail

import androidx.compose.runtime.Immutable
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.Memo
import com.sap.codelab.ui.components.mvi.UiAction
import com.sap.codelab.ui.components.mvi.UiState
import com.sap.codelab.ui.components.text.UiText

@Immutable
internal data class MemoDetailUiState(
    val isLoading: Boolean,
    val memo: MemoDetailItem?,
    /** Shown instead of the memo when it could not be loaded. */
    val error: UiText?,
) : UiState

@Immutable
internal data class MemoDetailItem(
    val title: String,
    val description: String,
    val location: GeoPoint,
    val isDone: Boolean,
    val isReminded: Boolean,
)

internal fun Memo.toItem() = MemoDetailItem(
    title = title,
    description = description,
    location = location,
    isDone = isDone,
    isReminded = isReminded,
)

internal sealed interface MemoDetailAction : UiAction {
    data object Retry : MemoDetailAction
}
