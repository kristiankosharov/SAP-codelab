package com.sap.codelab.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sap.codelab.R
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.ui.components.map.LocationPreviewMap
import com.sap.codelab.ui.components.text.asString
import com.sap.codelab.ui.components.theme.CodelabTheme

@Composable
fun MemoDetailRoute(
    memoId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<MemoDetailViewModel, MemoDetailViewModel.Factory> { factory ->
        factory.create(memoId)
    }
    MemoDetailScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoDetailScreen(
    state: MemoDetailUiState,
    onAction: (MemoDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.memo_detail_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.memo != null -> MemoDetails(state.memo)

                else -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(state.error?.asString().orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { onAction(MemoDetailAction.Retry) }) { Text(stringResource(R.string.action_retry)) }
                }
            }
        }
    }
}

@Composable
private fun MemoDetails(
    memo: MemoDetailItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(memo.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(memo.description, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(
                when {
                    memo.isDone -> R.string.memo_detail_status_done
                    memo.isReminded -> R.string.memo_detail_status_reminded
                    else -> R.string.memo_detail_status_waiting
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocationPreviewMap(point = memo.location, modifier = Modifier.fillMaxWidth().height(240.dp))
    }
}

private val previewState = MemoDetailUiState(
    isLoading = false,
    memo = MemoDetailItem(
        title = "Buy milk",
        description = "At the corner shop, the one with the green door",
        location = GeoPoint(42.6977, 23.3219),
        isDone = false,
        isReminded = false,
    ),
    error = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun MemoDetailScreenPreview() {
    CodelabTheme {
        MemoDetailScreen(state = previewState, onAction = {}, onBack = {})
    }
}
