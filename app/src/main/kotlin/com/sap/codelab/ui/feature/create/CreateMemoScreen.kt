package com.sap.codelab.ui.feature.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sap.codelab.R
import com.sap.codelab.ui.components.map.LocationPickerMap
import com.sap.codelab.ui.components.mvi.NavigationHandler
import com.sap.codelab.ui.components.mvi.UiEffectHandler
import com.sap.codelab.ui.components.permission.PermissionDialog
import com.sap.codelab.ui.components.permission.PermissionStep
import com.sap.codelab.ui.components.permission.rememberPermissionRequester
import com.sap.codelab.ui.components.text.UiText
import com.sap.codelab.ui.components.text.asString
import com.sap.codelab.ui.components.theme.CodelabTheme

@Composable
fun CreateMemoRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CreateMemoViewModel = hiltViewModel()
    CreateMemoScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Wires the permission requester (needs an Activity) and lifecycle; [CreateMemoContent] is the stateless UI. */
@Composable
internal fun CreateMemoScreen(
    state: CreateMemoUiState,
    onAction: (CreateMemoAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionRequester = rememberPermissionRequester(onResult = { onAction(CreateMemoAction.PermissionsChanged) })
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onAction(CreateMemoAction.PermissionsChanged) }
    CreateMemoContent(
        state = state,
        onAction = onAction,
        onBack = onBack,
        onRequestPermission = permissionRequester::request,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateMemoContent(
    state: CreateMemoUiState,
    onAction: (CreateMemoAction) -> Unit,
    onBack: () -> Unit,
    onRequestPermission: (PermissionStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    state.permissionDialog?.let { step ->
        PermissionDialog(
            step = step,
            onRequest = { onRequestPermission(step) },
            onDismiss = { onAction(CreateMemoAction.DismissPermissionDialog) },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { effect -> onAction(CreateMemoAction.EffectConsumed(effect)) },
    )
    NavigationHandler(
        navigation = state.navigation,
        onNavigate = { navigation ->
            when (navigation) {
                CreateMemoNavigation.Back -> onBack()
            }
        },
        onNavigationConsumed = { onAction(CreateMemoAction.NavigationConsumed(it)) },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_memo_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { onAction(CreateMemoAction.Save) }, enabled = !state.isSaving) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).imePadding().fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onAction(CreateMemoAction.TitleChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_memo_title_label)) },
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { error -> { Text(error.asString()) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onAction(CreateMemoAction.DescriptionChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_memo_description_label)) },
                    isError = state.descriptionError != null,
                    supportingText = state.descriptionError?.let { error -> { Text(error.asString()) } },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(
                    text = state.locationError?.asString() ?: stringResource(
                        if (state.location == null) R.string.create_memo_location_hint else R.string.create_memo_location_picked,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.locationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LocationPickerMap(
                selected = state.location,
                initialCenter = state.initialCenter,
                showMyLocation = state.showMyLocation,
                onLocationPicked = { onAction(CreateMemoAction.LocationPicked(it)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

private val previewState = CreateMemoUiState(
    title = "Buy milk",
    description = "",
    location = null,
    initialCenter = null,
    showMyLocation = false,
    titleError = null,
    descriptionError = UiText.Dynamic("Please provide a memo description"),
    locationError = UiText.Dynamic("Tap the map to choose where to be reminded"),
    isSaving = false,
    permissionDialog = null,
    effect = null,
    navigation = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun CreateMemoContentPreview() {
    CodelabTheme {
        CreateMemoContent(state = previewState, onAction = {}, onBack = {}, onRequestPermission = {})
    }
}

@PreviewLightDark
@Composable
private fun CreateMemoPermissionDialogPreview() {
    CodelabTheme {
        CreateMemoContent(
            state = previewState.copy(permissionDialog = PermissionStep.PreciseLocation),
            onAction = {},
            onBack = {},
            onRequestPermission = {},
        )
    }
}
