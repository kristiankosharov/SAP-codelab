package com.sap.codelab.core

/** Every failure, structured by source. The UI maps it to a message with `toUiText()`. */
sealed interface AppError {
    val cause: Throwable?

    sealed interface Local : AppError {
        data object NotFound : Local {
            override val cause: Throwable? = null
        }

        data class Constraint(override val cause: Throwable?) : Local

        data class Database(override val cause: Throwable?) : Local

        data class Unknown(override val cause: Throwable?) : Local
    }

    /** Location and geofencing failures (Play services), raised only by `data/location`. */
    sealed interface Location : AppError {
        /** A location permission the call needs is missing. */
        data class PermissionDenied(override val cause: Throwable?) : Location

        /** Location services are off or no fix could be obtained. */
        data class Unavailable(override val cause: Throwable?) : Location

        /** Play services rejected the geofence request (e.g. GEOFENCE_NOT_AVAILABLE, too many geofences). */
        data class Geofence(
            val statusCode: Int?,
            override val cause: Throwable?,
        ) : Location

        data class Unknown(override val cause: Throwable?) : Location
    }

    /** The user blocked notifications (permission revoked or the app's notifications turned off). */
    data class NotificationsBlocked(override val cause: Throwable?) : AppError

    data class Unknown(override val cause: Throwable?) : AppError
}
