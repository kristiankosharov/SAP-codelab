package com.sap.codelab.ui.components.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.sap.codelab.R
import com.sap.codelab.ui.components.theme.CodelabTheme

/** Asks for one missing [step]; [onRequest] starts the system request, [onDismiss] means "not now". */
@Composable
fun PermissionDialog(
    step: PermissionStep,
    onRequest: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_notification), contentDescription = null) },
        title = { Text(stringResource(R.string.permission_dialog_title)) },
        text = { Text(stringResource(step.messageRes)) },
        confirmButton = { TextButton(onClick = onRequest) { Text(stringResource(step.buttonRes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.permission_dialog_not_now)) } },
    )
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun PermissionDialogPreview() {
    CodelabTheme {
        PermissionDialog(step = PermissionStep.BackgroundLocation, onRequest = {}, onDismiss = {})
    }
}
