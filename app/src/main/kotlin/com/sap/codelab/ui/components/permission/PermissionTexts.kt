package com.sap.codelab.ui.components.permission

import androidx.annotation.StringRes
import com.sap.codelab.R

/** Why the app needs this step, shown in the home banner and the create-screen dialog. */
@get:StringRes
val PermissionStep.messageRes: Int
    get() = when (this) {
        PermissionStep.PreciseLocation -> R.string.permission_location_message
        PermissionStep.Notifications -> R.string.permission_notifications_message
        PermissionStep.BackgroundLocation -> R.string.permission_background_message
    }

/** The button that starts the request for this step. */
@get:StringRes
val PermissionStep.buttonRes: Int
    get() = when (this) {
        PermissionStep.PreciseLocation -> R.string.permission_location_button
        PermissionStep.Notifications -> R.string.permission_notifications_button
        PermissionStep.BackgroundLocation -> R.string.permission_background_button
    }
