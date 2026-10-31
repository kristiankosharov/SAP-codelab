package com.sap.codelab.ui.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sap.codelab.ui.feature.create.CreateMemoRoute
import com.sap.codelab.ui.feature.detail.MemoDetailRoute
import com.sap.codelab.ui.feature.home.HomeRoute

/**
 * The app's whole navigation: the back stack and one entry per destination. [deepLinkMemoId] (from
 * a reminder notification) opens that memo on top of the current stack, then is consumed.
 */
@Composable
fun AppNavigation(
    deepLinkMemoId: Long?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeKey)
    val currentOnDeepLinkHandled by rememberUpdatedState(onDeepLinkHandled)

    LaunchedEffect(deepLinkMemoId) {
        val memoId = deepLinkMemoId ?: return@LaunchedEffect
        currentOnDeepLinkHandled() // consume first, so it never opens twice
        backStack.add(MemoDetailKey(memoId))
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(), // each destination gets its own ViewModels
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeRoute(
                    onCreateMemo = { backStack.add(CreateMemoKey) },
                    onOpenMemo = { memoId -> backStack.add(MemoDetailKey(memoId)) },
                )
            }
            entry<CreateMemoKey> { CreateMemoRoute(onBack = { backStack.removeLastOrNull() }) }
            entry<MemoDetailKey> { key -> MemoDetailRoute(memoId = key.memoId, onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
