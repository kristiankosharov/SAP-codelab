package com.sap.codelab.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data object CreateMemoKey : NavKey

@Serializable
data class MemoDetailKey(val memoId: Long) : NavKey
