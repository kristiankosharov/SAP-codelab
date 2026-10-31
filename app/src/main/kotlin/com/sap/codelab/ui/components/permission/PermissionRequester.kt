package com.sap.codelab.ui.components.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Asks for one [PermissionStep] at a time. Created with [rememberPermissionRequester]. */
@Stable
class PermissionRequester internal constructor(private val onRequest: (PermissionStep) -> Unit) {
    fun request(step: PermissionStep) = onRequest(step)
}

/**
 * Requesting needs an Activity, so it lives here and not in the data layer's `PermissionManager`
 * (which only checks). [onResult] runs after every request; the screen then re-reads the status.
 * When Android won't show the dialog any more (denied twice, or notifications turned off), the
 * app's system settings are opened instead. On Android 11+ the background-location request itself
 * opens the settings page, where the user picks "Allow all the time".
 */
@Composable
fun rememberPermissionRequester(onResult: () -> Unit): PermissionRequester {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val denied = results.filterValues { granted -> !granted }.keys
        val activity = context.findActivity()
        if (activity != null && denied.isNotEmpty() && denied.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }) {
            context.openAppSettings()
        }
        currentOnResult()
    }
    return remember(context, launcher) {
        PermissionRequester { step ->
            val permissions = step.runtimePermissions(context)
            if (permissions.isEmpty()) context.openAppSettings() else launcher.launch(permissions.toTypedArray())
        }
    }
}

/** The runtime permissions to request for [this] step; empty when only the settings screen can help. */
private fun PermissionStep.runtimePermissions(context: Context): List<String> = when (this) {
    PermissionStep.PreciseLocation -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    PermissionStep.Notifications ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList() // granted, but the app's notifications are switched off in settings
        }

    PermissionStep.BackgroundLocation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else emptyList()
}

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
