package com.sap.codelab.ui.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sap.codelab.R
import com.sap.codelab.ui.components.mvi.UiEffectHandler
import com.sap.codelab.ui.components.permission.PermissionStep
import com.sap.codelab.ui.components.permission.buttonRes
import com.sap.codelab.ui.components.permission.messageRes
import com.sap.codelab.ui.components.permission.rememberPermissionRequester
import com.sap.codelab.ui.components.theme.CodelabTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HomeRoute(
    onCreateMemo: () -> Unit,
    onOpenMemo: (memoId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    HomeScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onCreateMemo = onCreateMemo,
        onOpenMemo = onOpenMemo,
        modifier = modifier,
    )
}

/** Wires the permission requester (needs an Activity) and lifecycle; [HomeContent] is the stateless UI. */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onCreateMemo: () -> Unit,
    onOpenMemo: (memoId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionRequester = rememberPermissionRequester(onResult = { onAction(HomeAction.PermissionsChanged) })
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onAction(HomeAction.PermissionsChanged) }
    HomeContent(
        state = state,
        onAction = onAction,
        onCreateMemo = onCreateMemo,
        onOpenMemo = onOpenMemo,
        onRequestPermission = permissionRequester::request,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onCreateMemo: () -> Unit,
    onOpenMemo: (memoId: Long) -> Unit,
    onRequestPermission: (PermissionStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { effect -> onAction(HomeAction.EffectConsumed(effect)) },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), modifier = Modifier.semantics { heading() }) },
                actions = {
                    TextButton(onClick = { onAction(HomeAction.ToggleShowAll) }) {
                        Text(stringResource(if (state.showAll) R.string.home_show_open else R.string.home_show_all))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMemo) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.home_new_memo))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            state.missingPermission?.let { step ->
                PermissionBanner(step = step, onRequest = { onRequestPermission(step) }, modifier = Modifier.padding(16.dp))
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                state.memos.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
                }

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.memos, key = { it.id }) { memo ->
                        MemoRow(
                            memo = memo,
                            onClick = { onOpenMemo(memo.id) },
                            onMarkDone = { onAction(HomeAction.MarkDone(memo.id)) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    step: PermissionStep,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(step.messageRes), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRequest, modifier = Modifier.align(Alignment.End)) { Text(stringResource(step.buttonRes)) }
        }
    }
}

@Composable
private fun MemoRow(
    memo: MemoItem,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
        headlineContent = { Text(memo.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(memo.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            // Done memos stay checked; un-checking is not supported.
            Checkbox(checked = memo.isDone, onCheckedChange = { if (it) onMarkDone() }, enabled = !memo.isDone)
        },
    )
}

private val previewState = HomeUiState(
    isLoading = false,
    memos = persistentListOf(
        MemoItem(1, "Buy milk", "At the corner shop, the one with the green door", isDone = false),
        MemoItem(2, "Return the book", "Library closes at 6 pm", isDone = true),
    ),
    showAll = true,
    missingPermission = PermissionStep.BackgroundLocation,
    effect = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun HomeContentPreview() {
    CodelabTheme {
        HomeContent(state = previewState, onAction = {}, onCreateMemo = {}, onOpenMemo = {}, onRequestPermission = {})
    }
}
