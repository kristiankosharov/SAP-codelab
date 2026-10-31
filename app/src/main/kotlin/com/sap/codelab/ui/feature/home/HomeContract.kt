package com.sap.codelab.ui.feature.home

import androidx.compose.runtime.Immutable
import com.sap.codelab.domain.model.Memo
import com.sap.codelab.ui.components.mvi.UiAction
import com.sap.codelab.ui.components.mvi.UiEffect
import com.sap.codelab.ui.components.mvi.UiState
import com.sap.codelab.ui.components.permission.PermissionStep
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class HomeUiState(
    val isLoading: Boolean,
    val memos: ImmutableList<MemoItem>,
    val showAll: Boolean,
    /** The next permission reminders need, shown as a banner; null when everything is granted. */
    val missingPermission: PermissionStep?,
    val effect: UiEffect<HomeAction>?,
) : UiState

@Immutable
internal data class MemoItem(
    val id: Long,
    val title: String,
    val description: String,
    val isDone: Boolean,
)

internal fun Memo.toItem() = MemoItem(id = id, title = title, description = description, isDone = isDone)

internal sealed interface HomeAction : UiAction {
    data object ToggleShowAll : HomeAction

    data class MarkDone(val id: Long) : HomeAction

    /** After a permission request, and whenever the screen resumes (the user may have changed settings). */
    data object PermissionsChanged : HomeAction

    class EffectConsumed(val effect: UiEffect<HomeAction>) : HomeAction
}
