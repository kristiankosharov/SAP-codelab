package com.sap.codelab.ui.components.text

import com.sap.codelab.R
import com.sap.codelab.core.AppError

/** The generic message for an error. Map to a screen-specific string first when the screen needs one. */
fun AppError.toUiText(): UiText = UiText.Resource(
    when (this) {
        AppError.Local.NotFound -> R.string.error_not_found

        is AppError.Local.Constraint,
        is AppError.Local.Database,
        is AppError.Local.Unknown,
        -> R.string.error_storage

        is AppError.Location.PermissionDenied -> R.string.error_location_permission

        is AppError.Location.Unavailable -> R.string.error_location_unavailable

        is AppError.Location.Geofence, is AppError.Location.Unknown -> R.string.error_location_unknown

        is AppError.NotificationsBlocked -> R.string.error_notifications_blocked

        is AppError.Unknown -> R.string.error_unknown
    },
)
